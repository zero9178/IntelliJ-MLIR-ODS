package com.github.zero9178.mlirods.language.psi

import com.github.zero9178.mlirods.index.IDENTIFIER_INDEX
import com.github.zero9178.mlirods.index.getElements
import com.github.zero9178.mlirods.language.completion.createLookupElement
import com.github.zero9178.mlirods.language.generated.psi.TableGenIdentifierValueNode
import com.github.zero9178.mlirods.model.TableGenIncludedSearchScope
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.IndexNotReadyException
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.PsiReferenceBase
import com.intellij.psi.ResolveResult
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.isAncestor
import com.intellij.util.concurrency.annotations.RequiresReadLock

/**
 * Implements the lookup procedure for plain identifier values.
 */
class TableGenIdentifierReference(element: TableGenIdentifierValueNode) :
    PsiReferenceBase.Poly<TableGenIdentifierValueNode>(element) {

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
        return if (index !in 0..<size)
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

    override fun getVariants(): Array<out Any?> {
        val parent = TableGenIdentifierScopeNode.getParentScope(element) ?: return emptyArray()
        return parent.idMap.values.mapNotNull {
            it.findBefore(element, scope = parent)?.element
        }.map {
            createLookupElement(it, element.identifier)
        }.toTypedArray()
    }

    @RequiresReadLock
    override fun multiResolve(incompleteCode: Boolean): Array<out ResolveResult> =
        CachedValuesManager.getProjectPsiDependentCache(element) { element ->
            val name = element.identifier.text

            val def = TableGenIdentifierScopeNode.getParentScope(element)?.let {
                it.idMap[name]?.findBefore(element, scope = it)?.element
            }

            // Lookup in the same file succeeded.
            if (def != null) return@getProjectPsiDependentCache arrayOf(PsiElementResolveResult(def))
            val project = element.project
            if (DumbService.isDumb(project)) throw IndexNotReadyException.create()

            // Otherwise, use the index to search in TableGen files included by this file.
            IDENTIFIER_INDEX.getElements(
                name,
                project,
                TableGenIncludedSearchScope(element, project)
            ).map { res -> PsiElementResolveResult(res) }.toTypedArray()
        }
}
