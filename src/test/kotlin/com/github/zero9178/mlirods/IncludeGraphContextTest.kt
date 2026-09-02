package com.github.zero9178.mlirods

import com.github.zero9178.mlirods.language.TableGenFileType
import com.github.zero9178.mlirods.language.generated.psi.TableGenIncludeDirective
import com.github.zero9178.mlirods.language.psi.TableGenFile
import com.github.zero9178.mlirods.model.IncludePaths
import com.github.zero9178.mlirods.model.TableGenIncludeGraphService
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiManager
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.VfsTestUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import kotlin.io.path.createDirectory
import kotlin.io.path.writeText

/**
 * Tests for [TableGenIncludeGraphService] context propagation: how include paths from the compile commands flow into the
 * root file and transitively into all (directly and indirectly) included files, and how the set of files visible to a
 * file ([TableGenIncludeGraphService.getIncludedFiles]) is populated as a result.
 */
class IncludeGraphContextTest : BasePlatformTestCase() {

    private val service: TableGenIncludeGraphService
        get() = project.service()

    private fun includePaths(vf: VirtualFile): List<VirtualFile> =
        service.getContextOf(vf)?.includePaths ?: emptyList()

    private fun includedFiles(vf: VirtualFile): Set<VirtualFile> =
        service.getIncludedFiles(PsiManager.getInstance(project).findFile(vf) as TableGenFile)

    fun `test compile command propagates include paths to root`() {
        val root = myFixture.createFile("root.td", "")
        val dir = root.parent
        installCompileCommands(project, mapOf(root to IncludePaths(listOf(dir))))

        assertEquals(listOf(dir), includePaths(root))
    }

    fun `test direct include inherits include paths and sees its includer`() {
        val included = myFixture.createFile("included.td", "")
        val root = myFixture.createFile(
            "root.td", """
            include "included.td"
        """.trimIndent()
        )
        val dir = root.parent
        installCompileCommands(project, mapOf(root to IncludePaths(listOf(dir))))

        // Include paths propagate unchanged from the root.
        assertEquals(listOf(dir), includePaths(included))
        // The file it is included from is visible to it.
        assertContainsElements(includedFiles(included), root)
    }

    fun `test include paths propagate down the include chain`() {
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

        // Include paths propagate down the entire chain, unchanged.
        assertEquals(listOf(dir), includePaths(mid))
        assertEquals(listOf(dir), includePaths(leaf))
    }

    fun `test earlier includes are visible to later ones but not the reverse`() {
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

        // 'second' is preceded by 'first' in the root, so 'first' is visible to it, but not vice versa.
        assertContainsElements(includedFiles(second), first)
        assertDoesntContain(includedFiles(first), second)
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

    /**
     * Replaces the contents of [file] and waits for the include graph to have caught up with the change.
     */
    private fun writeToFile(file: VirtualFile, content: String) {
        WriteCommandAction.runWriteCommandAction(project) {
            val document = FileDocumentManager.getInstance().getDocument(file)!!
            document.setText(content)
            PsiDocumentManager.getInstance(project).commitDocument(document)
        }
        awaitIncludeGraph(project)
    }

    fun `test removing the winning path keeps a grandchild reachable via a surviving path`() {
        // Graph shape (all files in the same directory, 'root' is the compile-command root):
        //
        //   root --0--> a --0--> t --0--> x
        //   root --1---------------^
        //
        // 't' is reachable both through 'a' (the earlier, and therefore winning, DFS path) and directly from 'root'
        // (a later, larger path). 'x' is only reachable through 't'. The direct root->t edge is added *after* the
        // root->a->t->x chain has fully settled, so at the moment it is added it is not the smallest path into 't' and
        // is therefore never propagated down to 'x'.
        val x = myFixture.createFile("x.td", "class X;")
        val t = myFixture.createFile("t.td", "include \"x.td\"")
        val a = myFixture.createFile("a.td", "include \"t.td\"")
        val root = myFixture.createFile(
            "root.td", """
            include "a.td"
        """.trimIndent()
        )
        val dir = root.parent
        installCompileCommands(project, mapOf(root to IncludePaths(listOf(dir))))

        // Sanity: the whole chain has a context through the root -> a -> t -> x path.
        assertNotNull(service.getContextOf(t))
        assertNotNull(service.getContextOf(x))

        // Add the second, direct root -> t edge. It is a larger DFS path than root -> a -> t, so it becomes a
        // non-winning parent edge of 't' that is never pushed down into 'x'.
        writeToFile(
            root, """
            include "a.td"
            include "t.td"
        """.trimIndent()
        )

        assertNotNull(service.getContextOf(t))
        assertNotNull(service.getContextOf(x))

        // Now remove the winning root -> a -> t path by dropping a's include of 't'. 't' is still reachable via the
        // surviving direct root -> t edge, so both 't' and 'x' must keep their context.
        writeToFile(a, "")

        // 't' survives because it still has the direct edge from the root.
        assertNotNull("'t' is still directly included from the root and must keep its context", service.getContextOf(t))
        // 'x' is still reachable via root -> t -> x, so it must keep its context too. With the buggy propagation the
        // surviving root -> t path was never propagated into 'x', so removing the root -> a -> t path wrongly strips
        // 'x' of its only recorded numbering and it loses its context.
        assertNotNull(
            "'x' is still reachable via root -> t -> x and must keep its context", service.getContextOf(x)
        )
    }

    fun `test removing an include added after the first one drops its context`() {
        // 'later' is included from the root as its *second* include, and only added once the root -> first edge has
        // already been recorded. The path recorded for 'later' must use its actual position among the root's includes,
        // otherwise dropping the include again does not match – and thus does not remove – the recorded path.
        val first = myFixture.createFile("first.td", "class First;")
        val later = myFixture.createFile("later.td", "class Later;")
        val root = myFixture.createFile(
            "root.td", """
            include "first.td"
        """.trimIndent()
        )
        val dir = root.parent
        installCompileCommands(project, mapOf(root to IncludePaths(listOf(dir))))

        assertNull(service.getContextOf(later))

        writeToFile(
            root, """
            include "first.td"
            include "later.td"
        """.trimIndent()
        )
        assertNotNull("'later' is included from the root and must have a context", service.getContextOf(later))

        // Drop the include again. 'later' is not reachable from any compile command anymore.
        writeToFile(
            root, """
            include "first.td"
        """.trimIndent()
        )
        assertNotNull("'first' is still included from the root", service.getContextOf(first))
        assertNull("'later' is not included from anywhere anymore", service.getContextOf(later))
    }

    fun `test removing an include whose position shifted drops its context`() {
        // 'shifted' is the root's only include to begin with and is pushed to the second position by 'first' being
        // added in front of it. The paths recorded for it and for the files it includes embed the position of the
        // include, so they have to follow that shift. Otherwise dropping the include afterwards does not match – and
        // thus does not remove – the recorded path, leaving both files with a context they no longer have.
        val deep = myFixture.createFile("deep.td", "class Deep;")
        val shifted = myFixture.createFile("shifted.td", "include \"deep.td\"")
        val first = myFixture.createFile("first.td", "class First;")
        val root = myFixture.createFile(
            "root.td", """
            include "shifted.td"
        """.trimIndent()
        )
        val dir = root.parent
        installCompileCommands(project, mapOf(root to IncludePaths(listOf(dir))))

        assertNotNull(service.getContextOf(shifted))
        assertNotNull(service.getContextOf(deep))

        // Push 'shifted' from the first to the second include of the root.
        writeToFile(
            root, """
            include "first.td"
            include "shifted.td"
        """.trimIndent()
        )
        assertNotNull("'shifted' is still included from the root", service.getContextOf(shifted))
        assertNotNull("'deep' is still included from 'shifted'", service.getContextOf(deep))

        // Drop the include again. Neither file is reachable from any compile command anymore.
        writeToFile(
            root, """
            include "first.td"
        """.trimIndent()
        )
        assertNotNull("'first' is still included from the root", service.getContextOf(first))
        assertNull("'shifted' is not included from anywhere anymore", service.getContextOf(shifted))
        assertNull("'deep' is only included from 'shifted' and must lose its context too", service.getContextOf(deep))
    }

    fun `test a graph with exponentially many include paths settles`() {
        // 'f0' includes 'f1' and 'f2', 'f1' includes 'f2' and 'f3' and so on, making the number of distinct paths from
        // 'f0' to 'fN' grow exponentially with N while the graph itself stays linear in size. Anything that reasons
        // about the paths through the graph rather than about the graph itself does not finish this test.
        val count = 40
        val files = (0 until count).map { index ->
            myFixture.createFile(
                "f$index.td", (1..2).mapNotNull { (index + it).takeIf { next -> next < count } }
                    .joinToString("\n") { "include \"f$it.td\"" })
        }
        val dir = files.first().parent
        installCompileCommands(project, mapOf(files.first() to IncludePaths(listOf(dir))))

        // Every file is reachable from the root and therefore has a context.
        files.forEach { assertEquals(listOf(dir), includePaths(it)) }
        // The last file is reachable from every other one, so all of them are visible to it.
        assertContainsElements(includedFiles(files.last()), files)
    }

    fun `test the graph can be traversed backwards`() {
        val leaf = myFixture.createFile("leaf.td", "")
        val second = myFixture.createFile("second.td", "include \"leaf.td\"")
        val first = myFixture.createFile("first.td", "include \"leaf.td\"")
        val root = myFixture.createFile(
            "root.td", """
            include "second.td"
            include "first.td"
        """.trimIndent()
        )
        val dir = root.parent
        installCompileCommands(project, mapOf(root to IncludePaths(listOf(dir))))

        // Both files including 'leaf' are reported, in a deterministic order that does not depend on the order the
        // includes were discovered in.
        assertEquals(listOf(first, second), service.getIncludersOf(leaf))
        assertEquals(listOf(root), service.getIncludersOf(first))
        assertEmpty(service.getIncludersOf(root))
    }

    fun `test the graph can be traversed backwards transitively`() {
        val leaf = myFixture.createFile("leaf.td", "")
        val mid = myFixture.createFile("mid.td", "include \"leaf.td\"")
        val sibling = myFixture.createFile("sibling.td", "")
        val root = myFixture.createFile(
            "root.td", """
            include "mid.td"
            include "sibling.td"
        """.trimIndent()
        )
        val dir = root.parent
        installCompileCommands(project, mapOf(root to IncludePaths(listOf(dir))))

        // Every file 'leaf' is pasted into, no matter how indirectly. 'sibling' is none of them: it neither includes
        // 'leaf' nor is it included by anything that does.
        assertEquals(setOf(mid, root), service.getTransitiveIncludersOf(leaf))
        assertEquals(setOf(root), service.getTransitiveIncludersOf(mid))
        assertEmpty(service.getTransitiveIncludersOf(root))

        // Deleting 'mid' is what took 'leaf' out of the graph, so nothing includes it anymore. Answering from the
        // closure computed above would keep reporting the include chain that no longer exists.
        WriteCommandAction.runWriteCommandAction(project) {
            mid.delete(this)
        }
        awaitIncludeGraph(project)

        assertEmpty(service.getTransitiveIncludersOf(leaf))
    }

    fun `test a file outside of any include chain has no includers`() {
        val root = myFixture.createFile("root.td", "")
        val orphan = myFixture.createFile("orphan.td", "")
        installCompileCommands(project, mapOf(root to IncludePaths(listOf(root.parent))))

        // A file that is not part of the graph at all is not an error, it simply has nothing including it.
        assertEmpty(service.getTransitiveIncludersOf(orphan))
    }

    fun `test creating a file resolves an include that resolved to nothing`() {
        val root = myFixture.createFile("root.td", "include \"appears.td\"")
        val dir = root.parent
        installCompileCommands(project, mapOf(root to IncludePaths(listOf(dir))))

        // The file the root includes does not exist yet, so the root sees nothing but itself.
        assertEquals(setOf(root), includedFiles(root))

        val appears = myFixture.createFile("appears.td", "class Appears;")
        awaitIncludeGraph(project)

        assertEquals(listOf(dir), includePaths(appears))
        assertContainsElements(includedFiles(root), appears)
    }

    fun `test renaming a file to the name an include names resolves it`() {
        val root = myFixture.createFile("root.td", "include \"target.td\"")
        val other = myFixture.createFile("other.td", "class Other;")
        val dir = root.parent
        installCompileCommands(project, mapOf(root to IncludePaths(listOf(dir))))

        assertNull("'other' is not included from anywhere under its current name", service.getContextOf(other))

        WriteCommandAction.runWriteCommandAction(project) {
            other.rename(this, "target.td")
        }
        awaitIncludeGraph(project)

        assertEquals(listOf(dir), includePaths(other))
        assertContainsElements(includedFiles(root), other)
    }

    fun `test moving a file into the directory an include names resolves it`() {
        val root = myFixture.createFile("root.td", "include \"sub/moved.td\"")
        val moved = myFixture.createFile("moved.td", "class Moved;")
        val dir = root.parent
        val sub = myFixture.tempDirFixture.findOrCreateDir("sub")
        installCompileCommands(project, mapOf(root to IncludePaths(listOf(dir))))

        assertNull("'moved' is not below 'sub' yet", service.getContextOf(moved))

        WriteCommandAction.runWriteCommandAction(project) {
            moved.move(this, sub)
        }
        awaitIncludeGraph(project)

        assertEquals(listOf(dir), includePaths(moved))
        assertContainsElements(includedFiles(root), moved)
    }

    fun `test deleting a file drops the context of files only reachable through it`() {
        val leaf = myFixture.createFile("leaf.td", "class Leaf;")
        val root = myFixture.createFile("root.td", "include \"leaf.td\"")
        val dir = root.parent
        installCompileCommands(project, mapOf(root to IncludePaths(listOf(dir))))

        assertNotNull(service.getContextOf(leaf))
        assertContainsElements(service.getFilesWithContext(), leaf)

        // Deleting the root leaves 'leaf' unreachable from any compile command.
        WriteCommandAction.runWriteCommandAction(project) {
            root.delete(this)
        }
        awaitIncludeGraph(project)

        assertNull("'leaf' is only reachable through the deleted 'root'", service.getContextOf(leaf))
        assertDoesntContain(service.getFilesWithContext(), leaf)
    }

    fun `test renaming a directory to the name an include names resolves it`() {
        // Renaming a directory is a single event about the directory; the files below it keep their names and are
        // therefore never events of their own, even though every one of them just changed path.
        val root = myFixture.createFile("root.td", "include \"target/leaf.td\"")
        val dir = root.parent
        val leaf = VfsTestUtil.createFile(dir, "other/leaf.td", "class Leaf;")
        installCompileCommands(project, mapOf(root to IncludePaths(listOf(dir))))

        assertNull("'leaf' is not below 'target' yet", service.getContextOf(leaf))

        WriteCommandAction.runWriteCommandAction(project) {
            leaf.parent.rename(this, "target")
        }
        awaitIncludeGraph(project)

        assertEquals(listOf(dir), includePaths(leaf))
        assertContainsElements(includedFiles(root), leaf)
    }

    fun `test moving a directory into an include path resolves an include naming it`() {
        // Moving a directory is a single event about the directory, just like renaming one is.
        val root = myFixture.createFile("root.td", "include \"sub/leaf.td\"")
        val dir = root.parent
        val leaf = VfsTestUtil.createFile(dir, "nested/sub/leaf.td", "class Leaf;")
        installCompileCommands(project, mapOf(root to IncludePaths(listOf(dir))))

        assertNull("'sub' is not below the include path yet", service.getContextOf(leaf))

        WriteCommandAction.runWriteCommandAction(project) {
            leaf.parent.move(this, dir)
        }
        awaitIncludeGraph(project)

        assertEquals(listOf(dir), includePaths(leaf))
        assertContainsElements(includedFiles(root), leaf)
    }

    fun `test deleting a directory unresolves the includes naming files below it`() {
        // Deleting a directory is a single event about the directory; the files below it disappear along with it
        // without events of their own.
        val root = myFixture.createFile("root.td", "include \"sub/leaf.td\"")
        val dir = root.parent
        val leaf = VfsTestUtil.createFile(dir, "sub/leaf.td", "class Leaf;")
        installCompileCommands(project, mapOf(root to IncludePaths(listOf(dir))))

        assertContainsElements(includedFiles(root), leaf)

        WriteCommandAction.runWriteCommandAction(project) {
            leaf.parent.delete(this)
        }
        awaitIncludeGraph(project)

        assertEquals("the include of the root does not resolve to anything anymore", setOf(root), includedFiles(root))
        assertEquals(setOf(root), service.getFilesWithContext())
    }

    /**
     * Returns the file the single 'include' directive of [file] resolves to according to its psi.
     *
     * The psi resolves an include on its own rather than reading the answer off the graph, so it has to agree with the
     * graph about what a directive resolves to for e.g. navigation and the graph's contexts to describe the same thing.
     */
    private fun psiIncludeTargetOf(file: VirtualFile): VirtualFile? {
        val psiFile = PsiManager.getInstance(project).findFile(file) as TableGenFile
        return PsiTreeUtil.findChildOfType(psiFile, TableGenIncludeDirective::class.java)!!.includedFile
    }

    fun `test an include naming a directory resolves to nothing`() {
        // A directory is not TableGen source and cannot be pasted into a file. Letting one into the graph has it try to
        // read the include directives of a directory, which is not something a directory can be asked for at all.
        val root = myFixture.createFile("root.td", "include \"sub\"")
        val dir = root.parent
        VfsTestUtil.createFile(dir, "sub/leaf.td", "class Leaf;")
        installCompileCommands(project, mapOf(root to IncludePaths(listOf(dir))))

        assertEquals(setOf(root), includedFiles(root))
        assertEquals(setOf(root), service.getFilesWithContext())
        assertNull(psiIncludeTargetOf(root))
    }

    fun `test an empty include does not resolve to the include path`() {
        // The empty suffix names the include path itself, i.e. a directory, and does so without any file existing.
        val root = myFixture.createFile("root.td", "include \"\"")
        val dir = root.parent
        installCompileCommands(project, mapOf(root to IncludePaths(listOf(dir))))

        assertEquals(setOf(root), includedFiles(root))
        assertEquals(setOf(root), service.getFilesWithContext())
        assertNull(psiIncludeTargetOf(root))
    }

    fun `test an include naming a file of another type resolves to nothing`() {
        // An 'include' may name any path, but only a TableGen file is TableGen source. Resolving to anything else has
        // the file lexed as TableGen and whatever that yields followed as further includes.
        val root = myFixture.createFile("root.td", "include \"header.h\"")
        val header = myFixture.createFile("header.h", "#include <vector>")
        val dir = root.parent
        installCompileCommands(project, mapOf(root to IncludePaths(listOf(dir))))

        assertEquals(setOf(root), includedFiles(root))
        assertNull("a C header is not part of the include graph", service.getContextOf(header))
        assertNull(psiIncludeTargetOf(root))
    }

    fun `test a directory in an earlier include path does not shadow the file in a later one`() {
        // The first include path holds a directory named exactly like the file the include names. Include paths are
        // searched in order for the *file*, so a path holding something that is not one has to be skipped rather than
        // end the search.
        val first = myFixture.tempDirFixture.findOrCreateDir("first")
        val second = myFixture.tempDirFixture.findOrCreateDir("second")
        VfsTestUtil.createDir(first, "leaf.td")
        val leaf = VfsTestUtil.createFile(second, "leaf.td", "class Leaf;")
        val root = myFixture.createFile("root.td", "include \"leaf.td\"")
        installCompileCommands(project, mapOf(root to IncludePaths(listOf(first, second))))

        assertContainsElements(includedFiles(root), leaf)
        assertEquals(leaf, psiIncludeTargetOf(root))
    }

    /**
     * Associates [pattern] with the TableGen file type for the duration of the test, exactly as adding it under
     * Settings | Editor | File Types does.
     */
    private fun associateWithTableGen(pattern: String) {
        val fileTypeManager = FileTypeManager.getInstance()
        val matcher = FileTypeManager.parseFromString(pattern)
        runWriteAction { fileTypeManager.associate(TableGenFileType.INSTANCE, matcher) }
        Disposer.register(testRootDisposable) {
            runWriteAction { fileTypeManager.removeAssociation(TableGenFileType.INSTANCE, matcher) }
        }
    }

    fun `test a file matching a pattern associated with the TableGen file type is TableGen source`() {
        // Which files are TableGen source is up to the file type the IDE associates with them rather than up to the
        // '.td' extension, so a file appearing under a pattern a user added to the TableGen file type is one appearing
        // in the graph just as much as a '.td' file is.
        associateWithTableGen("*.tablegen")

        val root = myFixture.createFile("root.td", "include \"appears.tablegen\"")
        val dir = root.parent
        installCompileCommands(project, mapOf(root to IncludePaths(listOf(dir))))

        assertEquals("the file the root includes does not exist yet", setOf(root), includedFiles(root))

        val appears = myFixture.createFile("appears.tablegen", "class Appears;")
        awaitIncludeGraph(project)

        assertEquals(listOf(dir), includePaths(appears))
        assertContainsElements(includedFiles(root), appears)
        assertEquals(appears, psiIncludeTargetOf(root))
    }

    /**
     * Creates an empty directory on the local file system that is deleted again once the test finishes.
     *
     * The in-memory file system the fixture uses can express neither a directory being discovered with files already in
     * it nor a directory being copied, both of which the tests using this are about.
     */
    private fun createLocalDirectory(): VirtualFile {
        val path = "${FileUtil.getTempDirectory()}/${getTestName(false)}"
        val directory = runWriteAction {
            // A previous run that did not get to clean up after itself must not leak into this one.
            LocalFileSystem.getInstance().refreshAndFindFileByPath(path)?.delete(this)
            VfsUtil.createDirectoryIfMissing(path)!!
        }
        Disposer.register(testRootDisposable) { runWriteAction { directory.delete(this) } }
        return directory
    }

    fun `test a directory appearing with a file in it resolves an include naming it`() {
        // A directory a refresh discovers is reported as a single creation event carrying its entire content; the files
        // that appear along with it are never events of their own.
        val dir = createLocalDirectory()
        val root = VfsTestUtil.createFile(dir, "root.td", "include \"sub/leaf.td\"")
        installCompileCommands(project, mapOf(root to IncludePaths(listOf(dir))))

        assertEquals("the file the root includes does not exist yet", setOf(root), service.getFilesWithContext())

        // Deliberately created behind the back of the virtual file system: the event this is about is precisely the one
        // reporting a directory that appeared without the virtual file system having created it, e.g. by a branch being
        // checked out.
        dir.toNioPath().resolve("sub").createDirectory().resolve("leaf.td").writeText("class Leaf;")
        VfsUtil.markDirtyAndRefresh(false, true, true, dir)
        awaitIncludeGraph(project)

        val leaf = dir.findFileByRelativePath("sub/leaf.td")!!
        assertEquals(listOf(dir), includePaths(leaf))
    }

    fun `test copying a directory resolves an include naming the copy`() {
        // Copying a directory copies everything below it, and is a single event about the directory.
        val dir = createLocalDirectory()
        val root = VfsTestUtil.createFile(dir, "root.td", "include \"target/leaf.td\"")
        val source = VfsTestUtil.createFile(dir, "source/leaf.td", "class Leaf;").parent
        installCompileCommands(project, mapOf(root to IncludePaths(listOf(dir))))

        assertEquals("'target' does not exist yet", setOf(root), service.getFilesWithContext())

        WriteCommandAction.runWriteCommandAction(project) {
            source.copy(this, dir, "target")
        }
        awaitIncludeGraph(project)

        val leaf = dir.findFileByRelativePath("target/leaf.td")!!
        assertEquals(listOf(dir), includePaths(leaf))
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
        assertEquals(listOf(dir), includePaths(rootA))
        // 'shared' is reachable from 'rootA'.
        assertContainsElements(includedFiles(shared), rootA)

        // Replace the compile commands so only 'rootB' is a root anymore.
        update(mapOf(rootB to IncludePaths(listOf(dir))))
        // 'rootB' is now a root with the include paths.
        assertEquals(listOf(dir), includePaths(rootB))
        // 'rootA' is no longer a root and thus has no context anymore.
        assertNull(service.getContextOf(rootA))
        // 'shared' is still reachable from the now-active root.
        assertContainsElements(includedFiles(rootB), shared)
    }
}
