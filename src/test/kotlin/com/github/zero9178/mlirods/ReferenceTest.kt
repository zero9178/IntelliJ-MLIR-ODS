package com.github.zero9178.mlirods

import com.github.zero9178.mlirods.language.generated.psi.TableGenClassStatement
import com.github.zero9178.mlirods.language.generated.psi.TableGenDefvarBodyItem
import com.github.zero9178.mlirods.language.generated.psi.TableGenDefvarStatement
import com.github.zero9178.mlirods.language.generated.psi.TableGenTemplateArgDecl
import com.github.zero9178.mlirods.model.IncludePaths
import com.intellij.psi.PsiFile
import com.intellij.psi.util.parentOfType
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class ReferenceTest : BasePlatformTestCase() {
    fun `test IncludeReference`() {
        val reference =
            myFixture.getReferenceAtCaretPositionWithAssertion("IncludeReference.td")
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

    fun `test GlobalDefResolution`() {
        val element = doTest<TableGenDefvarStatement>("test.td")
        assertEquals(element.name, "f")
        assertEquals(element.containingFile.name, "test.td")
    }

    fun `test LocalDefvarResolution`() {
        val element = doTest<TableGenDefvarBodyItem>()
        assertEquals(element.name, "i")
        assertNotNull(element.parentOfType<TableGenClassStatement>())
    }

    fun `test ClassArgResolution`() {
        val element = doTest<TableGenTemplateArgDecl>()
        assertEquals(element.name, "i")
        assertNotNull(element.parentOfType<TableGenClassStatement>())
    }

    override fun getTestDataPath(): String? {
        return "src/test/testData/references"
    }

    private inline fun <reified T> doTest(vararg additionalFiles: String): T {
        val name = getTestName(false).trim()

        val reference =
            myFixture.getReferenceAtCaretPositionWithAssertion("${name}.td")
        val list = additionalFiles.map { myFixture.copyFileToProject(it).parent }.toList()
        installCompileCommands(
            project,
            mapOf(
                reference.element.containingFile.viewProvider.virtualFile to IncludePaths(list)
            )
        )
        return assertInstanceOf(reference.resolve(), T::class.java)
    }

}