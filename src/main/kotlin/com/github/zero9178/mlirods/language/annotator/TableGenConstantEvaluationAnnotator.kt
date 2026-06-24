package com.github.zero9178.mlirods.language.annotator

import com.github.zero9178.mlirods.MyBundle
import com.github.zero9178.mlirods.language.generated.psi.TableGenBangOperatorValueNode
import com.github.zero9178.mlirods.language.generated.psi.TableGenDefStatement
import com.github.zero9178.mlirods.language.generated.psi.TableGenValueNode
import com.github.zero9178.mlirods.language.psi.TableGenBangOperator
import com.github.zero9178.mlirods.language.psi.impl.TableGenAbstractLetItem
import com.github.zero9178.mlirods.language.values.TableGenIntegerValue
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.psi.PsiElement
import com.intellij.util.takeWhileInclusive

/**
 * Flags a `!div` whose divisor evaluates to the constant `0` within [holder]'s context. Mirroring TableGen, this only
 * fires once both operands fold to concrete integers; a divisor that depends on a (still unknown) template argument
 * does not fold and is therefore not reported.
 */
private fun checkDivisionByZero(element: TableGenBangOperatorValueNode, holder: TableGenEvaluationHolder) {
    if (element.operator != TableGenBangOperator.DIV) return

    // A well-formed '!div' has exactly two operands; a wrong operand count is reported by the syntax annotator.
    val operands = element.valueNodeList
    if (operands.size != 2) return

    val dividend = operands[0].evaluate(holder.context)
    val divisor = operands[1].evaluate(holder.context)
    if (dividend !is TableGenIntegerValue || divisor !is TableGenIntegerValue) return
    if (divisor.value != 0L) return

    holder.error(operands[1], MyBundle.message("tableGen.syntax.divisionByZero"))
}

/**
 * Checks that work by constant-evaluating value nodes.
 */
private val EVALUATION_CHECKS = arrayOf(
    evaluationCheckFor { element: TableGenBangOperatorValueNode, holder -> checkDivisionByZero(element, holder) },
)

/**
 * Returns the children of [element] that are evaluated in [holder]'s context.
 * Specifically elements that are not evaluated due to conditional execution will not be part of the sequence.
 */
private fun liveChildrenOf(element: PsiElement, holder: TableGenEvaluationHolder): Sequence<PsiElement> {
    if (element is TableGenBangOperatorValueNode && element.operator == TableGenBangOperator.IF) {
        // Operands are '[condition, then, else]'. The condition is always evaluated.
        val operands = element.valueNodeList
        if (operands.size == 3) {
            val branch =
                when ((operands[0].evaluate(holder.context) as? TableGenIntegerValue)?.let { it.value != 0L }) {
                    true -> operands[1]
                    false -> operands[2]
                    null -> null // Unknown condition: descend into neither branch.
                }
            return sequenceOf(operands[0], branch).filterNotNull()
        }
    }

    return generateSequence(element.firstChild) { it.nextSibling }
}

/**
 * Yields [root] and its descendant value nodes, evaluated in [holder]'s context, in post-order (a node after its
 * children).
 */
private fun liveValuesPostOrder(
    root: TableGenValueNode, holder: TableGenEvaluationHolder
): Sequence<TableGenValueNode> = sequence {
    val stack = mutableListOf<Pair<PsiElement, Iterator<PsiElement>>>()
    stack.add(root to liveChildrenOf(root, holder).iterator())
    while (stack.isNotEmpty()) {
        val (node, children) = stack.last()
        if (children.hasNext()) {
            val child = children.next()
            stack.add(child to liveChildrenOf(child, holder).iterator())
        } else {
            stack.removeLast()
            if (node is TableGenValueNode) yield(node)
        }
    }
}

/**
 * Runs the [EVALUATION_CHECKS] over [root] and its live descendant value nodes, evaluated in [holder]'s context.
 */
private fun visitLiveValues(root: TableGenValueNode, holder: TableGenEvaluationHolder) {
    liveValuesPostOrder(root, holder).forEach { element ->
        EVALUATION_CHECKS.forEach { check ->
            // Only report a problem here if it does not already fail in the null context. Such constant problems are
            // reported by the direct pass, so reporting them again for every instantiation would duplicate the annotation.
            val probe = TableGenProbeEvaluationHolder()
            check(element, probe)
            if (!probe.emittedError) check(element, holder)
        }
    }
}

/**
 * Re-runs the [EVALUATION_CHECKS] over every final field expression of [def], evaluated in the def's instantiation
 * context. This catches problems that only become visible once a class's template arguments and fields are bound to
 * concrete values, e.g. a `!div` whose divisor is a template argument that this def sets to `0`.
 */
private fun checkInstantiation(def: TableGenDefStatement, holder: AnnotationHolder) {
    val anchor = def.nameIdentifier ?: return
    val evaluationHolder = TableGenInstantiationEvaluationHolder(def, anchor, holder)
    def.allFieldAssignments.values.asSequence().flatMap { assignments ->
        // Include previous field assignments if the 'let' is in prepend or append mode.
        assignments.asReversed().asSequence().takeWhileInclusive {
            when (it) {
                is TableGenAbstractLetItem -> it.letMode != null
                else -> false
            }
        }
    }.mapNotNull { it.assignedValueNode }.forEach {
        visitLiveValues(it, evaluationHolder)
    }
}

private val ANNOTATIONS = arrayOf(
    // Run the evaluation-based checks directly on each value node, evaluated in the null context. A problem found here
    // is constant (e.g. '!div(6, 0)') and therefore wrong as written, so it is reported even inside a dead '!if' branch.
    addAnnotationFor { element: TableGenValueNode, holder: AnnotationHolder ->
        val evaluationHolder = TableGenDirectEvaluationHolder(holder)
        EVALUATION_CHECKS.forEach { it(element, evaluationHolder) }
    },
    // Re-run them on every def, this time through the def's instantiation context.
    addAnnotationFor { element: TableGenDefStatement, holder -> checkInstantiation(element, holder) },
)

/**
 * Annotator reporting problems found by constant-evaluating value nodes, both as written and as seen through each def's
 * instantiation of a class.
 */
internal class TableGenConstantEvaluationAnnotator : TableGenAnnotator(ANNOTATIONS.asIterable())
