package com.github.zero9178.mlirods.model

import com.github.zero9178.mlirods.MyBundle
import com.github.zero9178.mlirods.language.TableGenFileType
import com.github.zero9178.mlirods.language.psi.TableGenFile
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.readAction
import com.intellij.openapi.application.writeAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.checkCanceled
import com.intellij.openapi.progress.runBlockingCancellable
import com.intellij.openapi.project.DumbModeTask
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.ModificationTracker
import com.intellij.openapi.util.SimpleModificationTracker
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.originalFileOrSelf
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiTreeAnyChangeAbstractAdapter
import com.intellij.psi.impl.PsiManagerEx
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.util.concurrency.annotations.RequiresReadLock
import com.intellij.util.indexing.FileBasedIndex
import com.jetbrains.rd.util.firstOrNull
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch
import java.io.Closeable
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

data class TableGenContext(
    /**
     * Chain of files starting from a TableGen file with compile commands (first element) until the file that directly
     * includes the file this context belongs to.
     */
    val includedFrom: PersistentList<VirtualFile> = persistentListOf(),
    /**
     * Paths used for include processing by the file this context belongs to.
     */
    val includePaths: List<VirtualFile> = emptyList(),
    /**
     * Macros defined outside this file and valid within this context.
     * These can be treated as if '#define's were placed at the very beginning of the file.
     */
    val defines: Set<String> = emptySet(),
    /**
     * Files that have been included before this file was included in this context.
     * These can and should be treated identically to as if they were include statements at the beginning of the file
     * this context belongs to.
     */
    val includedBeforeThis: Set<VirtualFile> = emptySet()
)

/**
 * Service used to create and update [TableGenContext] instances within [TableGenFile]s.
 */
@Service(Service.Level.PROJECT)
class TableGenContextService(val project: Project, private val cs: CoroutineScope) : Disposable {

    companion object {
        private val LOGGER = logger<TableGenContextService>()
    }

    override fun dispose() {

    }

    /**
     * Modification tracker which gets incremented each time an 'include' directive may resolve to a new file.
     */
    val includeResultModificationTracker: ModificationTracker
        get() = contextChangedModificationTracker

    /**
     * Modification tracker which gets incremented each time a context changes.
     */
    val contextChangedModificationTracker: ModificationTracker
        field = SimpleModificationTracker()

    private inner class Contexts(val file: VirtualFile) : Closeable {
        /**
         * Updates the contained contexts using [function] and performs a [refresh] iff the context changed.
         * Suspends until the fresh is complete.
         */
        suspend fun updateAndRefresh(
            function: (PersistentMap<VirtualFile, TableGenContext>) -> PersistentMap<VirtualFile, TableGenContext>
        ) {
            var refresh = true
            synchronized(this) {
                val newContexts = function(myContexts)
                if (newContexts == myContexts) {
                    return
                }
                if (newContexts.firstOrNull() == myContexts.firstOrNull()) refresh = false

                myContexts = newContexts
            }
            if (!refresh) return

            val epoch = myWriteActionRequestedFlow.updateAndGet { it + 1 }
            myWriteActionCompletedFlow.first { it >= epoch }
        }

        /**
         * Refreshes all effects this file has on included files and propagates a new context to them if needed.
         * Suspends until all context changes that this refresh caused have finished.
         */
        suspend fun refresh() {
            val epoch = myRefreshRequestedFlow.updateAndGet { it + 1 }
            myRefreshCompletedFlow.first { it >= epoch }
        }

        override fun close() = myJob.cancel()

        val contextToUse: TableGenContext?
            get() = synchronized(this) {
                myContexts.firstOrNull()?.value
            }

        private val myRefreshCompletedFlow = MutableStateFlow(0L)
        private val myRefreshRequestedFlow = MutableStateFlow(0L)
        private val myWriteActionCompletedFlow = MutableStateFlow(0L)
        private val myWriteActionRequestedFlow = MutableStateFlow(0L)

        private val myJob = cs.launch {
            try {
                coroutineScope {
                    launch {
                        myRefreshRequestedFlow.collectLatest { epoch ->
                            if (epoch <= 0) return@collectLatest

                            pushUpdatesToIncludesAfterNewContext(file)
                            myRefreshCompletedFlow.emit(epoch)
                        }
                    }
                    launch {
                        myWriteActionRequestedFlow.collectLatest { epoch ->
                            if (epoch <= 0) return@collectLatest

                            val fileManager = fileManager.await()
                            // Invalidate the Psi such that the 'TableGenFile' instance gets reallocated.
                            writeAction {
                                // Write actions before may have made the file invalid.
                                if (!file.isValid) return@writeAction

                                fileManager.setViewProvider(file, null)
                            }

                            readAction {
                                // Write actions before may have made the file invalid.
                                if (!file.isValid) return@readAction

                                // Massive workaround for stale indices.
                                // IntelliJ technically only allows file content dependent Psis and indices created
                                // from that Psi. However, TableGen is not context dependent due to the preprocessor
                                // and includes. We keep the index up to date by requesting reindexing when the context
                                // changes.
                                FileBasedIndex.getInstance().requestReindex(file)
                            }

                            // Propagate the new context further.
                            contextChangedModificationTracker.incModificationCount()
                            refresh()
                            myWriteActionCompletedFlow.emit(epoch)
                        }
                    }
                }
            } finally {
                // Unblock any coroutines waiting by now considering all requests completed from now on.
                myRefreshCompletedFlow.value = Long.MAX_VALUE
                myWriteActionCompletedFlow.value = Long.MAX_VALUE
            }
        }
        private var myContexts = persistentMapOf<VirtualFile, TableGenContext>()
    }

    private val myFileToContexts: MutableMap<VirtualFile, Contexts> = mutableMapOf()
    private val myLock = ReentrantReadWriteLock()
    private val fileManager = cs.async {
        (project.service<PsiManager>() as PsiManagerEx).fileManager
    }

    private suspend fun refreshAllFiles() = coroutineScope {
        myLock.read {
            myFileToContexts.values.toList()
        }.forEach {
            launch {
                it.refresh()
            }
        }
    }

    private suspend fun refreshFile(file: VirtualFile) {
        myLock.read {
            myFileToContexts[file]
        }?.refresh()
    }

    init {
        // Refresh the world if any TableGen file is added or removed as any include resolution might now change.
        project.messageBus.connect(cs).subscribe(
            FileTypeIndex.INDEX_CHANGE_TOPIC, object : FileTypeIndex.IndexChangeListener {
                override fun onChangedForFileType(fileType: FileType) {
                    if (fileType != TableGenFileType.INSTANCE) return
                    contextChangedModificationTracker.incModificationCount()
                    cs.launch {
                        val startTime = System.nanoTime()
                        refreshAllFiles()
                        val endTime = System.nanoTime()
                        LOGGER.info(
                            "Updating contexts after file type change took ${(endTime - startTime) / 1.0e9} seconds"
                        )
                    }
                }
            })
        // Refresh the context propagated from a file if its Psi changes.
        PsiManager.getInstance(project).addPsiTreeChangeListener(object : PsiTreeAnyChangeAbstractAdapter() {
            override fun onChange(file: PsiFile?) {
                if (file !is TableGenFile) return

                val vf = file.virtualFile ?: return
                cs.launch {
                    refreshFile(vf)
                }
            }
        }, this)

        cs.launch {
            // Apply value changes to all files mentioned in the compile commands.
            project.service<CompilationCommands>().stateFlow.collectLatest { state ->
                if (state == CompilationCommandsState()) return@collectLatest

                DumbService.getInstance(project).queueTask(object : DumbModeTask() {
                    override fun performInDumbMode(indicator: ProgressIndicator) {
                        indicator.isIndeterminate = false
                        indicator.text = MyBundle.message("tableGen.progress.updatingContexts")
                        runBlockingCancellable {
                            val startTime = System.nanoTime()
                            // Drive the fraction off the compile-command entries, the bulk of the work. Modern
                            // 'reportProgress' is not visible through 'runBlockingCancellable' under an indicator, so we
                            // update the indicator directly.
                            val total = state.map.size
                            val completed = AtomicInteger()
                            coroutineScope {
                                val updated = mutableSetOf<VirtualFile>()
                                state.map.forEach { (key, value) ->
                                    launch {
                                        getContextsForFile(key).updateAndRefresh {
                                            persistentMapOf(
                                                key to TableGenContext(includePaths = value.paths)
                                            )
                                        }
                                        if (total > 0) indicator.fraction =
                                            completed.incrementAndGet().toDouble() / total
                                    }
                                    updated.add(key)
                                }

                                // Erase all entries from previous compile commands.
                                myLock.read {
                                    myFileToContexts.toList()
                                }.forEach { (key, value) ->
                                    if (updated.contains(key)) return@forEach

                                    launch {
                                        // Roots use themselves as keys.
                                        value.updateAndRefresh {
                                            it.remove(key)
                                        }
                                    }
                                }
                            }
                            val endTime = System.nanoTime()
                            LOGGER.info(
                                "Updating contexts after compile commands change took ${(endTime - startTime) / 1.0e9} seconds"
                            )
                        }
                    }
                })
            }
        }
    }

    private fun getContextsForFile(file: VirtualFile) = myLock.read {
        myFileToContexts[file]?.let { return@read it }
        // Upgrade to a write lock otherwise. Note that multiple threads with the same key may be queuing for the write
        // lock here.
        // Using [computeIfAbsent] is therefore a requirement to make sure all but the first thread will immediately
        // return from the write lock without insertion in such a scenario.
        myLock.write {
            myFileToContexts.computeIfAbsent(file) {
                Contexts(it)
            }
        }
    }

    private suspend fun pushUpdatesToIncludesAfterNewContext(updatedFile: VirtualFile): Unit = coroutineScope {
        val instance = project.service<PsiManager>()

        // Limit the read action from the Psi read to let write actions run as soon as possible.
        val (included, newContext) = readAction {
            if (!updatedFile.isValid) return@readAction emptyList<VirtualFile>() to null

            val tableGenFile =
                instance.findFile(updatedFile) as? TableGenFile ?: return@readAction emptyList<VirtualFile>() to null

            tableGenFile.includeDirectives.mapNotNull {
                ProgressManager.checkCanceled()
                it.includedFile
            }.toList() to tableGenFile.context
        }
        if (newContext == null) {
            myLock.write {
                // Garbage collect deleted files.
                myFileToContexts.remove(updatedFile)?.close()
            }
            return@coroutineScope
        }

        val currentDefines = mutableSetOf<String>()
        val includedSoFar = newContext.includedBeforeThis.toMutableSet()
        included.forEach { file ->
            checkCanceled()

            // Recursive include! Ignore these.
            if (file == updatedFile) return@forEach

            val context = TableGenContext(
                newContext.includedFrom.add(updatedFile), newContext.includePaths, currentDefines, includedSoFar.toSet()
            )
            launch {
                getContextsForFile(file).updateAndRefresh { existing ->
                    existing.put(updatedFile, context)
                }
            }
            includedSoFar.add(file)
        }

        // Remove old newContext from all files where it no longer applies.
        myLock.read {
            // TODO: This is O(files) when it could be O(prev(includes)).
            myFileToContexts.forEach { (key, value) ->
                if (includedSoFar.contains(key)) return@forEach
                if (key == updatedFile) return@forEach

                launch {
                    value.updateAndRefresh {
                        it.remove(updatedFile)
                    }
                }
            }
        }
    }

    fun getActiveContext(virtualFile: VirtualFile) = myLock.read {
        myFileToContexts[virtualFile.originalFileOrSelf()]?.contextToUse ?: TableGenContext()
    }

    @RequiresReadLock
    private fun getAllIncludedFiles(file: TableGenFile, result: MutableSet<VirtualFile>) {
        val instance = PsiManager.getInstance(project)
        val workList = mutableListOf(file)
        while (workList.isNotEmpty()) {
            val last = workList.removeLast()

            workList += last.includeDirectives.mapNotNull {
                it.includedFile
            }.mapNotNull {
                if (!it.isValid) return@mapNotNull null

                if (!result.add(it)) return@mapNotNull null
                instance.findFile(it) as? TableGenFile
            }
        }
    }

    /**
     * Returns the set of all files that are included in [file], directly, transitively or as part of the active
     * context of the file.
     */
    @RequiresReadLock
    fun getIncludedFiles(file: TableGenFile): Set<VirtualFile> = CachedValuesManager.getCachedValue(file) {
        val result = mutableSetOf<VirtualFile>()
        val instance = PsiManager.getInstance(project)
        file.virtualFile?.let { vf ->
            // Add from context.
            val context = getActiveContext(vf)
            // Include all files in which we are included from. We do not need to recurse into them as all included
            // files prior to [file] are already part of [includedBeforeThis].
            // However, if we wanted to be very accurate, we should only allow definitions to be found in these files
            // prior to the include statement that cause this chain!
            result += context.includedFrom

            context.includedBeforeThis.asSequence().mapNotNull {
                if (!it.isValid) return@mapNotNull null

                if (!result.add(it)) return@mapNotNull null

                instance.findFile(it) as? TableGenFile
            }.forEach {
                getAllIncludedFiles(it, result)
            }
        }
        getAllIncludedFiles(file, result)
        CachedValueProvider.Result.create(result, result + file + contextChangedModificationTracker)
    }
}
