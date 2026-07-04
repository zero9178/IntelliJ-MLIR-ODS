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
 * [ProblemGroup] stamped onto every diagnostic produced by a reference annotator. It lets
 * [TableGenHighlightInfoFilter] recognize such diagnostics so that they can be suppressed centrally in a file without a
 * context. Reference annotators tag their annotations via [referenceAnnotation].
 */
internal val REFERENCE_PROBLEM_GROUP = ProblemGroup { "TableGenReference" }

/**
 * Starts a new reference-annotator error annotation tagged with [REFERENCE_PROBLEM_GROUP], so that
 * [TableGenHighlightInfoFilter] can suppress it in a file without a context.
 */
internal fun AnnotationHolder.referenceAnnotation(message: String): AnnotationBuilder =
    newAnnotation(HighlightSeverity.ERROR, message).problemGroup(REFERENCE_PROBLEM_GROUP)

/**
 * Base class for creating [Annotator]s that go through a list of annotation callbacks.
 */
abstract class TableGenAnnotator(private val annotations: Iterable<AnnotationCallback>) : Annotator {
    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        annotations.forEach {
            it(element, holder)
        }
    }
}
