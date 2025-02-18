package com.github.zero9178.mlirods.language.highlighting

import com.github.zero9178.mlirods.color.FIELD
import com.github.zero9178.mlirods.color.PREPROCESSOR_MACRO_NAME
import com.github.zero9178.mlirods.language.generated.psi.TableGenPreprocessorDirective
import com.github.zero9178.mlirods.language.psi.TableGenFieldIdentifierNode
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.project.DumbAware
import com.intellij.psi.PsiElement

private class TableGenSemanticTokensAnnotator : Annotator, DumbAware {
    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        when (element) {
            is TableGenFieldIdentifierNode -> {
                holder.newSilentAnnotation(HighlightSeverity.TEXT_ATTRIBUTES)
                    .range(element.fieldIdentifier)
                    .textAttributes(FIELD)
                    .create()
            }

            is TableGenPreprocessorDirective -> {
                val identifier = element.identifier ?: return
                holder.newSilentAnnotation(HighlightSeverity.TEXT_ATTRIBUTES)
                    .range(identifier)
                    .textAttributes(PREPROCESSOR_MACRO_NAME)
                    .create()
            }
        }
    }
}