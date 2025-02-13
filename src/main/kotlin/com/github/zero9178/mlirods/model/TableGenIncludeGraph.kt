package com.github.zero9178.mlirods.model

import com.github.zero9178.mlirods.index.INCLUDED_INDEX
import com.github.zero9178.mlirods.language.TableGenLanguage
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.IndexNotReadyException
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.stubs.StubIndex
import com.intellij.psi.util.PsiModificationTracker
import com.intellij.util.concurrency.annotations.RequiresBlockingContext
import com.intellij.util.concurrency.annotations.RequiresEdt
import com.intellij.util.concurrency.annotations.RequiresReadLock
import com.intellij.util.messages.impl.subscribeAsFlow
import com.jetbrains.rd.util.concurrentMapOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import org.jetbrains.annotations.VisibleForTesting
import java.nio.file.Path
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

/**
 * Service used for functionality related to TableGen's 'include' mechanism.
 */
@Service(Service.Level.PROJECT)
class TableGenIncludeGraph(private val project: Project, cs: CoroutineScope) {

    // Note: This class is on purpose rather "racy", as tons of actions can at any point in time change the include
    // graph. We therefore only try to ensure consistency at any point in time. That is, for the currently received
    // compile commands and up-to-date index, the cache may only ever contain idempotent results based on these.
    private val myCacheMap = concurrentMapOf<VirtualFile, List<Path>>()
    private val myIndividualFileLocks = concurrentMapOf<VirtualFile, Any>()
    private val myWholeMapLock = ReentrantReadWriteLock()

    @VisibleForTesting
    val myBaseMapFlow = MutableStateFlow(emptyMap<VirtualFile, List<Path>>())

    init {
        // Clear cache on:
        // * Exiting dumb mode.
        project.messageBus.connect(cs).subscribe(DumbService.DUMB_MODE, object : DumbService.DumbModeListener {
            @RequiresEdt
            override fun exitDumbMode() {
                // Method runs on the EDT and therefore has exclusive access to the map.
                // The methods below only run in read actions and will see the newly available index once restarted.
                myCacheMap.clear()
                myIndividualFileLocks.clear()
            }
        })
        // * PSI modifications.
        cs.launch {
            val tracker = PsiModificationTracker.getInstance(project).forLanguage(TableGenLanguage.INSTANCE)
            project.messageBus.subscribeAsFlow(PsiModificationTracker.TOPIC) {
                send(tracker.modificationCount)
                PsiModificationTracker.Listener {
                    trySend(tracker.modificationCount)
                }
            }.distinctUntilChanged().collect {
                myWholeMapLock.write {
                    myCacheMap.clear()
                    myIndividualFileLocks.clear()
                }
            }
        }
        // * Compile commands changes.
        cs.launch {
            project.service<CompilationCommands>().stateFlow.collect { state ->
                myWholeMapLock.write {
                    myCacheMap.clear()
                    myIndividualFileLocks.clear()
                    myBaseMapFlow.value = state.map.mapValues { it.value.paths }
                }
            }
        }
    }

    private fun getContextFromIncludingFiles(tableGenFile: VirtualFile) = myWholeMapLock.read {
        synchronized(myIndividualFileLocks.computeIfAbsent(tableGenFile) { Any() }) {
            val fileName = tableGenFile.name
            myCacheMap[tableGenFile]?.let { return it }
            // Prevent infinite recursion in odd cases.
            myCacheMap[tableGenFile] = emptyList()
            // TODO: This is technically incorrect as there might be false positives due to key choice of being just the
            //  filename. The negative effect would be that incorrect include paths may be used for a file.
            // TODO: This currently takes the first occurrence. We could have multiple resolution contexts as CLion
            //  does for C++ headers.
            val result = try {
                StubIndex.getInstance().getContainingFilesIterator(
                    INCLUDED_INDEX, fileName, project, GlobalSearchScope.projectScope(project)
                ).asSequence().firstNotNullOfOrNull {
                    ProgressManager.checkCanceled()
                    getIncludePathsInternal(it)
                }
            } finally {
                // In case any exception is thrown, we need to remove the temporary recursion prevention value.
                myCacheMap.remove(tableGenFile)
            }
            myCacheMap[tableGenFile] = result ?: emptyList()
            result
        }
    }

    private fun getIncludePathsInternal(tableGenFile: VirtualFile): List<Path>? {
        val entry = myBaseMapFlow.value[tableGenFile]
        if (entry != null) return entry

        return getContextFromIncludingFiles(tableGenFile)
    }

    @RequiresReadLock
    @RequiresBlockingContext
    fun getIncludePaths(tableGenFile: VirtualFile): List<Path> {
        if (DumbService.isDumb(project)) throw IndexNotReadyException.create()

        return getIncludePathsInternal(tableGenFile) ?: emptyList()
    }
}