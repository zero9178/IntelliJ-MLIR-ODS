package com.github.zero9178.mlirods.language.stubs

import com.intellij.extapi.psi.StubBasedPsiElementBase
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.util.childrenOfType
import com.intellij.util.AstLoadingFilter

/**
 * Returns a sequence of all child elements of this that are of [elementType].
 * Note that even if the Psi is loaded, only Psi elements that a stub would be created for are returned.
 */
inline fun <reified C : PsiElement, T : StubElement<*>> StubBasedPsiElementBase<T>.stubbedChildren(elementType: TableGenStubElementType<*, C>): Sequence<C> {
    greenStub?.let { stub ->
        return stub.stubbedChildren(elementType)
    }

    return childrenOfType<C>().asSequence().filter {
        elementType.shouldCreateStub(it.node)
    }
}

inline fun <reified C : PsiElement, T : PsiElement> StubElement<T>.stubbedChildren(elementType: TableGenStubElementType<*, C>): Sequence<C> {
    return childrenStubs.asSequence().filter {
        it?.stubType == elementType
    }.mapNotNull {
        it.psi
    }.filterIsInstance<C>()
}

/**
 * Kotlin friendly wrapper around [AstLoadingFilter.disallowTreeLoading].
 */
inline fun <R> disallowTreeLoading(crossinline block: () -> R): R =
    AstLoadingFilter.disallowTreeLoading<R, Throwable> {
        block()
    }
