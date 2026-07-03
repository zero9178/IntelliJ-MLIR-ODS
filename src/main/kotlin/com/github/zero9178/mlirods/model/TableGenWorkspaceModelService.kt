package com.github.zero9178.mlirods.model

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.components.serviceAsync
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.backend.workspace.WorkspaceModel
import com.intellij.platform.workspace.jps.entities.ContentRootEntity
import com.intellij.platform.workspace.jps.entities.ModuleEntity
import com.intellij.platform.workspace.storage.EntitySource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.jetbrains.annotations.TestOnly

/**
 * [EntitySource] tagging all workspace entities created by [TableGenWorkspaceModelService]. It is used to recognize and
 * remove the entities we previously contributed when the set of included files changes.
 */
internal object TableGenEntitySource : EntitySource

/**
 * Name of the synthetic module holding the TableGen content roots.
 */
private const val MODULE_NAME = "TableGen Roots"

/**
 * Project service maintaining a synthetic module in the workspace model whose content roots are exactly the TableGen
 * files that currently have a context, i.e. the compile-command roots and every file transitively included from one of
 * them (see [TableGenContextService.getFilesWithContext]).
 *
 * Registering these files as content roots makes them indexed and part of
 * [com.intellij.psi.search.GlobalSearchScope.allScope], even when they live outside the project's own content (e.g. the
 * MLIR/LLVM headers a TableGen file includes).
 */
@Service(Service.Level.PROJECT)
class TableGenWorkspaceModelService(private val project: Project, cs: CoroutineScope) {

    companion object {
        private val LOGGER = logger<TableGenWorkspaceModelService>()
    }

    /**
     * [StateFlow] holding the latest [TableGenContextService.contextGeneration] whose files have finished being applied
     * as content roots.
     */
    @TestOnly
    val finishedGeneration: StateFlow<Long>
        field = MutableStateFlow(-1L)

    init {
        cs.launch(start = CoroutineStart.UNDISPATCHED) {
            val contextService = project.service<TableGenContextService>()
            // Re-derive the content roots every time contexts change; 'collectLatest' coalesces bursts of changes.
            contextService.contextGeneration.collectLatest { generation ->
                val startTime = System.nanoTime()
                updateContentRoots(contextService.getFilesWithContext())
                val endTime = System.nanoTime()
                LOGGER.info("Updating content roots took ${(endTime - startTime) / 1.0e9} seconds")
                finishedGeneration.value = generation
            }
        }
    }

    private suspend fun updateContentRoots(files: Set<VirtualFile>) {
        val workspaceModel = project.serviceAsync<WorkspaceModel>()
        val urlManager = workspaceModel.getVirtualFileUrlManager()
        workspaceModel.update("Update TableGen content roots") { storage ->

            val roots = files.asSequence()
                .map { urlManager.getOrCreateFromUrl(it.url) }
                .toList()

            // Drop the module contributed previously; removing it cascades to its content roots.
            storage.entities(ModuleEntity::class.java)
                .filter { it.entitySource == TableGenEntitySource }
                .toList()
                .forEach { storage.removeEntity(it) }

            if (roots.isEmpty()) return@update

            storage.addEntity(
                ModuleEntity(MODULE_NAME, emptyList(), TableGenEntitySource) {
                    contentRoots = roots.map { url ->
                        ContentRootEntity(url, emptyList(), TableGenEntitySource)
                    }
                }
            )
        }
    }
}
