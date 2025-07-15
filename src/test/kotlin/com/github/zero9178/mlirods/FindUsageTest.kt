package com.github.zero9178.mlirods

import com.github.zero9178.mlirods.language.generated.psi.TableGenClassInstantiationValue
import com.github.zero9178.mlirods.language.generated.psi.TableGenClassRef
import com.github.zero9178.mlirods.language.generated.psi.TableGenClassTypeNode
import com.github.zero9178.mlirods.language.generated.psi.TableGenIdentifierValue
import com.github.zero9178.mlirods.language.generated.psi.TableGenIncludeDirective
import com.github.zero9178.mlirods.language.generated.psi.TableGenListTypeNode
import com.github.zero9178.mlirods.language.generated.psi.TableGenTemplateArgDecl
import com.github.zero9178.mlirods.model.IncludePaths
import com.github.zero9178.mlirods.model.TableGenContext
import com.intellij.psi.ElementDescriptionUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.testFramework.assertInstanceOf
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