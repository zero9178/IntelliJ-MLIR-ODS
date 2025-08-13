package com.github.zero9178.mlirods.language

import com.github.zero9178.mlirods.language.generated.psi.TableGenClassStatement
import com.github.zero9178.mlirods.language.generated.psi.TableGenFieldBodyItem
import com.github.zero9178.mlirods.language.psi.TableGenIdentifierElement
import com.intellij.lang.findUsages.FindUsagesProvider
import com.intellij.psi.ElementDescriptionUtil
import com.intellij.psi.PsiElement
import com.intellij.usageView.UsageViewLongNameLocation
import com.intellij.usageView.UsageViewNodeTextLocation
import com.intellij.usageView.UsageViewTypeLocation
import org.jetbrains.annotations.Nls

private class TableGenFindUsageProvider : FindUsagesProvider {
    override fun canFindUsagesFor(psiElement: PsiElement) = when (psiElement) {
        is TableGenClassStatement, is TableGenIdentifierElement, is TableGenFieldBodyItem -> true
        else -> false
    }

    override fun getHelpId(psiElement: PsiElement) = null

    override fun getType(element: PsiElement): @Nls String {
        return ElementDescriptionUtil.getElementDescription(element, UsageViewTypeLocation.INSTANCE)
    }

    override fun getDescriptiveName(element: PsiElement): @Nls String {
        return ElementDescriptionUtil.getElementDescription(element, UsageViewLongNameLocation.INSTANCE)
    }

    override fun getNodeText(
        element: PsiElement,
        useFullName: Boolean
    ): @Nls String {
        return ElementDescriptionUtil.getElementDescription(element, UsageViewNodeTextLocation.INSTANCE)
    }
}