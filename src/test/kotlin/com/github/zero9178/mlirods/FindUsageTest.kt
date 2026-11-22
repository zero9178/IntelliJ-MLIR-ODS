package com.github.zero9178.mlirods

import com.github.zero9178.mlirods.language.generated.psi.*
import com.github.zero9178.mlirods.model.IncludePaths
import com.intellij.psi.ElementDescriptionUtil
import com.intellij.testFramework.assertInstanceOf
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.usageView.UsageViewTypeLocation
import com.intellij.usages.rules.PsiElementUsage

class FindUsageTest : BasePlatformTestCase() {

    fun `test class`() {
        val actual = myFixture.testFindUsagesUsingAction("Class.td").filterIsInstance<PsiElementUsage>()
        assertUnorderedCollection(
            actual, {
                val element = assertInstanceOf<TableGenClassTypeNode>(it.element)
                assertInstanceOf<TableGenTemplateArgDecl>(element.parent)
            },
            {
                assertInstanceOf<TableGenClassRef>(it.element)
            },
            {
                assertInstanceOf<TableGenListTypeNode>(assertInstanceOf<TableGenClassTypeNode>(it.element).parent)
            },
            {
                assertInstanceOf<TableGenClassInstantiationValueNode>(it.element)
            })
    }

    fun `test multiclass`() {
        val actual = myFixture.testFindUsagesUsingAction("Multiclass.td").filterIsInstance<PsiElementUsage>()
        assertUnorderedCollection(
            actual, {
                val element = assertInstanceOf<TableGenMultiClassRef>(it.element)
                assertInstanceOf<TableGenMulticlassStatement>(element.parent)
            },
            {
                val element = assertInstanceOf<TableGenMultiClassRef>(it.element)
                assertInstanceOf<TableGenDefmStatement>(element.parent)
            })
    }

    fun `test field assigned by top-level let`() {
        val file = myFixture.configureByText(
            "test.td", """
            class C { int <caret>x; }
            let x = 1 in def a : C;
        """.trimIndent()
        )
        val usages = myFixture.findUsages(myFixture.elementAtCaret)
        assertInstanceOf<TableGenLetItem>(assertOneElement(usages).element)
        assertEquals(file.text.indexOf("x = 1"), assertOneElement(usages).navigationOffset)
    }

    fun `test def`() {
        val actual = myFixture.testFindUsagesUsingAction("Def.td").filterIsInstance<PsiElementUsage>()
        assertUnorderedCollection(
            actual, {
                assertInstanceOf<TableGenIdentifierValueNode>(it.element)
            })
    }

    fun `test define`() {
        val actual = myFixture.testFindUsagesUsingAction("Define.td").filterIsInstance<PsiElementUsage>()
        assertUnorderedCollection(
            actual, {
                assertInstanceOf<TableGenIfdefIfndefDirective>(it.element)
            },
            {
                assertInstanceOf<TableGenIfdefIfndefDirective>(it.element)
            })
    }

    // References to a field, including its redefinitions, resolve to the field body item that first defined it,
    // making a redefinition stand for the defining field.
    fun `test redefined field finds usages of defining field`() {
        myFixture.configureByText(
            "test.td", """
            class A {
                int x = 0;
            }
            class B : A {
                int <caret>x = 1;
                int y = x;
            }
            def D : B {
                let x = 2;
            }
            defvar v = D.x;
        """.trimIndent()
        )
        val actual = myFixture.testFindUsagesUsingAction().filterIsInstance<PsiElementUsage>()
        assertUnorderedCollection(
            actual, {
                assertInstanceOf<TableGenIdentifierValueNode>(it.element)
            },
            {
                assertInstanceOf<TableGenLetBodyItem>(it.element)
            },
            {
                assertInstanceOf<TableGenFieldAccessValueNode>(it.element)
            },
            {
                assertInstanceOf<TableGenFieldBodyItem>(it.element)
            })
    }

    fun `test defining field finds usages after redefinition`() {
        myFixture.configureByText(
            "test.td", """
            class A {
                int <caret>x = 0;
            }
            class B : A {
                int x = 1;
                int y = x;
            }
            def D : B {
                let x = 2;
            }
            defvar v = D.x;
        """.trimIndent()
        )
        val actual = myFixture.testFindUsagesUsingAction().filterIsInstance<PsiElementUsage>()
        assertUnorderedCollection(
            actual, {
                assertInstanceOf<TableGenIdentifierValueNode>(it.element)
            },
            {
                assertInstanceOf<TableGenLetBodyItem>(it.element)
            },
            {
                assertInstanceOf<TableGenFieldAccessValueNode>(it.element)
            },
            {
                assertInstanceOf<TableGenFieldBodyItem>(it.element)
            })
    }

    fun `test include`() {
        val includeFile = myFixture.copyFileToProject("Include.td")
        val defFile = myFixture.copyFileToProject("Def.td")
        installCompileCommands(project, mapOf(includeFile to IncludePaths(listOf(defFile.parent))))

        val actual = myFixture.testFindUsagesUsingAction("Include.td").filterIsInstance<PsiElementUsage>()
        assertUnorderedCollection(
            actual, {
                assertInstanceOf<TableGenIncludeDirective>(it.element)
                ElementDescriptionUtil.getElementDescription(it.element, UsageViewTypeLocation.INSTANCE)
            })
    }

    override fun getTestDataPath(): String {
        return "src/test/testData/findUsages"
    }
}