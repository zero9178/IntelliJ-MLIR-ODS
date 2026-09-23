package com.github.zero9178.mlirods.language.annotator

import com.github.zero9178.mlirods.model.TableGenCompilationContext
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.AnnotationSession
import com.intellij.lang.annotation.Annotator
import com.intellij.openapi.util.NotNullLazyKey
import com.intellij.psi.PsiElement

/**
 * Callback signature used in [TableGenAnnotator].
 */
typealias AnnotationCallback = (PsiElement, AnnotationHolder) -> Unit

/**
 * Creates an [AnnotationCallback] that calls [annotation] anytime an element of type [T] is encountered, for checks
 * that do not resolve anything.
 */
inline fun <reified T : PsiElement> addAnnotationFor(crossinline annotation: (T, AnnotationHolder) -> Unit): AnnotationCallback =
    { e, a ->
        if (e is T) annotation(e, a)
    }

/**
 * Creates an [AnnotationCallback] that calls [annotation] anytime an element of type [T] is encountered, passing the
 * compilation context the check resolves in along, see [compilationContext].
 */
inline fun <reified T : PsiElement> addAnnotationFor(
    crossinline annotation: (T, AnnotationHolder, TableGenCompilationContext) -> Unit,
): AnnotationCallback =
    { e, a ->
        if (e is T) annotation(e, a, a.currentAnnotationSession.compilationContext)
    }

private val COMPILATION_CONTEXT_KEY = NotNullLazyKey.createLazyKey<TableGenCompilationContext, AnnotationSession>(
    "TableGenCompilationContext"
) { TableGenCompilationContext.activeFor(it.file) }

/**
 * The context the file of this session is annotated in: the one it derives from the include graph. An annotator is
 * where the IDE enters, so this is decided once per file annotated, rather than once per element, and passed down
 * from there on.
 */
val AnnotationSession.compilationContext: TableGenCompilationContext
    get() = COMPILATION_CONTEXT_KEY.getValue(this)

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
