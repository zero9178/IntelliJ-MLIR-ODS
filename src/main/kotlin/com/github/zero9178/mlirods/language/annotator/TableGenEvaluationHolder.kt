package com.github.zero9178.mlirods.language.annotator

import com.github.zero9178.mlirods.language.generated.psi.TableGenDefStatement
import com.github.zero9178.mlirods.language.generated.psi.TableGenValueNode
import com.github.zero9178.mlirods.language.psi.impl.TableGenEvaluationContext
import com.github.zero9178.mlirods.model.TableGenCompilationContext
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.psi.PsiElement
import java.util.concurrent.ConcurrentLinkedQueue

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
     *
     * Checks are coroutines running in parallel, making this get called from any thread and at any time.
     */
    fun error(element: PsiElement, message: String)
}

/**
 * A check that inspects a [TableGenValueNode] and reports problems via [TableGenEvaluationHolder].
 */
class TableGenEvaluationCheck(
    /**
     * Whether [check] has anything to do for a value node at all. Answered without evaluating anything, which is what
     * allows an [Annotator] to only enter suspending code for the few value nodes that need it.
     */
    val appliesTo: (TableGenValueNode) -> Boolean,
    private val check: suspend (TableGenValueNode, TableGenEvaluationHolder) -> Unit,
) {
    suspend operator fun invoke(element: TableGenValueNode, holder: TableGenEvaluationHolder) {
        if (appliesTo(element)) check(element, holder)
    }
}

/**
 * Creates a [TableGenEvaluationCheck] that runs [check] whenever a value node of type [T] that [appliesTo] is
 * encountered.
 */
inline fun <reified T : TableGenValueNode> evaluationCheckFor(
    crossinline appliesTo: (T) -> Boolean = { true },
    crossinline check: suspend (T, TableGenEvaluationHolder) -> Unit,
): TableGenEvaluationCheck = TableGenEvaluationCheck({ it is T && appliesTo(it) }) { element, holder ->
    check(element as T, holder)
}

/**
 * [TableGenEvaluationHolder] collecting the problems reported from whatever thread, to be turned into annotations by
 * [flush] on the one thread an [AnnotationHolder] may be used from.
 */
internal abstract class TableGenCollectingEvaluationHolder(private val holder: AnnotationHolder) :
    TableGenEvaluationHolder {

    private val problems = ConcurrentLinkedQueue<Pair<PsiElement, String>>()

    final override fun error(element: PsiElement, message: String) {
        problems.add(element to message)
    }

    /**
     * Where the annotation for a problem reported on [element] goes.
     */
    protected abstract fun anchorOf(element: PsiElement): PsiElement

    fun flush() {
        while (true) {
            val (element, message) = problems.poll() ?: return
            holder.newAnnotation(HighlightSeverity.ERROR, message).range(anchorOf(element)).create()
        }
    }
}

/**
 * [TableGenEvaluationHolder] used by a regular [Annotator]: evaluates in the null context of [compilationContext] and
 * surfaces problems directly on the offending element.
 */
internal class TableGenDirectEvaluationHolder(
    holder: AnnotationHolder,
    compilationContext: TableGenCompilationContext,
) : TableGenCollectingEvaluationHolder(holder) {
    override val context = TableGenEvaluationContext(compilationContext)

    override fun anchorOf(element: PsiElement) = element
}

/**
 * [TableGenEvaluationHolder] used when checking a [def][TableGenDefStatement] instantiation: evaluates field
 * expressions in the def's context within [compilationContext] and anchors any problem on [anchor], a location inside
 * the def being highlighted (the offending expression itself may live in an out-of-file base class).
 */
internal class TableGenInstantiationEvaluationHolder(
    def: TableGenDefStatement,
    private val anchor: PsiElement,
    holder: AnnotationHolder,
    compilationContext: TableGenCompilationContext,
) : TableGenCollectingEvaluationHolder(holder) {
    override val context = TableGenEvaluationContext(def, compilationContext)

    override fun anchorOf(element: PsiElement) = anchor
}

/**
 * [TableGenEvaluationHolder] that emits no annotations and merely records whether a check reported a problem, evaluating
 * in the null context of [compilationContext].
 *
 * It is used to find out whether a check already fails without any instantiation, i.e. whether the problem is constant
 * and therefore already reported by [TableGenDirectEvaluationHolder]. Running it before re-checking in an instantiation
 * context lets the instantiation pass skip such problems instead of reporting them a second time.
 */
internal class TableGenProbeEvaluationHolder(
    compilationContext: TableGenCompilationContext,
) : TableGenEvaluationHolder {
    override val context = TableGenEvaluationContext(compilationContext)

    @Volatile
    var emittedError = false
        private set

    override fun error(element: PsiElement, message: String) {
        emittedError = true
    }
}
