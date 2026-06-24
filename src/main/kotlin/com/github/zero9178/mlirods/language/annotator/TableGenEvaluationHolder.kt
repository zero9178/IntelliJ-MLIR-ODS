package com.github.zero9178.mlirods.language.annotator

import com.github.zero9178.mlirods.language.generated.psi.TableGenDefStatement
import com.github.zero9178.mlirods.language.generated.psi.TableGenValueNode
import com.github.zero9178.mlirods.language.psi.impl.TableGenEvaluationContext
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.psi.PsiElement

/**
 * Abstraction over [AnnotationHolder] for checks that operate by constant-evaluating value nodes.
 *
 * The same check (e.g. division-by-zero) needs to run in two different situations:
 *  - From a regular [Annotator], directly on a value node as written. Evaluation happens in the
 *    [null context][TableGenEvaluationContext] and a problem is shown on the offending element itself.
 *  - From the def-statement annotator, which re-evaluates a record's field expressions through the lens of a concrete
 *    instantiation. Evaluation happens in that def's [context][TableGenEvaluationContext] and a problem is shown on the
 *    def being highlighted, since the offending expression typically lives in a (possibly out-of-file) base class.
 *
 * Implementations decide which [context] to evaluate in and where (or whether) a reported problem ends up as an
 * annotation. A check therefore only has to express *what* is wrong, not *how* it should be surfaced.
 */
interface TableGenEvaluationHolder {
    /**
     * Context in which checks should evaluate value nodes.
     */
    val context: TableGenEvaluationContext

    /**
     * Reports an error with the given [message]. [element] is the offending value node; implementations may use it to
     * position the annotation and/or to decide whether the problem is relevant.
     */
    fun error(element: PsiElement, message: String)
}

/**
 * A check that inspects a [TableGenValueNode] and reports problems via [TableGenEvaluationHolder].
 */
typealias TableGenEvaluationCheck = (element: TableGenValueNode, holder: TableGenEvaluationHolder) -> Unit

/**
 * Creates a [TableGenEvaluationCheck] that runs [check] whenever a value node of type [T] is encountered.
 */
inline fun <reified T : TableGenValueNode> evaluationCheckFor(crossinline check: (T, TableGenEvaluationHolder) -> Unit): TableGenEvaluationCheck =
    { element, holder ->
        if (element is T) check(element, holder)
    }

/**
 * [TableGenEvaluationHolder] used by a regular [Annotator]: evaluates in the null context and surfaces problems
 * directly on the offending element.
 */
internal class TableGenDirectEvaluationHolder(private val holder: AnnotationHolder) : TableGenEvaluationHolder {
    override val context = TableGenEvaluationContext()

    override fun error(element: PsiElement, message: String) {
        holder.newAnnotation(HighlightSeverity.ERROR, message).range(element).create()
    }
}

/**
 * [TableGenEvaluationHolder] used when checking a [def][TableGenDefStatement] instantiation: evaluates field
 * expressions in the def's context and anchors any problem on [anchor], a location inside the def being highlighted
 * (the offending expression itself may live in an out-of-file base class).
 */
internal class TableGenInstantiationEvaluationHolder(
    def: TableGenDefStatement,
    private val anchor: PsiElement,
    private val holder: AnnotationHolder,
) : TableGenEvaluationHolder {
    override val context = TableGenEvaluationContext(def)

    override fun error(element: PsiElement, message: String) {
        holder.newAnnotation(HighlightSeverity.ERROR, message).range(anchor).create()
    }
}

/**
 * [TableGenEvaluationHolder] that emits no annotations and merely records whether a check reported a problem, evaluating
 * in the null context.
 *
 * It is used to find out whether a check already fails without any instantiation, i.e. whether the problem is constant
 * and therefore already reported by [TableGenDirectEvaluationHolder]. Running it before re-checking in an instantiation
 * context lets the instantiation pass skip such problems instead of reporting them a second time.
 */
internal class TableGenProbeEvaluationHolder : TableGenEvaluationHolder {
    override val context = TableGenEvaluationContext()

    var emittedError = false
        private set

    override fun error(element: PsiElement, message: String) {
        emittedError = true
    }
}
