package com.github.zero9178.mlirods.language.annotators

import com.github.zero9178.mlirods.language.generated.psi.TableGenAbstractClassRef
import com.github.zero9178.mlirods.language.psi.TableGenClassReference
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.psi.PsiElement

private class TableGenNoSuchClassAnnotator : Annotator {
    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        if (element !is TableGenAbstractClassRef) return

        if (element.references.filterIsInstance<TableGenClassReference>()
                .flatMap { reference -> reference.multiResolve(false).map { it.element } }.any { it != null }
        ) return

        holder.newAnnotation(HighlightSeverity.ERROR, "Failed to find class '${element.className}'")
            .highlightType(ProblemHighlightType.LIKE_UNKNOWN_SYMBOL)
            .range(element.classIdentifier).create()
    }
}
