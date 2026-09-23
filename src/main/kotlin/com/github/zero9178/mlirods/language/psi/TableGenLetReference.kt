package com.github.zero9178.mlirods.language.psi

import com.github.zero9178.mlirods.language.generated.psi.TableGenClassStatement
import com.github.zero9178.mlirods.language.generated.psi.TableGenDefStatement
import com.github.zero9178.mlirods.language.generated.psi.TableGenFieldBodyItem
import com.github.zero9178.mlirods.language.generated.psi.TableGenLetBodyItem
import com.github.zero9178.mlirods.model.TableGenCompilationContext
import com.github.zero9178.mlirods.model.getProjectContextDependentCache
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
        fun findField(element: TableGenLetBodyItem, context: TableGenCompilationContext): TableGenFieldBodyItem? =
            getProjectContextDependentCache(element, context) { element ->
                val identifier = element.fieldIdentifier ?: return@getProjectContextDependentCache null
                val parent = element.parentOfTypes(TableGenClassStatement::class, TableGenDefStatement::class)
                    ?: return@getProjectContextDependentCache null
                parent.fields(context)[identifier]
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
