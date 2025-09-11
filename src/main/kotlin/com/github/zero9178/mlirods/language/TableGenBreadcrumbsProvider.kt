package com.github.zero9178.mlirods.language

import com.github.zero9178.mlirods.language.generated.psi.TableGenClassStatement
import com.github.zero9178.mlirods.language.generated.psi.TableGenDefStatement
import com.github.zero9178.mlirods.language.generated.psi.TableGenDefvarStatement
import com.github.zero9178.mlirods.language.generated.psi.TableGenFieldBodyItem
import com.github.zero9178.mlirods.language.generated.psi.TableGenForeachStatement
import com.github.zero9178.mlirods.language.generated.psi.TableGenIfStatement
import com.github.zero9178.mlirods.language.generated.psi.TableGenLetBodyItem
import com.github.zero9178.mlirods.language.generated.psi.TableGenLetStatement
import com.github.zero9178.mlirods.language.generated.psi.TableGenMulticlassStatement
import com.intellij.navigation.NavigationItem
import com.intellij.openapi.util.NlsSafe
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNamedElement
import com.intellij.ui.breadcrumbs.BreadcrumbsProvider
import javax.swing.Icon

private class TableGenBreadcrumbsProvider : BreadcrumbsProvider {
    override fun getLanguages() = arrayOf(TableGenLanguage.INSTANCE)

    override fun acceptElement(element: PsiElement): Boolean {
        return when (element) {
            is TableGenClassStatement, is TableGenDefStatement, is TableGenMulticlassStatement,
            is TableGenLetStatement, is TableGenIfStatement, is TableGenForeachStatement,
            is TableGenLetBodyItem, is TableGenFieldBodyItem, is TableGenDefvarStatement -> true

            else -> false
        }
    }

    override fun getElementInfo(element: PsiElement): @NlsSafe String {
        return when (element) {
            is TableGenFieldBodyItem -> element.fieldName ?: ""
            is TableGenLetBodyItem -> "let " + (element.fieldName ?: "")
            is PsiNamedElement -> element.name ?: ""
            else -> ""
        }
    }

    override fun getElementIcon(element: PsiElement): Icon? {
        return when (element) {
            is NavigationItem -> element.getIcon(0)
            else -> super.getElementIcon(element)
        }
    }
}