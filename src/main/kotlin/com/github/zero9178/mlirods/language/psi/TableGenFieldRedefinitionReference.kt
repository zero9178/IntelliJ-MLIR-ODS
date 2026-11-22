package com.github.zero9178.mlirods.language.psi

import com.github.zero9178.mlirods.cache.SuspendingCachedValue
import com.github.zero9178.mlirods.language.generated.psi.TableGenFieldBodyItem
import com.github.zero9178.mlirods.model.TableGenCompilationContext
import com.github.zero9178.mlirods.model.projectContextDependentSuspendingCachedValue
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.PsiReferenceBase
import com.intellij.psi.ResolveResult
import com.intellij.psi.util.parentOfType
import com.intellij.util.concurrency.annotations.RequiresReadLock

/**
 * Reference of a field body item redefining a field to the field body item that first defined it. Like every other
 * reference to the field, it makes a redefinition stand for the defining field: navigating from, finding the usages of
 * or renaming a redefinition acts on the defining field, whose usages include the redefinitions in turn. A field body
 * item that is itself the defining one refers to nothing.
 */
class TableGenFieldRedefinitionReference(element: TableGenFieldBodyItem) :
    PsiReferenceBase.Poly<TableGenFieldBodyItem>(element) {

    companion object {
        /**
         * Returns the field [element] redefines within [context]: the field body item that first defined the field
         * within the record, in this record before [element] or in a base class, and therefore also determined its
         * type. Returns `null` if [element] is itself the defining field body item. This is what resolving the
         * reference and every lookup on the PSI itself are built on; results are cached per [element] and [context].
         */
        @RequiresReadLock
        suspend fun findRedefinedField(
            element: TableGenFieldBodyItem, context: TableGenCompilationContext,
        ): TableGenFieldBodyItem? = redefinedFieldOf(element, context).await()

        /**
         * [findRedefinedField] for platform entry points. Suspending code must use [findRedefinedField] instead.
         */
        @RequiresReadLock
        fun findRedefinedFieldBlocking(
            element: TableGenFieldBodyItem, context: TableGenCompilationContext,
        ): TableGenFieldBodyItem? = redefinedFieldOf(element, context).getBlocking()

        /**
         * The cached value of [findRedefinedField].
         */
        private fun redefinedFieldOf(
            element: TableGenFieldBodyItem, context: TableGenCompilationContext,
        ): SuspendingCachedValue<TableGenFieldBodyItem?> =
            projectContextDependentSuspendingCachedValue(element, context, "redefined field") { element ->
                val fieldName = element.fieldName ?: return@projectContextDependentSuspendingCachedValue null
                val scope = element.parentOfType<TableGenFieldScopeNode>()
                    ?: return@projectContextDependentSuspendingCachedValue null
                scope.fieldDefinedBefore(fieldName, element, context)
            }
    }

    /**
     * Resolves the reference in the context the platform sees the file in, i.e. the one it derives from the include
     * graph. Anything that already has a context in hand should use [findRedefinedField] instead.
     */
    @RequiresReadLock
    override fun multiResolve(incompleteCode: Boolean): Array<out ResolveResult> {
        val context = TableGenCompilationContext.activeFor(element)
        return PsiElementResolveResult.createResults(listOfNotNull(findRedefinedFieldBlocking(element, context)))
    }
}
