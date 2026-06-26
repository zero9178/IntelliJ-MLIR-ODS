package com.github.zero9178.mlirods

import com.github.zero9178.mlirods.language.psi.TableGenFile
import com.github.zero9178.mlirods.model.IncludePaths
import com.github.zero9178.mlirods.model.TableGenContext
import com.github.zero9178.mlirods.model.TableGenContextService
import com.intellij.openapi.components.service
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.testFramework.fixtures.BasePlatformTestCase

/**
 * Tests for [TableGenContextService] context propagation: how include paths from the compile commands flow into the
 * root file and transitively into all (directly and indirectly) included files, and how [TableGenContext.includedFrom],
 * [TableGenContext.includedBeforeThis] and [TableGenContextService.getIncludedFiles] are populated as a result.
 */
class ContextTest : BasePlatformTestCase() {

    private fun tableGenFile(vf: VirtualFile): TableGenFile =
        PsiManager.getInstance(project).findFile(vf) as TableGenFile

    private fun contextOf(vf: VirtualFile): TableGenContext = tableGenFile(vf).context

    private fun includedFiles(vf: VirtualFile): Set<VirtualFile> =
        project.service<TableGenContextService>().getIncludedFiles(tableGenFile(vf))

    fun `test compile command propagates include paths to root`() {
        val root = myFixture.createFile("root.td", "")
        val dir = root.parent
        installCompileCommands(project, mapOf(root to IncludePaths(listOf(dir))))

        val context = contextOf(root)
        assertEquals(listOf(dir), context.includePaths)
        // A root is not included from anywhere and nothing is included before it.
        assertEmpty(context.includedFrom)
        assertEmpty(context.includedBeforeThis)
    }

    fun `test direct include inherits includedFrom and include paths`() {
        val included = myFixture.createFile("included.td", "")
        val root = myFixture.createFile(
            "root.td", """
            include "included.td"
        """.trimIndent()
        )
        val dir = root.parent
        installCompileCommands(project, mapOf(root to IncludePaths(listOf(dir))))

        val context = contextOf(included)
        assertEquals(listOf(root), context.includedFrom.toList())
        // Include paths propagate unchanged from the root.
        assertEquals(listOf(dir), context.includePaths)
    }

    fun `test transitive include builds includedFrom chain`() {
        val leaf = myFixture.createFile("leaf.td", "")
        val mid = myFixture.createFile(
            "mid.td", """
            include "leaf.td"
        """.trimIndent()
        )
        val root = myFixture.createFile(
            "root.td", """
            include "mid.td"
        """.trimIndent()
        )
        val dir = root.parent
        installCompileCommands(project, mapOf(root to IncludePaths(listOf(dir))))

        // 'mid' is included directly from the root.
        assertEquals(listOf(root), contextOf(mid).includedFrom.toList())
        // 'leaf' is included from the root via 'mid'; the chain is ordered from outermost to innermost.
        assertEquals(listOf(root, mid), contextOf(leaf).includedFrom.toList())
        // Include paths propagate down the entire chain.
        assertEquals(listOf(dir), contextOf(leaf).includePaths)
    }

    fun `test includedBeforeThis reflects earlier includes`() {
        val first = myFixture.createFile("first.td", "")
        val second = myFixture.createFile("second.td", "")
        val root = myFixture.createFile(
            "root.td", """
            include "first.td"
            include "second.td"
        """.trimIndent()
        )
        val dir = root.parent
        installCompileCommands(project, mapOf(root to IncludePaths(listOf(dir))))

        // 'second' is preceded by 'first' in the root, so 'first' is visible to it.
        assertContainsElements(contextOf(second).includedBeforeThis, first)
        // 'first' has nothing before it.
        assertEmpty(contextOf(first).includedBeforeThis)
    }

    fun `test getIncludedFiles returns transitive includes`() {
        val leaf = myFixture.createFile("leaf.td", "")
        val mid = myFixture.createFile(
            "mid.td", """
            include "leaf.td"
        """.trimIndent()
        )
        val root = myFixture.createFile(
            "root.td", """
            include "mid.td"
        """.trimIndent()
        )
        val dir = root.parent
        installCompileCommands(project, mapOf(root to IncludePaths(listOf(dir))))

        // The root transitively includes both 'mid' and 'leaf'.
        assertContainsElements(includedFiles(root), mid, leaf)
    }

    fun `test getIncludedFiles contains the includedFrom chain`() {
        val leaf = myFixture.createFile("leaf.td", "")
        val mid = myFixture.createFile(
            "mid.td", """
            include "leaf.td"
        """.trimIndent()
        )
        val root = myFixture.createFile(
            "root.td", """
            include "mid.td"
        """.trimIndent()
        )
        val dir = root.parent
        installCompileCommands(project, mapOf(root to IncludePaths(listOf(dir))))

        // Definitions in the files we are included from are visible, so they are part of the included set.
        assertContainsElements(includedFiles(leaf), root, mid)
    }

    fun `test recursive include is handled gracefully`() {
        val a = myFixture.createFile(
            "a.td", """
            include "b.td"
        """.trimIndent()
        )
        val b = myFixture.createFile(
            "b.td", """
            include "a.td"
        """.trimIndent()
        )
        val dir = a.parent
        // Must terminate despite the cycle.
        installCompileCommands(project, mapOf(a to IncludePaths(listOf(dir))))

        // Both files see each other, but the traversal does not loop forever.
        assertContainsElements(includedFiles(a), b)
        assertContainsElements(includedFiles(b), a)
    }

    fun `test updating compile commands switches active root`() {
        val shared = myFixture.createFile("shared.td", "")
        val rootA = myFixture.createFile(
            "rootA.td", """
            include "shared.td"
        """.trimIndent()
        )
        val rootB = myFixture.createFile(
            "rootB.td", """
            include "shared.td"
        """.trimIndent()
        )
        val dir = shared.parent

        val update = compileCommandsUpdater(project)
        update(mapOf(rootA to IncludePaths(listOf(dir))))
        assertEquals(listOf(dir), contextOf(rootA).includePaths)
        assertContainsElements(contextOf(shared).includedFrom, rootA)

        // Replace the compile commands so only 'rootB' is a root anymore.
        update(mapOf(rootB to IncludePaths(listOf(dir))))
        // 'rootB' is now a root with the include paths.
        assertEquals(listOf(dir), contextOf(rootB).includePaths)
        // 'rootA' is no longer a root and its include paths have been cleared.
        assertEmpty(contextOf(rootA).includePaths)
        // 'shared' is still reachable from the now-active root.
        assertContainsElements(includedFiles(rootB), shared)
    }
}
