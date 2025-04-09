package com.github.zero9178.mlirods.language

import com.github.zero9178.mlirods.language.generated.psi.TableGenClassStatement
import com.github.zero9178.mlirods.language.generated.psi.TableGenDefStatement
import com.github.zero9178.mlirods.language.generated.psi.TableGenFieldBodyItem
import com.github.zero9178.mlirods.language.generated.psi.TableGenForeachOperatorValue
import com.intellij.openapi.util.NlsSafe
import com.intellij.psi.ElementDescriptionLocation
import com.intellij.psi.ElementDescriptionProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNamedElement
import com.intellij.usageView.UsageViewLongNameLocation
import com.intellij.usageView.UsageViewNodeTextLocation
import com.intellij.usageView.UsageViewTypeLocation

private class TableGenElementDescriptionProvider : ElementDescriptionProvider {
    override fun getElementDescription(
        element: PsiElement,
        location: ElementDescriptionLocation
    ): @NlsSafe String? {
        if (element is PsiNamedElement)
            if (location is UsageViewLongNameLocation || location is UsageViewNodeTextLocation)
                getElementDescription(element, UsageViewTypeLocation.INSTANCE)?.let {
                    element.name?.let { name ->
                        return "$it '$name'"
                    }
                }


        if (location !is UsageViewTypeLocation) return null

        return when (element) {
            is TableGenFieldBodyItem -> "field"
            is TableGenDefStatement -> "record"
            is TableGenClassStatement -> "class"
            is TableGenForeachOperatorValue -> "iterator"
            else -> null
        }
    }
}