package com.github.zero9178.mlirods.language.annotator

import com.github.zero9178.mlirods.MyBundle
import com.github.zero9178.mlirods.language.psi.impl.TableGenAbstractLetItem
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.project.DumbAware
import com.intellij.psi.PsiElement

internal class TableGenSyntaxAnnotator : Annotator, DumbAware {
    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        when (element) {
            is TableGenAbstractLetItem -> {
                val letModeIdentifier = element.letModeIdentifier
                if (letModeIdentifier != null && element.letMode == null) holder.newAnnotation(
                    HighlightSeverity.ERROR, MyBundle.message("tableGen.syntax.invalidLetMode", letModeIdentifier.text)
                ).range(letModeIdentifier).create()
            }
        }
    }
}