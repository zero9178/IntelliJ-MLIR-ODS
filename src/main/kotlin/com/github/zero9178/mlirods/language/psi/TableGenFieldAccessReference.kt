package com.github.zero9178.mlirods.language.psi

import com.github.zero9178.mlirods.language.generated.psi.TableGenFieldAccessValueNode
import com.github.zero9178.mlirods.language.generated.psi.TableGenFieldBodyItem
import com.github.zero9178.mlirods.language.types.TableGenRecordType
import com.github.zero9178.mlirods.model.TableGenCompilationContext
import com.github.zero9178.mlirods.model.getProjectContextDependentCache
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
        fun findField(
            element: TableGenFieldAccessValueNode, context: TableGenCompilationContext
        ): TableGenFieldBodyItem? = getProjectContextDependentCache(element, context) { element ->
            val fieldName = element.fieldName ?: return@getProjectContextDependentCache null
            val type = element.valueNode.typeBlocking(context) as? TableGenRecordType
                ?: return@getProjectContextDependentCache null
            type.record(context)?.fields(context)?.get(fieldName)
        }
    }

    /**
     * Resolves the reference in the context the platform sees the file in, i.e. the one it derives from the include
     * graph. Anything that already has a context in hand should use [findField] instead.
     */
    @RequiresReadLock
    override fun multiResolve(incompleteCode: Boolean): Array<out ResolveResult> {
        val context = TableGenCompilationContext.activeFor(element)
        return PsiElementResolveResult.createResults(listOfNotNull(findField(element, context)))
    }
}
