package com.github.zero9178.mlirods

import com.github.zero9178.mlirods.language.generated.psi.*
import com.github.zero9178.mlirods.model.IncludePaths
import com.intellij.openapi.application.runWriteAction
import com.intellij.psi.PsiFile
import com.intellij.psi.util.parentOfType
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.testFramework.utils.vfs.deleteRecursively

class ReferenceTest : BasePlatformTestCase() {
    fun `test IncludeReference`() {
        val testFile = myFixture.copyFileToProject("test.td")
        val virtualFile = myFixture.copyFileToProject("HasCompileCommands.td")
        val targetFile = myFixture.copyFileToProject("IncludeReference.td")
        installCompileCommands(
            project, mapOf(
                virtualFile to IncludePaths(listOf(testFile.parent))
            ),
            listOf(
                targetFile to IncludePaths(listOf(testFile.parent))
            )
        )

        myFixture.configureFromExistingVirtualFile(targetFile)
        val element = assertInstanceOf(myFixture.elementAtCaret, PsiFile::class.java)
        assertEquals(element.viewProvider.virtualFile.name, "test.td")
    }

    fun `test included from context`() {
        val testFile = myFixture.createFile(
            "test.td", """
            def : <caret>A;
        """.trimIndent()
        )
        val root = myFixture.createFile(
            "HasCompileCommands.td", """
            class A;
            
            include "test.td"
        """.trimIndent()
        )
        installCompileCommands(
            project, mapOf(
                root to IncludePaths(listOf(testFile.parent))
            ),
            listOf(
                testFile to IncludePaths(listOf(testFile.parent))
            )
        )

        myFixture.configureFromExistingVirtualFile(testFile)
        val element = assertInstanceOf(myFixture.elementAtCaret, TableGenClassStatement::class.java)
        assertEquals(element.name, "A")
    }

    fun `test included before context`() {
        val testFile = myFixture.createFile(
            "test.td", """
            def : <caret>A;
        """.trimIndent()
        )
        myFixture.createFile(
            "class.td", """
            class A;
        """.trimIndent()
        )
        val root = myFixture.createFile(
            "HasCompileCommands.td", """
            include "class.td"
            include "test.td"
        """.trimIndent()
        )
        installCompileCommands(
            project, mapOf(
                root to IncludePaths(listOf(testFile.parent))
            ),
            listOf(
                testFile to IncludePaths(listOf(testFile.parent))
            )
        )

        myFixture.configureFromExistingVirtualFile(testFile)
        val element = assertInstanceOf(myFixture.elementAtCaret, TableGenClassStatement::class.java)
        assertEquals(element.name, "A")
    }

    fun `test IncludeReference exception`() {
        val testFile = myFixture.copyFileToProject("test.td")
        val virtualFile = myFixture.copyFileToProject("HasCompileCommands.td", "toBeDeleted/HasCompileCommands.td")
        val targetFile = myFixture.copyFileToProject("IncludeReference.td")
        installCompileCommands(
            project, mapOf(
                targetFile to IncludePaths(listOf(virtualFile.parent, testFile.parent))
            )
        )
        runWriteAction {
            virtualFile.parent.deleteRecursively()
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

    fun `test ClassSelfTypeResolution`() {
        val element = doTest<TableGenClassStatement>("ClassSelfTypeResolution.td")
        assertEquals(element.name, "G")
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

    fun `test FieldDefResolutionLexical`() {
        val element = doTest<TableGenDefvarStatement>()
        assertEquals(element.name, "i")
    }

    fun `test FieldDefParentResolutionLexical`() {
        val element = doTest<TableGenTemplateArgDecl>()
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

    fun `test FieldAccessResolution`() {
        val element = doTest<TableGenFieldBodyItem>()
        assertEquals(element.name, "i")
    }

    fun `test ForeachDefvarResolution`() {
        val element = doTest<TableGenBangOperatorDefinition>()
        assertEquals(element.name, "i")
    }

    fun `test ForeachDefvarResolutionIterable`() {
        val element = doTest<TableGenDefvarStatement>()
        assertEquals(element.name, "i")
    }

    fun `test ForeachDefvarResolutionIterableParent`() {
        val element = doTest<TableGenDefvarStatement>()
        assertEquals(element.name, "i")
    }

    fun `test FoldlIteratorDefvarResolution`() {
        val element = doTest<TableGenBangOperatorDefinition>()
        assertEquals(element.name, "i")
    }

    fun `test FoldlAccDefvarResolution`() {
        val element = doTest<TableGenBangOperatorDefinition>()
        assertEquals(element.name, "acc")
    }


    fun `test FoldlDefvarResolutionIterable`() {
        val element = doTest<TableGenDefvarStatement>()
        assertEquals(element.name, "i")
    }

    fun `test ParentMultiClassListResolution`() {
        val name = getTestName(false).trim()
        val mainVF = myFixture.copyFileToProject("${name}.td")
        myFixture.configureFromExistingVirtualFile(mainVF)

        // For now ensure that the multiclass ref does not find a class statement.
        // Multi class reference will be implemented later.
        assertNull(myFixture.file.findReferenceAt(myFixture.caretOffset)?.resolve())
    }

    fun `test in let statement`() {
        val statement = doTestInline<TableGenDefStatement>("""
            class AArch64Unsupported { list<Predicate> F; }

            let F = [] in def SVE2p1Unsupported : AArch64Unsupported;

            defvar v = <caret>SVE2p1Unsupported;
        """.trimIndent())
        assertEquals(statement.name, "SVE2p1Unsupported")
    }

    fun `test class decl statement`() {
        val field = doTestInline<TableGenFieldBodyItem>(
            """
            class Instruction;

            class Instruction {
                int i = 0;
            }
            
            def : Instruction {
                let <caret>i = 5;
            }
        """.trimIndent()
        )
        assertEquals(field.fieldName, "i")
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

        myFixture.configureFromExistingVirtualFile(mainVF)
        return assertInstanceOf(myFixture.elementAtCaret, T::class.java)
    }


    private inline fun <reified T> doTestInline(source: String): T {
        val mainVF = myFixture.createFile("test.td", source)
        installCompileCommands(
            project, mapOf(
                mainVF to IncludePaths(emptyList())
            )
        )

        myFixture.configureFromExistingVirtualFile(mainVF)
        return assertInstanceOf(myFixture.elementAtCaret, T::class.java)
    }
}