package com.github.zero9178.mlirods.model

import com.github.zero9178.mlirods.language.TableGenFile
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
import com.intellij.openapi.util.SimpleModificationTracker
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.originalFileOrSelf
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiTreeAnyChangeAbstractAdapter
import com.intellij.psi.impl.PsiManagerEx
import com.intellij.psi.search.FileTypeIndex
import com.intellij.util.indexing.FileBasedIndex
import com.jetbrains.rd.util.firstOrNull
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

data class TableGenContext(
    val includePaths: List<VirtualFile> = emptyList(),
    val defines: Set<String> = emptySet(),
    val inScopeFiles: Set<VirtualFile> = emptySet()
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
                        key to TableGenContext(value.paths)
                    )
                    updated.add(key)
                }

                // Erase all entries from previous compile commands.
                myLock.read {
                    myFileToContexts.toList()
                }.forEach { (key, value) ->
                    if (updated.contains(key)) return@forEach

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
                            updateFromNewContext(file)
                        }
                    }
                }
                Value(flow, job, refreshFlow)
            }.stateFlow
        }
    }

    private suspend fun updateFromNewContext(updated: VirtualFile) {
        val instance = project.serviceAsync<PsiManager>()

        // Limit the read action from the Psi read to let write actions run as soon as possible.
        val (included, context) = readAction {
            if (!updated.isValid) return@readAction emptyList<VirtualFile>() to null

            val tableGenFile =
                instance.findFile(updated) as? TableGenFile ?: return@readAction emptyList<VirtualFile>() to null

            tableGenFile.includeDirectives.mapNotNull {
                ProgressManager.checkCanceled()
                it.includedFile
            }.toList() to tableGenFile.context
        }
        if (context == null) {
            myLock.write {
                // Garbage collect deleted files.
                myFileToContexts.remove(updated)?.reactiveJob?.cancel()
            }
            return
        }

        val currentDefines = mutableSetOf<String>()
        val includedSoFar = mutableSetOf<VirtualFile>()
        included.forEach { file ->
            checkCanceled()

            // We use the same file as roots for any compile commands, so avoid adding them here.
            if (file == updated) return@forEach

            val context = TableGenContext(
                context.includePaths, currentDefines, includedSoFar.toSet()
            )
            stateFlowForFile(file).update { existing ->
                existing.put(updated, context)
            }
            includedSoFar.add(file)
        }

        // Remove old context from all files where it no longer applies.
        myLock.read {
            // TODO: This is O(files) when it could be O(prev(includes)).
            myFileToContexts.forEach { (key, value) ->
                if (includedSoFar.contains(key)) return@forEach
                if (key == updated) return@forEach

                value.stateFlow.update {
                    it.remove(updated)
                }
            }
        }
    }

    fun getActiveContext(virtualFile: VirtualFile) = myLock.read {
        myFileToContexts[virtualFile.originalFileOrSelf()]?.stateFlow?.value?.firstOrNull()?.value ?: TableGenContext()
    }

    /**
     * Returns the set of all files that are included in [file], directly, transitively or as part of the active
     * context of the file.
     */
    fun getIncludedFiles(file: TableGenFile): Set<VirtualFile> {
        // Worklist of the current tablegen file that we are trying to get all transitive includes of.
        val workList = mutableListOf(file)
        val rToT: (VirtualFile) -> TableGenFile? = {
            PsiManager.getInstance(project).findFile(it) as? TableGenFile
        }
        val result = mutableSetOf<VirtualFile>()
        file.virtualFile?.let { vf ->
            // Add from context.
            val context = getActiveContext(vf)
            result += context.inScopeFiles
            workList += result.mapNotNull(rToT)
        }

        while (true) {
            val current = workList.removeLastOrNull() ?: break

            current.includeDirectives.mapNotNull {
                it.includedFile
            }.forEach {
                // No need to add to worklist again if already seen.
                if (!result.add(it)) return@forEach

                rToT(it)?.run {
                    workList.add(this)
                }
            }
        }
        return result
    }
}