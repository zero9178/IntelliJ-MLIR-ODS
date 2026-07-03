package com.github.zero9178.mlirods.model

import com.github.zero9178.mlirods.MyBundle
import com.github.zero9178.mlirods.language.TableGenFileType
import com.github.zero9178.mlirods.language.psi.TableGenFile
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.readAction
import com.intellij.openapi.application.readAndBackgroundWriteAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.checkCanceled
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.ModificationTracker
import com.intellij.openapi.util.SimpleModificationTracker
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.originalFileOrSelf
import com.intellij.platform.ide.progress.withBackgroundProgress
import com.intellij.platform.util.progress.reportProgressScope
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
import kotlinx.collections.immutable.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.update
import org.jetbrains.annotations.TestOnly
import java.io.Closeable
import java.util.concurrent.locks.ReentrantReadWriteLock
import java.util.concurrent.locks.StampedLock
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
    val includedBeforeThis: PersistentSet<VirtualFile> = persistentSetOf()
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
        field = SimpleModificationTracker()

    /**
     * Modification tracker which gets incremented each time a context changes.
     */
    val contextChangedModificationTracker: ModificationTracker = ModificationTracker { contextGeneration.value }

    /**
     * Generation counter bumped in lockstep with [contextChangedModificationTracker].
     * Consumers can collect this [StateFlow] to react to context changes.
     */
    val contextGeneration: StateFlow<Long>
        field = MutableStateFlow(0L)

    /**
     * Returns the set of files that currently have an active context, i.e. the compile-command roots and every file
     * transitively included from one of them.
     */
    fun getFilesWithContext(): Set<VirtualFile> = myLock.read {
        myFileToContexts.mapNotNullTo(mutableSetOf()) { (file, contexts) ->
            file.takeIf { contexts.contextToUse != null }
        }
    }

    /**
     * [SharedFlow] that receives the compilation commands state any time that state has been finished applying.
     */
    @TestOnly
    val finishedCompileCommands: SharedFlow<CompilationCommandsState>
        field = MutableStateFlow(CompilationCommandsState())

    private val myFileToContexts: MutableMap<VirtualFile, Contexts> = mutableMapOf()
    private val myLock = ReentrantReadWriteLock()
    private val fileManager = cs.async {
        (project.service<PsiManager>() as PsiManagerEx).fileManager
    }

    init {
        // Refresh the world if any TableGen file is added or removed as any include resolution might now change.
        project.messageBus.connect(cs).subscribe(
            FileTypeIndex.INDEX_CHANGE_TOPIC, object : FileTypeIndex.IndexChangeListener {
                override fun onChangedForFileType(fileType: FileType) {
                    if (fileType != TableGenFileType.INSTANCE) return
                    includeResultModificationTracker.incModificationCount()
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

        cs.launch(start = CoroutineStart.UNDISPATCHED) {
            // Apply value changes to all files mentioned in the compile commands.
            project.service<CompilationCommands>().stateFlow.collectLatest { state ->
                withBackgroundProgress(project, MyBundle.message("tableGen.progress.updatingContexts")) {
                    val startTime = System.nanoTime()
                    // Drive the fraction off the compile-command entries, the bulk of the work.
                    // 'withBackgroundProgress' installs a reporter into the coroutine context, which
                    // 'runBlockingCancellable' inherits, so 'reportProgressScope' is visible here.
                    reportProgressScope(state.map.size) { reporter ->
                        val updated = mutableSetOf<VirtualFile>()
                        state.map.forEach { (key, value) ->
                            launch(start = CoroutineStart.UNDISPATCHED) {
                                reporter.itemStep(MyBundle.message("tableGen.progress.updatingContext", key.name)) {
                                    getContextsForFile(key).updateAndRefresh(this) {
                                        persistentMapOf(
                                            key to TableGenContext(includePaths = value.paths)
                                        )
                                    }?.join()
                                }
                            }
                            updated.add(key)
                        }

                        // Erase all entries from previous compile commands.
                        myLock.read {
                            myFileToContexts.toList()
                        }.forEach { (key, value) ->
                            if (updated.contains(key)) return@forEach

                            // Roots use themselves as keys.
                            value.updateAndRefresh(this) {
                                it.remove(key)
                            }
                        }
                    }
                    val endTime = System.nanoTime()
                    LOGGER.info(
                        "Updating contexts after compile commands change took ${(endTime - startTime) / 1.0e9} seconds"
                    )
                }
                finishedCompileCommands.emit(state)
            }
        }
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

    private inner class Contexts(val file: VirtualFile) : Closeable {
        /**
         * Updates the contained contexts using [function] and performs a [refresh] iff the context changed.
         * The refresh is performed as a separate job launched within [cs] that is returned by this method.
         */
        fun updateAndRefresh(
            cs: CoroutineScope,
            function: (PersistentMap<VirtualFile, TableGenContext>) -> PersistentMap<VirtualFile, TableGenContext>
        ): Job? {
            // Start out holding only a read lock to compute the new contexts and upgrade to a write lock lazily, as the
            // common case is that no mutation is required.
            var stamp = myLock.readLock()
            val (oldContext, newContext) = try {
                var newContexts = function(myContexts)
                // Nothing changed; the read lock was all we ever needed.
                if (newContexts == myContexts) {
                    return null
                }

                // A mutation is required, so upgrade to a write lock. The upgrade may fail under contention, in which
                // case we drop the read lock, acquire the write lock outright and recompute against the now up-to-date
                // contexts.
                val writeStamp = myLock.tryConvertToWriteLock(stamp)
                if (writeStamp != 0L) {
                    stamp = writeStamp
                } else {
                    myLock.unlockRead(stamp)
                    stamp = myLock.writeLock()
                    newContexts = function(myContexts)
                    if (newContexts == myContexts) {
                        return null
                    }
                }

                if (newContexts.firstOrNull() == myContexts.firstOrNull()) {
                    // TODO: Refresh currently only takes the active context into account meaning no refresh is needed
                    //       if the active context doesn't change.
                    myContexts = newContexts
                    contextGeneration.update { it + 1 }
                    return null
                }

                val old = myContexts
                myContexts = newContexts
                old to newContexts
            } finally {
                myLock.unlock(stamp)
            }
            contextGeneration.update { it + 1 }
            if (newContext.firstOrNull()?.value?.includePaths != oldContext.firstOrNull()?.value?.includePaths) {
                includeResultModificationTracker.incModificationCount()
            }

            return cs.launch {
                val oldDefines = oldContext.firstOrNull()?.value?.defines.orEmpty()
                val newDefines = newContext.firstOrNull()?.value?.defines.orEmpty()
                val instance = PsiManager.getInstance(project)
                val fileManager = fileManager.await()
                readAndBackgroundWriteAction {
                    if (!file.isValid || project.isDisposed) return@readAndBackgroundWriteAction value(Unit)

                    val tableGenFile =
                        (instance.findFile(file) as? TableGenFile) ?: return@readAndBackgroundWriteAction value(
                            Unit
                        )
                    val needsReparse = tableGenFile.usedMacros.any {
                        oldDefines.contains(it) != newDefines.contains(it)
                    }
                    if (!needsReparse) value(Unit)
                    else writeAction {
                        fileManager.setViewProvider(file, null)
                        FileBasedIndex.getInstance().requestReindex(file)
                    }
                }

                refresh()
            }
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

        /**
         * Returns the active context of [file] that is used for include paths, defines and more.
         */
        val contextToUse: TableGenContext?
            get() {
                // Reading a single reference; an optimistic read avoids blocking concurrent writers.
                val stamp = myLock.tryOptimisticRead()
                val contexts = myContexts
                if (myLock.validate(stamp)) {
                    return contexts.firstOrNull()?.value
                }
                val readStamp = myLock.readLock()
                try {
                    return myContexts.firstOrNull()?.value
                } finally {
                    myLock.unlockRead(readStamp)
                }
            }

        private val myRefreshCompletedFlow = MutableStateFlow(0L)
        private val myRefreshRequestedFlow = MutableStateFlow(0L)
        private val myLock = StampedLock()

        private val myJob = cs.launch(start = CoroutineStart.UNDISPATCHED) {
            try {
                myRefreshRequestedFlow.collect { epoch ->
                    if (epoch <= 0) return@collect

                    pushUpdatesToIncludesAfterNewContext(file)
                    myRefreshCompletedFlow.emit(epoch)
                }
            } finally {
                // Unblock any coroutines waiting by now considering all requests completed from now on.
                myRefreshCompletedFlow.value = Long.MAX_VALUE
            }
        }
        private var myContexts = persistentMapOf<VirtualFile, TableGenContext>()
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
            if (!updatedFile.isValid || project.isDisposed) return@readAction emptyList<VirtualFile>() to null

            val tableGenFile =
                instance.findFile(updatedFile) as? TableGenFile ?: return@readAction emptyList<VirtualFile>() to null

            tableGenFile.includeDirectives.mapNotNull {
                ProgressManager.checkCanceled()
                it.includedFile
            }.toList() to tableGenFile.context
        }
        checkCanceled()
        if (newContext == null) {
            myLock.write {
                // Garbage collect deleted files.
                myFileToContexts.remove(updatedFile)?.close()
            }
            return@coroutineScope
        }

        val currentDefines = mutableSetOf<String>()
        var includedSoFar = newContext.includedBeforeThis
        included.forEach { file ->
            checkCanceled()

            // Recursive include! Ignore these.
            if (file == updatedFile) return@forEach

            val context = TableGenContext(
                newContext.includedFrom.add(updatedFile), newContext.includePaths, currentDefines, includedSoFar
            )
            getContextsForFile(file).updateAndRefresh(this) { existing ->
                existing.put(updatedFile, context)
            }
            includedSoFar = includedSoFar.add(file)
        }

        // Remove old newContext from all files where it no longer applies.
        myLock.read {
            // TODO: This is O(files) when it could be O(prev(includes)).
            myFileToContexts.forEach { (key, value) ->
                if (includedSoFar.contains(key)) return@forEach
                if (key == updatedFile) return@forEach

                value.updateAndRefresh(this) {
                    it.remove(updatedFile)
                }
            }
        }
    }

    /**
     * Returns the context that is used to parse [virtualFile] or an empty context if none exists.
     */
    fun getActiveContext(virtualFile: VirtualFile): TableGenContext = myLock.read {
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
