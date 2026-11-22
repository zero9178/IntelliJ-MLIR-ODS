package com.github.zero9178.mlirods.language.psi

import com.github.zero9178.mlirods.cache.SuspendingCachedValue
import com.github.zero9178.mlirods.language.generated.psi.TableGenClassStatement
import com.github.zero9178.mlirods.language.generated.psi.TableGenDefStatement
import com.github.zero9178.mlirods.language.generated.psi.TableGenFieldBodyItem
import com.github.zero9178.mlirods.language.generated.psi.TableGenLetBodyItem
import com.github.zero9178.mlirods.model.TableGenCompilationContext
import com.github.zero9178.mlirods.model.projectContextDependentSuspendingCachedValue
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.PsiReferenceBase
import com.intellij.psi.ResolveResult
import com.intellij.psi.util.parentOfTypes
import com.intellij.util.concurrency.annotations.RequiresReadLock

/**
 * Resolution used to find fields referenced by 'let' body items.
 */
class TableGenLetReference(element: TableGenLetBodyItem) : PsiReferenceBase.Poly<TableGenLetBodyItem>(element) {

    companion object {
        /**
         * Returns the field [element] assigns to within [context], searching the record it is part of and the base
         * classes thereof, or `null` if there is none. This is what resolving the reference and every lookup on the
         * PSI itself are built on; results are cached per [element] and [context].
         */
        @RequiresReadLock
        suspend fun findField(element: TableGenLetBodyItem, context: TableGenCompilationContext): TableGenFieldBodyItem? =
            fieldOf(element, context).await()

        /**
         * [findField] for platform entry points. Suspending code must use [findField] instead.
         */
        @RequiresReadLock
        fun findFieldBlocking(element: TableGenLetBodyItem, context: TableGenCompilationContext): TableGenFieldBodyItem? =
            fieldOf(element, context).getBlocking()

        /**
         * The cached value of [findField].
         */
        private fun fieldOf(
            element: TableGenLetBodyItem, context: TableGenCompilationContext
        ): SuspendingCachedValue<TableGenFieldBodyItem?> =
            projectContextDependentSuspendingCachedValue(element, context, "assigned field") { element ->
                val fieldName = element.fieldName ?: return@projectContextDependentSuspendingCachedValue null
                val parent = element.parentOfTypes(TableGenClassStatement::class, TableGenDefStatement::class)
                    ?: return@projectContextDependentSuspendingCachedValue null
                parent.fieldDefinedBefore(fieldName, element, context)
            }
    }

    /**
     * Resolves the reference in the context the platform sees the file in, i.e. the one it derives from the include
     * graph. Anything that already has a context in hand should use [findField] instead.
     */
    @RequiresReadLock
    override fun multiResolve(incompleteCode: Boolean): Array<out ResolveResult> {
        val context = TableGenCompilationContext.activeFor(element)
        return PsiElementResolveResult.createResults(listOfNotNull(findFieldBlocking(element, context)))
    }
}
