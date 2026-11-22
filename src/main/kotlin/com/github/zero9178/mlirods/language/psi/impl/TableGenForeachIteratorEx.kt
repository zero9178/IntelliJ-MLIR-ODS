package com.github.zero9178.mlirods.language.psi.impl

import com.github.zero9178.mlirods.language.generated.TableGenTypes
import com.github.zero9178.mlirods.language.generated.psi.TableGenForeachIterator
import com.github.zero9178.mlirods.language.psi.TableGenTypedElement
import com.github.zero9178.mlirods.language.psi.cachedTypeOf
import com.github.zero9178.mlirods.language.stubs.impl.TableGenForeachIteratorStub
import com.github.zero9178.mlirods.language.types.TableGenType
import com.github.zero9178.mlirods.language.types.typeOfForeachIterator
import com.github.zero9178.mlirods.model.TableGenCompilationContext
import com.intellij.psi.PsiElement
import com.intellij.util.concurrency.annotations.RequiresBlockingContext
import com.intellij.util.concurrency.annotations.RequiresReadLock

/**
 * Interface used to inject methods into [com.github.zero9178.mlirods.language.generated.psi.TableGenForeachIterator].
 */
interface TableGenForeachIteratorEx : PsiElement, TableGenTypedElement {

    val stub: TableGenForeachIteratorStub?

    /**
     * Returns whether this iterator iterates over a range list in braces, e.g. `foreach i = {0-3, 5} in`, rather than
     * over a single range or a value.
     */
    val isRangeList: Boolean
        get() = stub?.isRangeList ?: (node.findChildByType(TableGenTypes.LBRACE) != null)

    /**
     * Returns the type of the values the iterator takes on.
     */
    override suspend fun type(context: TableGenCompilationContext): TableGenType = cachedType(context).await()

    @RequiresReadLock
    @RequiresBlockingContext
    override fun typeBlocking(context: TableGenCompilationContext): TableGenType = cachedType(context).getBlocking()

    private fun cachedType(context: TableGenCompilationContext) =
        cachedTypeOf(this as TableGenForeachIterator, context) { typeOfForeachIterator(it, context) }
}
