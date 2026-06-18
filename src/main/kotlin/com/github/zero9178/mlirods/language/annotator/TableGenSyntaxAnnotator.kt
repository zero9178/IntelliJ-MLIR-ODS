package com.github.zero9178.mlirods.language.annotator

import com.github.zero9178.mlirods.MyBundle
import com.github.zero9178.mlirods.language.generated.psi.TableGenAbstractClassRef
import com.github.zero9178.mlirods.language.generated.psi.TableGenSwitchOperatorValueNode
import com.github.zero9178.mlirods.language.psi.impl.TableGenAbstractLetItem
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.project.DumbAware

private val ANNOTATIONS = arrayOf(
    addAnnotationFor { element: TableGenAbstractLetItem, holder ->
        val letModeIdentifier = element.letModeIdentifier
        if (letModeIdentifier != null && element.letMode == null)
            holder.newAnnotation(
                HighlightSeverity.ERROR, MyBundle.message("tableGen.syntax.invalidLetMode", letModeIdentifier.text)
            ).range(letModeIdentifier).create()
    },
    /**
     * Verifies the grammar invariants of a `!switch` operator: there must be at least one `case : value` pair, every
     * clause but the last must be such a pair, and the last clause must be the mandatory default value (a clause without
     * a `:`).
     */
    addAnnotationFor { element: TableGenSwitchOperatorValueNode, holder ->
        val clauses = element.switchClauseList
        if (clauses.isEmpty()) return@addAnnotationFor

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
    },
    addAnnotationFor { element: TableGenAbstractClassRef, holder ->
        // First drop all positional arguments, followed by all named arguments.
        // If any arguments remain after, then there must have been a positional argument after a named argument.
        element.argValueItemList.asSequence().dropWhile {
            it.isPositionalArgument
        }.dropWhile {
            it.isNamedArgument
        }.firstOrNull()?.let {
            holder.newAnnotation(
                HighlightSeverity.ERROR, MyBundle.message("tableGen.syntax.positionalAfterNamed")
            ).range(it).create()
        }
    },
)

internal class TableGenSyntaxAnnotator : TableGenAnnotator(ANNOTATIONS.asIterable()), DumbAware
