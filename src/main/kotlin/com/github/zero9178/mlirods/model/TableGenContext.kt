package com.github.zero9178.mlirods.model

import com.github.zero9178.mlirods.language.psi.TableGenFile
import com.github.zero9178.mlirods.language.TableGenFileType
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.readAction
import com.intellij.openapi.application.writeAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.serviceAsync
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.checkCanceled
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.ModificationTracker
import com.intellij.openapi.util.RecursionManager
import com.intellij.openapi.util.SimpleModificationTracker
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.originalFileOrSelf
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiTreeAnyChangeAbstractAdapter
import com.intellij.psi.impl.PsiManagerEx
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.CachedValueProvider
import com.intellij.util.concurrency.annotations.RequiresReadLock
import com.intellij.util.indexing.FileBasedIndex
import com.jetbrains.rd.util.firstOrNull
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write
import kotlin.sequences.forEach
import kotlin.sequences.mapNotNull

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
@OptIn(ExperimentalCoroutinesApi::class)
@Service(Service.Level.PROJECT)
class TableGenContextService(val project: Project, private val cs: CoroutineScope) : Disposable {

    companion object {
        private val LOGGER = logger<TableGenContextService>()
    }

    override fun dispose() {

    }

    private val myIncludeResultsModificationTracker = SimpleModificationTracker()

    /**
     * Modification tracker which gets incremented each time an 'include' directive may resolve to a new file.
     */
    val includeResultModificationTracker: ModificationTracker
        get() = myIncludeResultsModificationTracker

    /**
     * Modification tracker which gets incremented each time a context changes.
     */
    val contextChangedModificationTracker: ModificationTracker
        get() = myIncludeResultsModificationTracker

    private data class Value(
        val stateFlow: MutableStateFlow<PersistentMap<VirtualFile, TableGenContext>>,
        val reactiveJob: Job,
        val refreshFlow: MutableSharedFlow<TableGenContext?>
    )

    private val myFileToContexts: MutableMap<VirtualFile, Value> = mutableMapOf()
    private val myLock = ReentrantReadWriteLock()
    private val fileManager = cs.async {
        (project.serviceAsync<PsiManager>() as PsiManagerEx).fileManager
    }

    // Extra flow purely used for profiling purposes. A replay of 1 makes sure that new subscribers immediately receive
    // at least one value. The extra buffer capacity reduces the chances of the emitting coroutine needing to suspend
    // greatly.
    private val myProfilingRefreshFlow = MutableSharedFlow<Pair<Long, Boolean>>(replay = 1, extraBufferCapacity = 63)

    init {
        // Coroutine for pure logging and profiling.
        cs.launch {
            // We consider a context up-date event to range from when a start even is triggered until no more refreshes
            // occur for a whole second.
            myProfilingRefreshFlow.filter { (time, start) ->
                start
            }
                // Ignore other start requests while the collector is still running.
                .conflate().collect { (startTime, start) ->
                    // Collect until the refresh flow does not receive a value for 1s.
                    // This works by delaying the collector by 1s and cancelling and restarting it immediately when a
                    // new value comes in.
                    // Using [take] we immediately complete collection once a single Unit value has been received.
                    myProfilingRefreshFlow.mapLatest { (endTime, start) ->
                        delay(timeMillis = 1000)
                        checkCanceled()

                        LOGGER.info(
                            "Updating contexts took ${(endTime - startTime) / 1.0e9} seconds"
                        )
                    }.take(1).collect()
                }
        }

        // Refresh the world if any TableGen file is added or removed as any include resolution might now change.
        project.messageBus.connect(cs).subscribe(
            FileTypeIndex.INDEX_CHANGE_TOPIC, object : FileTypeIndex.IndexChangeListener {
                override fun onChangedForFileType(fileType: FileType) {
                    if (fileType != TableGenFileType.INSTANCE) return

                    myIncludeResultsModificationTracker.incModificationCount()
                    myLock.read {
                        myFileToContexts.values.forEach {
                            it.refreshFlow.tryEmit(null)
                        }
                    }
                }
            })
        // Refresh the context propagated from a file if its Psi changes.
        PsiManager.getInstance(project).addPsiTreeChangeListener(object : PsiTreeAnyChangeAbstractAdapter() {
            override fun onChange(file: PsiFile?) {
                if (file !is TableGenFile) return

                val vf = file.virtualFile ?: return
                myLock.read {
                    myFileToContexts[vf]?.refreshFlow?.tryEmit(null)
                }
            }
        }, this)

        cs.launch {
            // Apply value changes to all files mentioned in the compile commands.
            project.serviceAsync<CompilationCommands>().stateFlow.collectLatest { state ->
                myProfilingRefreshFlow.emit(System.nanoTime() to true)

                val updated = mutableSetOf<VirtualFile>()
                state.map.forEach { (key, value) ->
                    stateFlowForFile(key).value = persistentMapOf(
                        key to TableGenContext(includePaths = value.paths)
                    )
                    updated.add(key)
                }

                // Erase all entries from previous compile commands.
                myLock.read {
                    myFileToContexts.toList()
                }.forEach { (key, value) ->
                    if (updated.contains(key)) return@forEach

                    // Roots use themselves as keys.
                    value.stateFlow.update {
                        it.remove(key)
                    }
                }
            }
        }
    }

    private fun stateFlowForFile(file: VirtualFile) = myLock.read {
        myFileToContexts[file]?.let { return@read it.stateFlow }
        // Upgrade to a write lock otherwise. Note that multiple threads with the same key may be queuing for the write
        // lock here.
        // Using [computeIfAbsent] is therefore a requirement to make sure all but the first thread will immediately
        // return from the write lock without insertion in such a scenario.
        myLock.write {
            myFileToContexts.computeIfAbsent(file) {
                // State flow containing all contexts.
                val flow = MutableStateFlow(persistentMapOf<VirtualFile, TableGenContext>())
                // Channel used to request refreshing all contexts produced by this file.
                // Multiple requests can then be coalesced to a single active computation.
                val refreshFlow = MutableSharedFlow<TableGenContext?>(
                    onBufferOverflow = BufferOverflow.DROP_OLDEST, extraBufferCapacity = 1
                )
                val job = cs.launch {
                    val fileManager = fileManager.await()
                    launch {
                        flow.map {
                            // Arbitrary choice of picking the first context for now.
                            it.firstOrNull()?.value ?: TableGenContext()
                        }.distinctUntilChanged().collectLatest { context ->
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
                            myIncludeResultsModificationTracker.incModificationCount()
                            refreshFlow.emit(context)
                        }
                    }
                    launch {
                        // Cancel currently running action when a new request comes in.
                        refreshFlow.collectLatest {
                            myProfilingRefreshFlow.emit(System.nanoTime() to false)
                            pushUpdatesToIncludesAfterNewContext(file)
                        }
                    }
                }
                Value(flow, job, refreshFlow)
            }.stateFlow
        }
    }

    private suspend fun pushUpdatesToIncludesAfterNewContext(updatedFile: VirtualFile) {
        val instance = project.serviceAsync<PsiManager>()

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
                myFileToContexts.remove(updatedFile)?.reactiveJob?.cancel()
            }
            return
        }

        val currentDefines = mutableSetOf<String>()
        val includedSoFar = newContext.includedBeforeThis.toMutableSet()
        included.forEach { file ->
            checkCanceled()

            // Recursive include! Ignore these.
            if (file == updatedFile) return@forEach

            val context = TableGenContext(
                newContext.includedFrom.add(updatedFile),
                newContext.includePaths, currentDefines, includedSoFar.toSet()
            )
            stateFlowForFile(file).update { existing ->
                existing.put(updatedFile, context)
            }
            includedSoFar.add(file)
        }

        // Remove old newContext from all files where it no longer applies.
        myLock.read {
            // TODO: This is O(files) when it could be O(prev(includes)).
            myFileToContexts.forEach { (key, value) ->
                if (includedSoFar.contains(key)) return@forEach
                if (key == updatedFile) return@forEach

                value.stateFlow.update {
                    it.remove(updatedFile)
                }
            }
        }
    }

    fun getActiveContext(virtualFile: VirtualFile) = myLock.read {
        myFileToContexts[virtualFile.originalFileOrSelf()]?.stateFlow?.value?.firstOrNull()?.value ?: TableGenContext()
    }

    @RequiresReadLock
    private fun getAllIncludedFiles(file: TableGenFile): Set<VirtualFile> = CachedValuesManager.getCachedValue(file) {
        RecursionManager.doPreventingRecursion(file, true) {
            val instance = PsiManager.getInstance(project)

            val result = mutableSetOf<VirtualFile>()
            file.includeDirectives.mapNotNull {
                it.includedFile
            }.mapNotNull {
                if (!it.isValid) return@mapNotNull null

                result += it
                instance.findFile(it) as? TableGenFile
            }.forEach {
                result += getAllIncludedFiles(it)
            }

            CachedValueProvider.Result.create(result, result + file + contextChangedModificationTracker)
        }
    } ?: emptySet()

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

                result += it
                instance.findFile(it) as? TableGenFile
            }.forEach {
                result += getAllIncludedFiles(it)
            }
        }
        result += getAllIncludedFiles(file)
        CachedValueProvider.Result.create(result, result + file + contextChangedModificationTracker)
    }
}
