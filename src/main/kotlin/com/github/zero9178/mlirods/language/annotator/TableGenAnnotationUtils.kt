package com.github.zero9178.mlirods.language.annotator

import com.intellij.lang.annotation.AnnotationBuilder
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.lang.annotation.ProblemGroup
import com.intellij.psi.PsiElement

/**
 * Callback signature used in [TableGenAnnotator].
 */
typealias AnnotationCallback = (PsiElement, AnnotationHolder) -> Unit

/**
 * Creates an [AnnotationCallback] that calls [annotation] anytime an element of type [T] is encountered.
 */
inline fun <reified T : PsiElement> addAnnotationFor(crossinline annotation: (T, AnnotationHolder) -> Unit): AnnotationCallback =
    { e, a ->
        if (e is T) annotation(e, a)
    }

/**
 * [ProblemGroup] stamped onto every annotation produced by a reference [TableGenAnnotator]. It lets
 * [TableGenHighlightInfoFilter] recognize diagnostics that originate from a reference annotator so that they can be
 * suppressed centrally without the annotators knowing about it.
 */
internal val REFERENCE_PROBLEM_GROUP = ProblemGroup { "TableGenReference" }

/**
 * [AnnotationHolder] that stamps [problemGroup] onto every annotation created through it; all other behavior is
 * delegated unchanged.
 */
private class ProblemGroupAnnotationHolder(
    private val delegate: AnnotationHolder, private val problemGroup: ProblemGroup
) : AnnotationHolder by delegate {
    override fun newAnnotation(severity: HighlightSeverity, message: String): AnnotationBuilder =
        delegate.newAnnotation(severity, message).problemGroup(problemGroup)
}

/**
 * Base class for creating [Annotator]s that go through a list of annotation callbacks.
 */
abstract class TableGenAnnotator(
    private val annotations: Iterable<AnnotationCallback>,
    /**
     * When non-null, every annotation created by this annotator is tagged with this [ProblemGroup]. This is how a
     * reference annotator marks its diagnostics for [TableGenHighlightInfoFilter].
     */
    private val problemGroup: ProblemGroup? = null
) : Annotator {
    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        val effectiveHolder = if (problemGroup != null) ProblemGroupAnnotationHolder(holder, problemGroup) else holder
        annotations.forEach {
            it(element, effectiveHolder)
        }
    }
}
