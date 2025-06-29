package com.github.zero9178.mlirods

import com.github.zero9178.mlirods.language.generated.psi.TableGenClassInstantiationValue
import com.github.zero9178.mlirods.language.generated.psi.TableGenClassRef
import com.github.zero9178.mlirods.language.generated.psi.TableGenClassTypeNode
import com.github.zero9178.mlirods.language.generated.psi.TableGenIdentifierValue
import com.github.zero9178.mlirods.language.generated.psi.TableGenListTypeNode
import com.github.zero9178.mlirods.language.generated.psi.TableGenTemplateArgDecl
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.testFramework.assertInstanceOf
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
                assertInstanceOf<TableGenClassInstantiationValue>(it.element)
            })
    }

    fun `test def`() {
        val actual = myFixture.testFindUsagesUsingAction("Def.td").filterIsInstance<PsiElementUsage>()
        assertUnorderedCollection(
            actual, {
                assertInstanceOf<TableGenIdentifierValue>(it.element)
            })
    }

    override fun getTestDataPath(): String {
        return "src/test/testData/findUsages"
    }
}