package com.github.zero9178.mlirods.language.psi

import com.github.zero9178.mlirods.cache.SuspendingCachedValue
import com.github.zero9178.mlirods.language.generated.psi.TableGenFieldAccessValueNode
import com.github.zero9178.mlirods.language.generated.psi.TableGenFieldBodyItem
import com.github.zero9178.mlirods.language.types.TableGenRecordType
import com.github.zero9178.mlirods.model.TableGenCompilationContext
import com.github.zero9178.mlirods.model.projectContextDependentSuspendingCachedValue
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.PsiReferenceBase
import com.intellij.psi.ResolveResult
import com.intellij.util.concurrency.annotations.RequiresReadLock


class TableGenFieldAccessReference(element: TableGenFieldAccessValueNode) :
    PsiReferenceBase.Poly<TableGenFieldAccessValueNode>(element) {

    companion object {
        /**
         * Returns the field [element] accesses within [context]: the one of that name in the record the accessed
         * value has as its type, or `null` if the value is not a record or has no such field. This is what resolving
         * the reference and every lookup on the PSI itself are built on; results are cached per [element] and
         * [context].
         */
        @RequiresReadLock
        suspend fun findField(
            element: TableGenFieldAccessValueNode, context: TableGenCompilationContext
        ): TableGenFieldBodyItem? = fieldOf(element, context).await()

        /**
         * [findField] for platform entry points. Suspending code must use [findField] instead.
         */
        @RequiresReadLock
        fun findFieldBlocking(
            element: TableGenFieldAccessValueNode, context: TableGenCompilationContext
        ): TableGenFieldBodyItem? = fieldOf(element, context).getBlocking()

        /**
         * The cached value of [findField].
         */
        private fun fieldOf(
            element: TableGenFieldAccessValueNode, context: TableGenCompilationContext
        ): SuspendingCachedValue<TableGenFieldBodyItem?> =
            projectContextDependentSuspendingCachedValue(element, context, "accessed field") { element ->
                val fieldName = element.fieldName ?: return@projectContextDependentSuspendingCachedValue null
                val type = element.valueNode.type(context) as? TableGenRecordType
                    ?: return@projectContextDependentSuspendingCachedValue null
                type.record(context)?.allFieldsByName(context)?.get(fieldName)
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
