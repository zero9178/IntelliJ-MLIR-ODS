package com.github.zero9178.mlirods.language.annotator

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
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
 * Base class for creating [Annotator]s that go through a list of annotation callbacks.
 */
abstract class TableGenAnnotator(private val annotations: Iterable<AnnotationCallback>) : Annotator {
    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        annotations.forEach {
            it(element, holder)
        }
    }
}
