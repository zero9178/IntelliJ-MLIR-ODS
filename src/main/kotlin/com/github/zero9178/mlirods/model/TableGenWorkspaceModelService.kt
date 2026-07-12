package com.github.zero9178.mlirods.model

import com.intellij.openapi.application.readAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.components.serviceAsync
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.backend.workspace.WorkspaceModel
import com.intellij.platform.workspace.jps.entities.ContentRootEntity
import com.intellij.platform.workspace.jps.entities.ModuleEntity
import com.intellij.platform.workspace.storage.EntityChange
import com.intellij.platform.workspace.storage.EntitySource
import com.intellij.platform.workspace.storage.VersionedStorageChange
import com.intellij.platform.workspace.storage.WorkspaceEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
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
            val workspaceModel = project.serviceAsync<WorkspaceModel>()

            // Re-derive the content roots on two signals:
            //  - the set of files-with-context changed (contextGeneration), and
            //  - another module's roots changed, e.g. CMake reconfigured. updateContentRoots drops files another
            //    module already owns, so that decision goes stale when those modules' content roots move.
            val foreignRootChanges = workspaceModel.eventLog.filter { it.hasForeignRootChange() }
            merge(contextService.contextGeneration.map { }, foreignRootChanges.map { }).collectLatest {
                val generation = contextService.contextGeneration.value
                val startTime = System.nanoTime()
                updateContentRoots(contextService.getFilesWithContext())
                val endTime = System.nanoTime()
                LOGGER.info("Updating content roots took ${(endTime - startTime) / 1.0e9} seconds")
                finishedGeneration.value = generation
            }
        }
    }

    /**
     * Whether [this] change touches a module or content root that is not one of ours. Such a change may shift which
     * files other modules own, invalidating the filtering [updateContentRoots] performs; changes to our own entities
     * (tagged [TableGenEntitySource]) are ignored so reacting to them cannot loop back into another update.
     */
    private fun VersionedStorageChange.hasForeignRootChange(): Boolean =
        getChanges(ModuleEntity::class.java).any { it.isForeign() } ||
                getChanges(ContentRootEntity::class.java).any { it.isForeign() }

    private fun EntityChange<out WorkspaceEntity>.isForeign(): Boolean {
        val entity = newEntity ?: oldEntity
        return entity != null && entity.entitySource != TableGenEntitySource
    }

    private suspend fun updateContentRoots(files: Set<VirtualFile>) {
        val workspaceModel = project.serviceAsync<WorkspaceModel>()
        val urlManager = workspaceModel.getVirtualFileUrlManager()

        // Filter out files another module already owns before mutating the model. Contributing a content root for them
        // would overlap that module's content and make the file ambiguously belong to two modules.
        // Note: Technically [WorkspaceModel.eventLog] does not guarantee this index to be up-to-date but there is
        // currently no better mechanism.
        val fileIndex = ProjectFileIndex.getInstance(project)
        val ownFiles = readAction {
            files.filter { file ->
                val owner = fileIndex.getModuleForFile(file)
                owner == null || owner.name == MODULE_NAME
            }
        }

        workspaceModel.update("Update TableGen content roots") { storage ->
            val roots = ownFiles.map { urlManager.getOrCreateFromUrl(it.url) }

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
