package com.github.zero9178.mlirods.language.annotator

import com.github.zero9178.mlirods.MyBundle
import com.github.zero9178.mlirods.language.generated.psi.TableGenSwitchOperatorValueNode
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

            is TableGenSwitchOperatorValueNode -> annotateSwitch(element, holder)
        }
    }

    /**
     * Verifies the grammar invariants of a `!switch` operator: there must be at least one `case : value` pair, every
     * clause but the last must be such a pair, and the last clause must be the mandatory default value (a clause without
     * a `:`).
     */
    private fun annotateSwitch(element: TableGenSwitchOperatorValueNode, holder: AnnotationHolder) {
        val clauses = element.switchClauseList
        if (clauses.isEmpty()) return

        // Besides the trailing default value, at least one 'case : value' pair is mandatory.
        if (clauses.none { it.hasColon }) holder.newAnnotation(
            HighlightSeverity.ERROR, MyBundle.message("tableGen.syntax.switch.missingCase")
        ).range(element).create()

        clauses.forEachIndexed { index, clause ->
            val isLast = index == clauses.lastIndex
            if (isLast) {
                // The trailing argument is the default and must therefore not have a ':'.
                if (clause.hasColon) holder.newAnnotation(
                    HighlightSeverity.ERROR, MyBundle.message("tableGen.syntax.switch.missingDefault")
                ).range(clause).create()
            } else {
                // Every non-trailing argument is a 'case : value' pair and must have a ':'.
                if (!clause.hasColon) holder.newAnnotation(
                    HighlightSeverity.ERROR, MyBundle.message("tableGen.syntax.switch.misplacedDefault")
                ).range(clause).create()
            }
        }
    }
}
