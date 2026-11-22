package com.github.zero9178.mlirods.language.psi

import com.github.zero9178.mlirods.cache.SuspendingCachedValue
import com.github.zero9178.mlirods.cache.SuspendingCachedValueScope
import com.github.zero9178.mlirods.language.types.TableGenType
import com.github.zero9178.mlirods.language.types.TableGenUnknownType
import com.github.zero9178.mlirods.model.TableGenCompilationContext
import com.github.zero9178.mlirods.model.projectContextDependentSuspendingCachedValue
import com.intellij.psi.PsiElement
import com.intellij.util.concurrency.annotations.RequiresBlockingContext
import com.intellij.util.concurrency.annotations.RequiresReadLock

/**
 * An element denoting values of a [TableGenType]: an expression, or a named definition such as a 'defvar' or a
 * 'foreach' iterator, whose type is that of the values an identifier referring to it denotes.
 */
interface TableGenTypedElement : PsiElement {
    /**
     * Returns the type of the values this element denotes, resolving whatever it refers to within [context]. A type
     * that depends on itself is unknown.
     *
     * This is for code that is a coroutine, with [typeBlocking] for everything else.
     */
    suspend fun type(context: TableGenCompilationContext): TableGenType

    /**
     * [type] for callers that cannot suspend. See [SuspendingCachedValue.getBlocking] for what is required of the read
     * action this is called in.
     */
    @RequiresReadLock
    @RequiresBlockingContext
    fun typeBlocking(context: TableGenCompilationContext): TableGenType
}

/**
 * The type of [element] in [context] as computed by [compute], cached for implementations of
 * [TableGenTypedElement.type] whose type depends on other elements. Values are cached per class of [compute], making
 * it necessary for [TableGenTypedElement.type] and [TableGenTypedElement.typeBlocking] to share a single call site.
 */
internal fun <E : TableGenTypedElement> cachedTypeOf(
    element: E,
    context: TableGenCompilationContext,
    compute: suspend SuspendingCachedValueScope.(E) -> TableGenType,
): SuspendingCachedValue<TableGenType> =
    projectContextDependentSuspendingCachedValue(element, context, "type", onCycle = { TableGenUnknownType }, compute)
