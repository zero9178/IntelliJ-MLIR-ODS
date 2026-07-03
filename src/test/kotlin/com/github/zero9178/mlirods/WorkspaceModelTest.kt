package com.github.zero9178.mlirods

import com.github.zero9178.mlirods.index.CLASS_INDEX
import com.github.zero9178.mlirods.language.generated.psi.TableGenClassStatement
import com.github.zero9178.mlirods.model.IncludePaths
import com.github.zero9178.mlirods.model.TableGenContextService
import com.github.zero9178.mlirods.model.TableGenEntitySource
import com.github.zero9178.mlirods.model.TableGenWorkspaceModelService
import com.intellij.openapi.components.service
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.backend.workspace.WorkspaceModel
import com.intellij.platform.workspace.jps.entities.ModuleEntity
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.stubs.StubIndex
import com.intellij.testFramework.HeavyPlatformTestCase
import com.intellij.testFramework.IndexingTestUtil
import com.intellij.testFramework.PlatformTestUtil
import java.io.File

/**
 * Tests for [TableGenWorkspaceModelService]: every TableGen file that has a context – a compile-command root or a file
 * transitively included from one – is contributed as a content root, so it is indexed and part of
 * [GlobalSearchScope.allScope] even when it lives outside the project's own content.
 *
 * This uses a heavy (real) project because the service adds a module to the workspace model, which light test projects
 * forbid.
 */
class WorkspaceModelTest : HeavyPlatformTestCase() {

    /**
     * Returns a function that applies a new set of include paths and waits until both the context has settled and
     * [TableGenWorkspaceModelService] has contributed the resulting content roots.
     */
    private fun workspaceUpdater(): (Map<VirtualFile, IncludePaths>) -> Unit {
        val setCompileCommands = compileCommandsUpdater(project)
        val contextService = project.service<TableGenContextService>()
        val workspaceService = project.service<TableGenWorkspaceModelService>()

        return { map ->
            // Sets the compile commands and waits for the context service to finish propagating them.
            setCompileCommands(map)
            // The workspace service reacts to the context change off the EDT and commits via an EDT write action, so
            // keep pumping the event queue until it has caught up to the settled context generation.
            PlatformTestUtil.waitWithEventsDispatching(
                "TableGen content roots were not applied",
                { workspaceService.finishedGeneration.value == contextService.contextGeneration.value },
                10
            )
            IndexingTestUtil.waitUntilIndexesAreReady(project)
        }
    }

    /**
     * Creates a directory outside the project's content with a single TableGen [fileName] holding [content], returning
     * the directory and the created file.
     */
    private fun createExternalDir(fileName: String, content: String): Pair<VirtualFile, VirtualFile> {
        val ioDir = FileUtil.createTempDirectory("tablegen-external", null)
        val ioFile = File(ioDir, fileName).apply { writeText(content) }
        val lfs = LocalFileSystem.getInstance()
        val dir = lfs.refreshAndFindFileByIoFile(ioDir)!!
        val file = lfs.refreshAndFindFileByIoFile(ioFile)!!
        return dir to file
    }

    private fun ourContentRootUrls(): Set<String> =
        WorkspaceModel.getInstance(project).currentSnapshot.entities(ModuleEntity::class.java)
            .filter { it.entitySource == TableGenEntitySource }
            .flatMap { it.contentRoots }
            .map { it.url.url }
            .toSet()

    fun `test root and its included files become content roots`() {
        val (includeDir, included) = createExternalDir("included.td", "class Included;")
        val (_, root) = createExternalDir("root.td", "include \"included.td\"")

        val update = workspaceUpdater()
        update(mapOf(root to IncludePaths(listOf(includeDir))))

        // The root file and the file it includes both have a context, so both are content roots. The include directory
        // itself is not.
        assertContainsElements(ourContentRootUrls(), root.url, included.url)
        assertDoesntContain(ourContentRootUrls(), includeDir.url)
    }

    fun `test switching root drops files that lost their context`() {
        val (includeDir, included) = createExternalDir("included.td", "class Included;")
        val (_, rootA) = createExternalDir("rootA.td", "include \"included.td\"")
        val (_, rootB) = createExternalDir("rootB.td", "")

        val update = workspaceUpdater()
        update(mapOf(rootA to IncludePaths(listOf(includeDir))))
        assertContainsElements(ourContentRootUrls(), rootA.url, included.url)

        // 'rootB' includes nothing, so after the switch neither 'rootA' nor 'included' has a context anymore.
        update(mapOf(rootB to IncludePaths(listOf(includeDir))))
        val urls = ourContentRootUrls()
        assertContainsElements(urls, rootB.url)
        assertDoesntContain(urls, rootA.url)
        assertDoesntContain(urls, included.url)
    }

    fun `test class in included file is indexed and in allScope`() {
        val (includeDir, included) = createExternalDir("included.td", "class ExternalClass;")
        val (_, root) = createExternalDir("root.td", "include \"included.td\"")

        val update = workspaceUpdater()
        update(mapOf(root to IncludePaths(listOf(includeDir))))

        // The included file now counts as project content.
        assertTrue(ProjectFileIndex.getInstance(project).isInContent(included))

        // And its class is discoverable through the stub index in the all scope.
        val classes = StubIndex.getElements(
            CLASS_INDEX, "ExternalClass", project, GlobalSearchScope.allScope(project),
            TableGenClassStatement::class.java
        )
        assertSize(1, classes)
    }
}
