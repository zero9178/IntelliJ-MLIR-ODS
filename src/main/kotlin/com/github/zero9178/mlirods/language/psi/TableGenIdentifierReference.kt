package com.github.zero9178.mlirods.language.psi

import com.github.zero9178.mlirods.index.IDENTIFIER_INDEX
import com.github.zero9178.mlirods.index.getVisibleElements
import com.github.zero9178.mlirods.language.generated.psi.TableGenIdentifierValueNode
import com.github.zero9178.mlirods.model.TableGenCompilationContext
import com.github.zero9178.mlirods.cache.SuspendingCachedValue
import com.github.zero9178.mlirods.model.projectContextDependentSuspendingCachedValue
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.IndexNotReadyException
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.PsiReferenceBase
import com.intellij.psi.ResolveResult
import com.intellij.psi.util.isAncestor
import com.intellij.util.concurrency.annotations.RequiresReadLock

/**
 * Implements the lookup procedure for plain identifier values.
 */
class TableGenIdentifierReference(element: TableGenIdentifierValueNode) :
    PsiReferenceBase.Poly<TableGenIdentifierValueNode>(element) {

    companion object {
        private fun List<TableGenIdentifierScopeNode.IdMapEntry>.findBefore(
            element: TableGenIdentifierValueNode,
            scope: TableGenIdentifierScopeNode
        ): TableGenIdentifierScopeNode.IdMapEntry? {
            // Find the last element that occurs before 'element'.
            // We can use binary search due to the lexicographical ordering.
            var index = binarySearch {
                it.compareTo(element)
            }
            // A positive value is an exact match.
            if (index > 0)
                return this[index]

            // Otherwise, an inverse insertion point is returned that points to the last element before
            // 'element'.
            index = -(index + 1) - 1

            // Not found cases.
            return if (index !in indices)
                null
            // Special case: If the found element is an ancestor of 'element', and directly nested within its
            // parent scope, then it should be skipped. This avoids cases such as returning a 'defvar i = i;'
            // statement when resolving the identifier in the initialization.
            else if (this[index].occurrence.parent == scope && this[index].occurrence.isAncestor(element)) {
                if (index == 0)
                    null
                else
                    this[index - 1]
            } else this[index]
        }

        /**
         * Returns the declarations of the scopes enclosing [element] that a reference at [element] may refer to
         * within [context]: for every name declared before [element], the declaration closest to it. They take
         * precedence over whatever the index knows under the same name, which a reference only consults if none of
         * them matches.
         */
        @RequiresReadLock
        suspend fun findLocalElements(
            element: TableGenIdentifierValueNode, context: TableGenCompilationContext
        ): List<TableGenIdentifierElement> {
            val parent = TableGenIdentifierScopeNode.getParentScope(element) ?: return emptyList()
            return parent.idMap(context).values.mapNotNull {
                it.findBefore(element, scope = parent)?.element
            }
        }

        /**
         * The cached value of [findVisibleDeclarations].
         */
        private fun visibleDeclarationsOf(
            element: TableGenIdentifierValueNode, context: TableGenCompilationContext
        ): SuspendingCachedValue<List<TableGenIdentifierElement>> =
            projectContextDependentSuspendingCachedValue(element, context, "visible declarations") { element ->
                val name = element.identifier.text

                val def = TableGenIdentifierScopeNode.getParentScope(element)?.let {
                    it.idMapEntries(name, context)?.findBefore(element, scope = it)?.element
                }

                // Lookup in the same file succeeded.
                if (def != null) return@projectContextDependentSuspendingCachedValue listOf(def)
                val project = element.project
                if (DumbService.isDumb(project)) throw IndexNotReadyException.create()

                // Otherwise, use the index to search for the global identifiers preceding the reference, be it in
                // this file or in another one.
                IDENTIFIER_INDEX.getVisibleElements(name, context.at(element))
            }

        /**
         * Returns all declarations [element] may refer to within [context]: the closest one preceding it within its
         * own file's scopes if there is one, and otherwise every global one visible from it once every 'include'
         * directive is pasted in. This is what resolving the reference and every lookup on the PSI itself are built
         * on; results are cached per [element] and [context].
         */
        @RequiresReadLock
        suspend fun findVisibleDeclarations(
            element: TableGenIdentifierValueNode, context: TableGenCompilationContext
        ): List<TableGenIdentifierElement> = visibleDeclarationsOf(element, context).await()

        /**
         * [findVisibleDeclarations] for platform entry points. Suspending code must use [findVisibleDeclarations]
         * instead.
         */
        @RequiresReadLock
        fun findVisibleDeclarationsBlocking(
            element: TableGenIdentifierValueNode, context: TableGenCompilationContext
        ): List<TableGenIdentifierElement> = visibleDeclarationsOf(element, context).getBlocking()
    }

    /**
     * Resolves the reference in the context the platform sees the file in, i.e. the one it derives from the include
     * graph. Anything that already has a context in hand should use [findVisibleDeclarations] instead.
     */
    @RequiresReadLock
    override fun multiResolve(incompleteCode: Boolean): Array<out ResolveResult> {
        val context = TableGenCompilationContext.activeFor(element)
        return PsiElementResolveResult.createResults(findVisibleDeclarationsBlocking(element, context))
    }
}
