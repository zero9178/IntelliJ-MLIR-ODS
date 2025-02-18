package com.github.zero9178.mlirods

import com.github.zero9178.mlirods.model.IncludePaths
import com.intellij.psi.PsiFile
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class ReferenceTest : BasePlatformTestCase() {
    fun `test include`() {
        val reference =
            myFixture.getReferenceAtCaretPositionWithAssertion("ReferenceTestData.td")
        val testFile = myFixture.copyFileToProject("test.td")
        val virtualFile = myFixture.copyFileToProject("HasCompileCommands.td")
        installCompileCommands(
            project,
            mapOf(
                virtualFile to IncludePaths(listOf(testFile.parent))
            )
        )

        val element = assertInstanceOf(reference.resolve(), PsiFile::class.java)
        assertEquals(element.viewProvider.virtualFile.name, "test.td")
    }

    override fun getTestDataPath(): String? {
        return "src/test/testData/references"
    }
}