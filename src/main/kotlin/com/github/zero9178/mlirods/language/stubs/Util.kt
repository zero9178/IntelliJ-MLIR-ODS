package com.github.zero9178.mlirods.language.stubs

import com.intellij.extapi.psi.StubBasedPsiElementBase
import com.intellij.openapi.diagnostic.Logger
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.intellij.util.AstLoadingFilter

private val LOG = Logger.getInstance("#com.github.zero9178.mlirods.language.stubs")

/**
 * Writes [names] so that [readNames] reads them back.
 */
fun StubOutputStream.writeNames(names: List<String>) {
    writeVarInt(names.size)
    names.forEach {
        writeName(it)
    }
}

/**
 * Reads back a list of names written by [writeNames].
 */
fun StubInputStream.readNames(): List<String> {
    val size = readVarInt()
    return buildList {
        repeat(size) { index ->
            val name = readNameString()
            if (name == null) {
                LOG.error("Name $index of $size read back as null from the stub stream")
                return@repeat
            }
            add(name)
        }
    }
}

/**
 * Reads back a single name that was written non-null.
 * [what] is used in an assertion message for debugging.
 */
fun StubInputStream.readRequiredName(what: String): String =
    checkNotNull(readNameString()) { "$what read back as null from the stub stream" }

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
