package com.github.zero9178.mlirods.model

import com.github.zero9178.mlirods.language.TableGenFile
import com.github.zero9178.mlirods.language.TableGenFileType
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.readAction
import com.intellij.openapi.application.writeAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.ModificationTracker
import com.intellij.openapi.util.SimpleModificationTracker
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiTreeAnyChangeAbstractAdapter
import com.intellij.psi.impl.PsiManagerEx
import com.intellij.psi.search.FileTypeIndex
import com.intellij.util.AstLoadingFilter
import com.intellij.util.concurrency.annotations.RequiresReadLock
import com.jetbrains.rd.util.firstOrNull
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
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
@Service(Service.Level.PROJECT)
class TableGenContextService(val project: Project, private val cs: CoroutineScope) : Disposable {

    override fun dispose() {

    }

    init {
        cs.launch {
            // Apply value changes to all files mentioned in the compile commands.
            project.service<CompilationCommands>().stateFlow.collectLatest { state ->
                val updated = mutableSetOf<VirtualFile>()
                state.map.forEach {
                    if (it.key.fileType != TableGenFileType.INSTANCE) return@forEach

                    stateFlowForFile(it.key).value = persistentMapOf(
                        it.key to TableGenContext(it.value.paths)
                    )
                    updated.add(it.key)
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
                    launch {
                        flow.map {
                            // Arbitrary choice of picking the first context for now.
                            it.firstOrNull()?.value ?: TableGenContext()
                        }.distinctUntilChanged().collectLatest { context ->
                            // Invalidate the Psi such that the 'TableGenFile' instance gets reallocated.
                            writeAction {
                                if (!file.isValid) return@writeAction

                                val fileManager = PsiManagerEx.getInstanceEx(project).fileManager
                                fileManager.setViewProvider(file, null)
                            }
                            // Propagate the new context further.
                            myIncludeResultsModificationTracker.incModificationCount()
                            refreshFlow.emit(context)
                        }
                    }
                    launch {
                        // Cancel currently running action when a new request comes in.
                        refreshFlow.collectLatest {
                            readAction {
                                AstLoadingFilter.disallowTreeLoading<Throwable> {
                                    updateFromNewContext(file)
                                }
                            }
                        }
                    }
                }
                Value(flow, job, refreshFlow)
            }.stateFlow
        }
    }

    @RequiresReadLock
    private fun updateFromNewContext(updated: VirtualFile) {
        if (!updated.isValid || updated.fileType != TableGenFileType.INSTANCE) {
            myLock.write {
                // Garbage collect deleted files.
                myFileToContexts.remove(updated)?.reactiveJob?.cancel()
            }
            return
        }

        val tableGenFile = PsiManager.getInstance(project).findFile(updated) as? TableGenFile ?: return

        val currentDefines = mutableSetOf<String>()
        val includedSoFar = mutableSetOf<VirtualFile>()
        tableGenFile.includeDirectives.forEach { includeDirective ->
            ProgressManager.checkCanceled()

            val file = includeDirective.includedFile ?: return@forEach

            // We use the same file as roots for any compile commands, so avoid adding them here.
            if (file == updated) return@forEach

            val context = TableGenContext(
                tableGenFile.context.includePaths,
                currentDefines,
                includedSoFar.toSet()
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
        myFileToContexts[virtualFile]?.stateFlow?.value?.firstOrNull()?.value ?: TableGenContext()
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