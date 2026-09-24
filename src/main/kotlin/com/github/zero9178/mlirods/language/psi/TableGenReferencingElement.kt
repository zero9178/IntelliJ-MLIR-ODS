package com.github.zero9178.mlirods.language.psi

import com.github.zero9178.mlirods.model.TableGenCompilationContext
import com.intellij.psi.PsiElement
import com.intellij.util.concurrency.annotations.RequiresReadLock

/**
 * An element whose reference resolves to the definition of what it names, e.g. the name of a class or the field of a
 * field access.
 */
interface TableGenReferencingElement : PsiElement {
    /**
     * Returns the definition this element refers to within [context] or null if there is none. Implementations narrow
     * the type to the kind of definition they refer to. Only implementations whose lookup suspends override this, all
     * others implement [referencedDefinitionBlocking].
     */
    @RequiresReadLock
    suspend fun referencedDefinition(context: TableGenCompilationContext): PsiElement? =
        referencedDefinitionBlocking(context)

    /**
     * Blocking variant of [referencedDefinition] for platform entry points. Suspending code must use
     * [referencedDefinition] instead.
     */
    @RequiresReadLock
    fun referencedDefinitionBlocking(context: TableGenCompilationContext): PsiElement?
}
