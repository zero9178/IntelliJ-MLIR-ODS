package com.github.zero9178.mlirods

import com.github.zero9178.mlirods.model.IncludePaths
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class RenameTest : BasePlatformTestCase() {
    fun `test rename include`() {
        val testFile = myFixture.copyFileToProject("test.td")
        val virtualFile = myFixture.copyFileToProject("HasCompileCommands.td")
        val inputFile = myFixture.copyFileToProject("IncludeReference.td")
        installCompileCommands(
            project,
            mapOf(
                virtualFile to IncludePaths(listOf(testFile.parent))
            ),
            listOf(
                inputFile to IncludePaths(listOf(testFile.parent))
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