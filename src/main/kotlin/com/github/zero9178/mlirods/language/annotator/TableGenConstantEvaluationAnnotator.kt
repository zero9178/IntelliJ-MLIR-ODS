package com.github.zero9178.mlirods.language.annotator

import com.github.zero9178.mlirods.MyBundle
import com.github.zero9178.mlirods.language.generated.psi.TableGenBangOperatorValueNode
import com.github.zero9178.mlirods.language.generated.psi.TableGenDefStatement
import com.github.zero9178.mlirods.language.generated.psi.TableGenFieldBodyItem
import com.github.zero9178.mlirods.language.generated.psi.TableGenIdentifierValueNode
import com.github.zero9178.mlirods.language.generated.psi.TableGenValueNode
import com.github.zero9178.mlirods.language.psi.TableGenBangOperator
import com.github.zero9178.mlirods.language.psi.TableGenFieldAssignmentNode
import com.github.zero9178.mlirods.language.psi.impl.TableGenAbstractLetItem
import com.github.zero9178.mlirods.language.psi.impl.TableGenEvaluationContext
import com.github.zero9178.mlirods.language.values.TableGenIntegerValue
import com.github.zero9178.mlirods.model.TableGenCompilationContext
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.progress.runBlockingCancellable
import com.intellij.psi.PsiElement
import com.intellij.util.takeWhileInclusive
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/**
 * Flags a `!div` whose divisor evaluates to the constant `0` within [holder]'s context. Mirroring TableGen, this only
 * fires once both operands fold to concrete integers; a divisor that depends on a (still unknown) template argument
 * does not fold and is therefore not reported.
 */
private suspend fun checkDivisionByZero(element: TableGenBangOperatorValueNode, holder: TableGenEvaluationHolder) {
    // A well-formed '!div' has exactly two operands; a wrong operand count is reported by the syntax annotator.
    val operands = element.valueNodeList
    if (operands.size != 2) return

    val (dividend, divisor) = coroutineScope {
        operands.map { async { it.evaluate(holder.context) } }.awaitAll()
    }
    if (dividend !is TableGenIntegerValue || divisor !is TableGenIntegerValue) return
    if (divisor.value != 0L) return

    holder.error(operands[1], MyBundle.message("tableGen.syntax.divisionByZero"))
}

/**
 * Checks that work by constant-evaluating value nodes.
 */
private val EVALUATION_CHECKS = arrayOf(
    evaluationCheckFor<TableGenBangOperatorValueNode>({ it.operator == TableGenBangOperator.DIV }) { element, holder ->
        checkDivisionByZero(element, holder)
    },
)

/**
 * Returns the children of [element] that are evaluated in [context].
 * Specifically elements that are not evaluated due to conditional execution will not be part of the result.
 */
private suspend fun liveChildrenOf(element: PsiElement, context: TableGenEvaluationContext): List<PsiElement> {
    if (element is TableGenBangOperatorValueNode && element.operator == TableGenBangOperator.IF) {
        // Operands are '[condition, then, else]'. The condition is always evaluated.
        val operands = element.valueNodeList
        if (operands.size == 3) {
            val branch =
                when ((operands[0].evaluate(context) as? TableGenIntegerValue)?.let { it.value != 0L }) {
                    true -> operands[1]
                    false -> operands[2]
                    null -> null // Unknown condition: descend into neither branch.
                }
            return listOfNotNull(operands[0], branch)
        }
    }

    // Leaves are neither value nodes nor do they contain any.
    return generateSequence(element.firstChild) { it.nextSibling }.filter { it.firstChild != null }.toList()
}

/**
 * Returns [root] and its descendant value nodes, evaluated in [context], in post-order (a node after its children).
 *
 * The tree is walked by one coroutine: a coroutine per node costs far more than visiting the node does.
 */
private suspend fun liveValuesPostOrder(
    root: TableGenValueNode, context: TableGenEvaluationContext
): List<TableGenValueNode> = buildList {
    val stack = mutableListOf<Pair<PsiElement, Iterator<PsiElement>>>()
    stack.add(root to liveChildrenOf(root, context).iterator())
    while (stack.isNotEmpty()) {
        val (node, children) = stack.last()
        if (children.hasNext()) {
            val child = children.next()
            stack.add(child to liveChildrenOf(child, context).iterator())
        } else {
            stack.removeLast()
            if (node is TableGenValueNode) add(node)
        }
    }
}

/**
 * Runs the [EVALUATION_CHECKS] over [root] and its live descendant value nodes, evaluated in [holder]'s context.
 */
private suspend fun visitLiveValues(root: TableGenValueNode, holder: TableGenEvaluationHolder) {
    liveValuesPostOrder(root, holder.context).forEach { element ->
        EVALUATION_CHECKS.forEach { check ->
            if (!check.appliesTo(element)) return@forEach

            // Only report a problem here if it does not already fail in the null context. Such constant problems are
            // reported by the direct pass, so reporting them again for every instantiation would duplicate the annotation.
            val probe = TableGenProbeEvaluationHolder(holder.context.compilationContext)
            check(element, probe)
            if (!probe.emittedError) check(element, holder)
        }
    }
}

/**
 * Returns the values making up a field whose [assignments] are given in order of application: the final assignment and,
 * if it is a 'let' in prepend or append mode, the previous ones it builds upon.
 */
private fun effectiveFieldValues(assignments: List<TableGenFieldAssignmentNode>): Sequence<TableGenValueNode> =
    assignments.asReversed().asSequence().takeWhileInclusive {
        when (it) {
            is TableGenAbstractLetItem -> it.letMode != null
            else -> false
        }
    }.mapNotNull { it.assignedValueNode }

/**
 * Re-runs the [EVALUATION_CHECKS] over every final field expression of [def], evaluated in the def's instantiation
 * context. This catches problems that only become visible once a class's template arguments and fields are bound to
 * concrete values, e.g. a `!div` whose divisor is a template argument that this def sets to `0`.
 */
private fun checkInstantiation(
    def: TableGenDefStatement, holder: AnnotationHolder, context: TableGenCompilationContext
) {
    val anchor = def.nameIdentifier ?: return
    val evaluationHolder = TableGenInstantiationEvaluationHolder(def, anchor, holder, context)
    val values = def.allFieldAssignments(context).values.asSequence().flatMap(::effectiveFieldValues).toList()
    if (values.isEmpty()) return

    runBlockingCancellable {
        values.forEach { visitLiveValues(it, evaluationHolder) }
    }
    evaluationHolder.flush()
}

/**
 * Returns the fields reachable from [start] in the field dependency graph [dependencies], excluding [start] itself
 * unless it lies on a cycle.
 */
private fun reachableFields(start: String, dependencies: Map<String, Set<String>>): Set<String> {
    val reached = mutableSetOf<String>()
    val worklist = ArrayDeque(dependencies[start].orEmpty())
    while (worklist.isNotEmpty()) {
        val field = worklist.removeLast()
        if (reached.add(field)) worklist.addAll(dependencies[field].orEmpty())
    }
    return reached
}

/**
 * Reports fields of [def] that are defined in terms of each other, e.g. 'int g = f; let f = g;'. Such fields can never
 * be resolved, which TableGen reports as an error on the def. The evaluator yields unknown values for them instead (see
 * [TableGenEvaluationContext]).
 *
 * Only the fields on a cycle are reported, not fields merely referring to one. References within '!if' branches not
 * taken are ignored, as TableGen does not resolve them either.
 */
private fun checkCyclicFields(
    def: TableGenDefStatement, holder: AnnotationHolder, context: TableGenCompilationContext
) {
    val anchor = def.nameIdentifier ?: return
    val evaluationContext = TableGenEvaluationContext(def, context)
    val assignments = def.allFieldAssignments(context)
    if (assignments.isEmpty()) return

    // Maps each field to the fields its value refers to.
    val dependencies = runBlockingCancellable {
        assignments.mapValues { (_, assignments) ->
            effectiveFieldValues(assignments).toList().flatMap { liveValuesPostOrder(it, evaluationContext) }
                .filterIsInstance<TableGenIdentifierValueNode>()
                .mapNotNull { (it.referencedDeclaration(context) as? TableGenFieldBodyItem)?.fieldName }
                .toSet()
        }
    }
    val reachable = dependencies.keys.associateWith { reachableFields(it, dependencies) }

    // Fields on the same cycle reach each other. Group them to report each cycle once.
    val cycles = reachable.filter { (field, reached) -> field in reached }.map { (field, reached) ->
        reached.filter { field in reachable[it].orEmpty() }.sorted()
    }.distinct()
    for (cycle in cycles) {
        val message = cycle.singleOrNull()?.let { MyBundle.message("tableGen.syntax.selfReferentialField", it) }
            ?: MyBundle.message("tableGen.syntax.cyclicFields", cycle.joinToString { "'$it'" })
        holder.newAnnotation(HighlightSeverity.ERROR, message).range(anchor).create()
    }
}

private val ANNOTATIONS = arrayOf(
    // Run the evaluation-based checks directly on each value node, evaluated in the null context. A problem found here
    // is constant (e.g. '!div(6, 0)') and therefore wrong as written, so it is reported even inside a dead '!if' branch.
    addAnnotationFor { element: TableGenValueNode, holder: AnnotationHolder, context ->
        val checks = EVALUATION_CHECKS.filter { it.appliesTo(element) }
        if (checks.isEmpty()) return@addAnnotationFor

        val evaluationHolder = TableGenDirectEvaluationHolder(holder, context)
        runBlockingCancellable {
            checks.forEach { launch { it(element, evaluationHolder) } }
        }
        evaluationHolder.flush()
    },
    // Re-run them on every def, this time through the def's instantiation context.
    addAnnotationFor { element: TableGenDefStatement, holder, context ->
        checkInstantiation(element, holder, context)
    },
    addAnnotationFor { element: TableGenDefStatement, holder, context ->
        checkCyclicFields(element, holder, context)
    },
)

/**
 * Annotator reporting problems found by constant-evaluating value nodes, both as written and as seen through each def's
 * instantiation of a class.
 */
internal class TableGenConstantEvaluationAnnotator : TableGenAnnotator(ANNOTATIONS.asIterable())
