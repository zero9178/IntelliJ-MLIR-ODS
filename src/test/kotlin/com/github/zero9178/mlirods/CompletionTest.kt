package com.github.zero9178.mlirods

import com.github.zero9178.mlirods.model.IncludePaths
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.application.writeAction
import com.intellij.openapi.progress.runBlockingCancellable
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.DumbModeTestUtils
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.testFramework.utils.vfs.createDirectory
import com.intellij.testFramework.utils.vfs.createFile
import kotlinx.coroutines.runBlocking

class CompletionTest : BasePlatformTestCase() {
    fun `test foldl completion`() = doTest(
        """
            defvar values = [1];
            defvar test = !foldl(0, <caret>, acc, i, i);
        """.trimIndent(), "values"
        )


    fun `test foreach completion`() = doTest(
        """
            defvar values = [1];
            defvar test = !foreach(i, <caret>, i);
        """.trimIndent(), "values"
    )


    fun `test dumb field lookup`() = doDumbTest(
        """
            defvar v = 0;
            
            class A : B {
                int i = <caret>;
            }
        """.trimIndent(), "v"
    )


    fun `test field access lookup`() = doTest(
        """
            defvar v = 0;
            
            class B {
                int j = 0;
            }
            
            defvar l = B<>.<caret>
        """.trimIndent(),
            "j"
        )

    fun `test field cross file access lookup`() {
        val otherTD = myFixture.configureByText(
            "other.td", """   
            class B {
                int j = 0;
            }
            """.trimIndent()
        )

        val testTD = myFixture.configureByText(
            "test.td", """
            include "other.td"
                
            defvar l = B<>.<caret>
        """.trimIndent()
        )
        installCompileCommands(
            project, mapOf(
                testTD.virtualFile to IncludePaths(listOf(otherTD.virtualFile.parent))
            )
        )

        myFixture.completeBasic()
        assertSameElements(
            requireNotNull(myFixture.lookupElementStrings),
            "j"
        )
    }

    fun `test include directory completion`() {
        val testTD = myFixture.createFile(
            "test.td", """
            include "<caret>"
        """.trimIndent()
        )
        var directory = testTD.parent
        directory = WriteAction.computeAndWait<VirtualFile, Throwable> {
            directory.createDirectory("subdir").apply {
                createDirectory("to-complete")
            }
        }

        installCompileCommands(
            project, mapOf(
                testTD to IncludePaths(listOf(directory))
            )
        )
        myFixture.configureFromExistingVirtualFile(testTD)

        myFixture.completeBasic()
        assertSameElements(
            requireNotNull(myFixture.lookupElementStrings),
            "to-complete"
        )
        myFixture.type('\t')

        myFixture.checkResult(
            """
            include "to-complete/<caret>"
        """.trimIndent()
        )
    }

    fun `test include file completion`() {
        val testTD = myFixture.createFile(
            "test.td", """
            include "<caret>"
        """.trimIndent()
        )
        var directory = testTD.parent
        directory = WriteAction.computeAndWait<VirtualFile, Throwable> {
            directory.createDirectory("subdir").apply {
                createFile("to-complete.td")
            }
        }

        installCompileCommands(
            project, mapOf(
                testTD to IncludePaths(listOf(directory))
            )
        )
        myFixture.configureFromExistingVirtualFile(testTD)

        myFixture.completeBasic()
        assertSameElements(
            requireNotNull(myFixture.lookupElementStrings),
            "to-complete.td"
        )
        myFixture.type('\t')

        myFixture.checkResult(
            """
            include "to-complete.td<caret>"
        """.trimIndent()
        )
    }

    fun `test dumb class completion`() {
        doDumbTest(
            """
            class A;
            
            class B<<caret>
        """.trimIndent(), "A"
        )

        doDumbTest(
            """
            class A;
            
            class B {
                <caret>
            }
        """.trimIndent(), "A"
        )


        doDumbTest(
            """
            class A;
            
            class B : <caret>
        """.trimIndent(), "A"
        )
    }


    private fun doTest(source: String, vararg expected: String) {
        myFixture.configureByText(
            "test.td", source
        )


        myFixture.completeBasic()
        assertSameElements(requireNotNull(myFixture.lookupElementStrings), *expected)
    }

    private fun doDumbTest(source: String, vararg expected: String) =
        DumbModeTestUtils.runInDumbModeSynchronously(project) {
            doTest(source, *expected)
        }
}