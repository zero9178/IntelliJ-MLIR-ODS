package com.github.zero9178.mlirods.language.annotator

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.openapi.progress.runBlockingCancellable
import com.intellij.psi.PsiElement
import com.intellij.util.concurrency.annotations.RequiresBlockingContext
import com.intellij.util.concurrency.annotations.RequiresReadLock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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

/**
 * Where an [Annotator] enters suspending code, returning once [checks] and everything it launched is done.
 *
 * Types and values are [com.github.zero9178.mlirods.cache.SuspendingCachedValue]s, which an annotator could request
 * one blocking call at a time. Entering once and requesting them as a coroutine instead is what allows requests that do
 * not depend on each other to be made in parallel. [checks] runs where its coroutines are, with the read access of the
 * highlighting pass that is blocked on it, which a pending write action cancels.
 *
 * An [com.intellij.lang.annotation.AnnotationHolder] must not be used from within [checks]: it belongs to the thread
 * the annotator was called on.
 */
@RequiresReadLock
@RequiresBlockingContext
internal fun <T> runSuspendingChecks(checks: suspend CoroutineScope.() -> T): T = runBlockingCancellable {
    withContext(Dispatchers.Default, checks)
}
