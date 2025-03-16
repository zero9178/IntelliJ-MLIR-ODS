package com.github.zero9178.mlirods

import com.github.zero9178.mlirods.language.TableGenFile
import com.github.zero9178.mlirods.language.generated.psi.*
import com.github.zero9178.mlirods.model.IncludePaths
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.util.parentOfType
import com.intellij.testFramework.PlatformTestUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class ReferenceTest : BasePlatformTestCase() {
    fun `test IncludeReference`() {
        val testFile = myFixture.copyFileToProject("test.td")
        val virtualFile = myFixture.copyFileToProject("HasCompileCommands.td")
        val targetFile = myFixture.copyFileToProject("IncludeReference.td")
        installCompileCommands(
            project, mapOf(
                virtualFile to IncludePaths(listOf(testFile.parent))
            )
        )

        PlatformTestUtil.waitWhileBusy {
            val file =
                PsiManager.getInstance(project).findFile(targetFile) as? TableGenFile ?: return@waitWhileBusy true
            file.context.includePaths.isEmpty()
        }

        myFixture.configureFromExistingVirtualFile(targetFile)
        val element = assertInstanceOf(myFixture.elementAtCaret, PsiFile::class.java)
        assertEquals(element.viewProvider.virtualFile.name, "test.td")
    }

    fun `test GlobalDefResolution`() {
        val element = doTest<TableGenDefvarStatement>("test.td")
        assertEquals(element.name, "f")
        assertEquals(element.containingFile.name, "test.td")
    }

    fun `test LocalDefvarResolution`() {
        val element = doTest<TableGenDefvarStatement>()
        assertEquals(element.name, "i")
        assertNotNull(element.parentOfType<TableGenClassStatement>())
    }

    fun `test ClassArgResolution`() {
        val element = doTest<TableGenTemplateArgDecl>()
        assertEquals(element.name, "i")
        assertNotNull(element.parentOfType<TableGenClassStatement>())
    }

    fun `test ClassArgInheritanceResolution`() {
        val element = doTest<TableGenTemplateArgDecl>()
        assertEquals(element.name, "i")
        assertNotNull(element.parentOfType<TableGenClassStatement>())
    }

    fun `test LocalDefvarStatementResolution`() {
        val element = doTest<TableGenDefvarStatement>()
        assertEquals(element.name, "i")
        assertNotNull(element.parentOfType<TableGenIfBody>())
    }

    fun `test DefVarShadowingResolution`() {
        val element = doTest<TableGenDefvarStatement>()
        assertEquals(element.name, "i")
        assertNotNull(element.parentOfType<TableGenIfBody>())
        assertNull(element.parentOfType<TableGenIfBody>()?.parentOfType<TableGenIfBody>())
    }

    fun `test DefInIncludeResolution`() {
        val element = doTest<TableGenDefvarStatement>("test2.td", "test.td")
        assertEquals(element.name, "f")
        assertEquals(element.containingFile.name, "test2.td")
    }

    fun `test ParentClassListResolution`() {
        val element = doTest<TableGenClassStatement>("ParentClassListResolution.td")
        assertEquals(element.name, "F")
    }

    fun `test ClassInstantiationResolution`() {
        val element = doTest<TableGenClassStatement>("ParentClassListResolution.td")
        assertEquals(element.name, "F")
    }

    fun `test ClassTypeResolution`() {
        val element = doTest<TableGenClassStatement>("ParentClassListResolution.td")
        assertEquals(element.name, "F")
    }

    fun `test GlobalClassInstantiationResolution`() {
        val element = doTest<TableGenClassStatement>("GlobalClassInstantiationResolution.td", "test.td")
        assertEquals(element.name, "F")
        assertEquals(element.containingFile.name, "test.td")
    }

    fun `test FieldDefResolution`() {
        val element = doTest<TableGenFieldBodyItem>()
        assertEquals(element.name, "i")
    }

    fun `test ParentClassFieldDefResolution`() {
        val element = doTest<TableGenFieldBodyItem>()
        assertEquals(element.name, "i")
        val parentClass = assertInstanceOf(element.parent, TableGenClassStatement::class.java)
        assertEquals(parentClass.name, "A")
    }

    fun `test GlobalFieldDefResolution`() {
        val element = doTest<TableGenFieldBodyItem>("test.td")
        assertEquals(element.name, "i")
        val parentClass = assertInstanceOf(element.parent, TableGenClassStatement::class.java)
        assertEquals(parentClass.name, "A")
        assertEquals(parentClass.containingFile.name, "test.td")
    }

    fun `test ClassTemplateArgFieldResolutionPos`() {
        val element = doTest<TableGenTemplateArgDecl>()
        assertEquals(element.name, "i")
        val parentClass = assertInstanceOf(element.parent, TableGenClassStatement::class.java)
        assertEquals(parentClass.name, "Foobar")
    }

    fun `test ClassTemplateArgFieldResolutionNeg`() {
        val element = doTest<TableGenFieldBodyItem>()
        assertEquals(element.name, "i")
        val parentClass = assertInstanceOf(element.parent, TableGenClassStatement::class.java)
        assertEquals(parentClass.name, "Bar")
    }

    fun `test LetBodyItemResolution`() {
        val element = doTest<TableGenFieldBodyItem>()
        assertEquals(element.name, "i")
    }

    override fun getTestDataPath(): String? {
        return "src/test/testData/references"
    }

    private inline fun <reified T> doTest(vararg additionalFiles: String): T {
        val name = getTestName(false).trim()

        val mainVF = myFixture.copyFileToProject("${name}.td")
        val list = additionalFiles.map { myFixture.copyFileToProject(it).parent }.toList()
        installCompileCommands(
            project, mapOf(
                mainVF to IncludePaths(list)
            )
        )
        PlatformTestUtil.waitWhileBusy {
            val file =
                PsiManager.getInstance(project).findFile(mainVF) as? TableGenFile ?: return@waitWhileBusy true
            file.context.includePaths != list
        }

        myFixture.configureFromExistingVirtualFile(mainVF)
        return assertInstanceOf(myFixture.elementAtCaret, T::class.java)
    }

}