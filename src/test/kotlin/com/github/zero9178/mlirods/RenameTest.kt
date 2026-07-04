package com.github.zero9178.mlirods

import com.github.zero9178.mlirods.model.IncludePaths
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class RenameTest : BasePlatformTestCase() {
    fun `test rename macro from ifdef`() {
        // Renaming from an '#ifdef' renames the '#define' declaration and updates its identifier via the manipulator.
        doTestInline(
            "BAR",
            """
            #define FOO
            #ifdef <caret>FOO
            #endif
        """.trimIndent(),
            """
            #define BAR
            #ifdef BAR
            #endif
        """.trimIndent()
        )
    }

    fun `test rename macro from define`() {
        // Renaming the '#define' declaration updates the '#ifdef' reference via the manipulator.
        doTestInline(
            "BAR",
            """
            #define <caret>FOO
            #ifndef FOO
            #endif
        """.trimIndent(),
            """
            #define BAR
            #ifndef BAR
            #endif
        """.trimIndent()
        )
    }

    fun `test rename include`() {
        val testFile = myFixture.copyFileToProject("test.td")
        val virtualFile = myFixture.copyFileToProject("HasCompileCommands.td")
        val inputFile = myFixture.copyFileToProject("IncludeReference.td")
        installCompileCommands(
            project,
            mapOf(
                virtualFile to IncludePaths(listOf(testFile.parent))
            )
        )

        myFixture.configureFromExistingVirtualFile(inputFile)
        myFixture.renameElementAtCaret("tests.td")
        myFixture.checkResultByFile("IncludeReference.td", "IncludeReferenceAfter.td", false)
    }

    fun `test LocalDefvarResolution`() {
        doTest("j")
    }

    fun `test ClassInstantiationResolution`() {
        doTest("J")
    }

    fun `test ClassTypeResolution`() {
        doTest("J")
    }

    fun `test ParentClassListResolution`() {
        doTest("J")
    }

    fun `test FieldAccessResolution`() {
        doTest("j")
    }

    fun `test ForeachDefvarResolution`() {
        doTest("j")
    }

    fun `test FoldlAccDefvarResolution`() {
        doTest("j")
    }

    fun `test FoldlIteratorDefvarResolution`() {
        doTest("j")
    }

    fun `test rename named arg`() {
        // Renaming a template argument must update the named argument identifier via the manipulator.
        doTestInline(
            "j",
            """
            class F<int <caret>i>;

            def : F<i = 0>;
        """.trimIndent(),
            """
            class F<int j>;

            def : F<j = 0>;
        """.trimIndent()
        )
    }

    private fun doTestInline(newName: String, source: String, expected: String) {
        val mainVF = myFixture.createFile("test.td", source)
        installCompileCommands(
            project, mapOf(
                mainVF to IncludePaths(emptyList())
            )
        )

        myFixture.configureFromExistingVirtualFile(mainVF)
        myFixture.renameElementAtCaret(newName)
        myFixture.checkResult(expected)
    }

    private fun doTest(newName: String) {
        val name = getTestName(false).trim()

        val mainVF = myFixture.copyFileToProject("${name}.td")
        myFixture.configureFromExistingVirtualFile(mainVF)
        myFixture.renameElementAtCaret(newName)
        myFixture.checkResultByFile("${name}.td", "${name}After.td", false)
    }

    override fun getTestDataPath(): String? {
        return "src/test/testData/references"
    }
}