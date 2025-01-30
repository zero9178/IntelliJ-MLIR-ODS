package com.github.zero9178.mlirods.language.highlighting

import com.github.zero9178.mlirods.language.generated.psi.TableGenLetDirective
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.psi.PsiElement

private class TableGenSemanticTokensAnnotator : Annotator {
    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        when (element) {
            is TableGenLetDirective -> {
                holder.newSilentAnnotation(HighlightSeverity.TEXT_ATTRIBUTES)
                    .range(element.identifier)
                    .textAttributes(DefaultLanguageHighlighterColors.INSTANCE_FIELD)
                    .create()
            }
        }
    }
}