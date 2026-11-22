package com.github.zero9178.mlirods.language.psi.impl

import com.github.zero9178.mlirods.language.generated.psi.TableGenBangOperatorDefinition
import com.github.zero9178.mlirods.language.psi.TableGenTypedElement
import com.github.zero9178.mlirods.language.psi.cachedTypeOf
import com.github.zero9178.mlirods.language.types.TableGenType
import com.github.zero9178.mlirods.language.types.typeOfBangOperatorDefinition
import com.github.zero9178.mlirods.model.TableGenCompilationContext
import com.intellij.psi.PsiElement
import com.intellij.util.concurrency.annotations.RequiresBlockingContext
import com.intellij.util.concurrency.annotations.RequiresReadLock

/**
 * Interface used to inject methods into [TableGenBangOperatorDefinition].
 */
interface TableGenBangOperatorDefinitionEx : PsiElement, TableGenTypedElement {

    /**
     * Returns the type of the values the variable takes on, e.g. the element type of the list a '!foreach' iterates
     * over.
     */
    override suspend fun type(context: TableGenCompilationContext): TableGenType = cachedType(context).await()

    @RequiresReadLock
    @RequiresBlockingContext
    override fun typeBlocking(context: TableGenCompilationContext): TableGenType = cachedType(context).getBlocking()

    private fun cachedType(context: TableGenCompilationContext) =
        cachedTypeOf(this as TableGenBangOperatorDefinition, context) { typeOfBangOperatorDefinition(it, context) }
}
