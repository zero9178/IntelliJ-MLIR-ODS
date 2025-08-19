package com.github.zero9178.mlirods.language.stubs

import com.intellij.extapi.psi.StubBasedPsiElementBase
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.StubElement
import com.intellij.util.AstLoadingFilter

inline fun <reified C, T> Sequence<T>.filterIsInstance(vararg klasses: Class<out C>) = mapNotNull { c ->
    if (klasses.any { it.isInstance(c) })
        c as C
    else null
}

/**
 * Returns a sequence of all child elements of this that are of type [C].
 */
inline fun <reified C : PsiElement> StubBasedPsiElementBase<*>.stubbedChildren() = stubbedChildren(C::class.java)

/**
 * Returns a sequence of all child elements of this that are one of [klasses].
 */
inline fun <reified C : PsiElement> StubBasedPsiElementBase<*>.stubbedChildren(vararg klasses: Class<out C>): Sequence<C> {
    stub?.let { stub ->
        return stub.stubbedChildren(*klasses)
    }

    return children.asSequence().filterIsInstance(*klasses)
}

inline fun <reified C : PsiElement> StubElement<*>.stubbedChildren() = stubbedChildren(C::class.java)

inline fun <reified C : PsiElement> StubElement<*>.stubbedChildren(vararg klasses: Class<out C>): Sequence<C> {
    return childrenStubs.asSequence().mapNotNull {
        it.psi
    }.filterIsInstance(*klasses)
}

/**
 * Kotlin friendly wrapper around [AstLoadingFilter.disallowTreeLoading].
 */
inline fun <R> disallowTreeLoading(crossinline block: () -> R): R =
    AstLoadingFilter.disallowTreeLoading<R, Throwable> {
        block()
    }
