package com.github.zero9178.mlirods.language.psi

import com.github.zero9178.mlirods.index.IDENTIFIER_INDEX
import com.github.zero9178.mlirods.index.getElements
import com.github.zero9178.mlirods.language.completion.createLookupElement
import com.github.zero9178.mlirods.language.generated.psi.*
import com.github.zero9178.mlirods.model.TableGenIncludedSearchScope
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.IndexNotReadyException
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.PsiReferenceBase
import com.intellij.psi.ResolveResult
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.isAncestor
import com.intellij.psi.util.parents
import com.intellij.util.concurrency.annotations.RequiresReadLock
import com.intellij.util.containers.withPrevious

/**
 * Add extra names that are defined by the parent but visible only within its scope.
 * These must act as if defined at the very beginning of that scope (i.e. are found only after any elements within the
 * body).
 */
private fun addExtraDefNamesForParent(parent: TableGenScopeItem, name: TableGenIdentifierValueNode, useIndex: Boolean) =
    sequence<PsiElement> {
        // Lookup is done for fields before template arguments.
        if (useIndex && parent is TableGenFieldScopeNode) parent.fields[name]?.let {
            yield(it)
        }

        if (parent is TableGenAbstractClassStatement) yieldAll(parent.templateArgDeclList.asReversed())
    }

/**
 * Returns all [TableGenIdentifierElement] by performing a backwards traversal starting from [root] and walking up
 * parents whenever the start has been reached.
 */
private fun traverse(
    root: TableGenScopeItem,
    name: TableGenIdentifierValueNode,
    useIndex: Boolean
): Sequence<PsiElement> = sequence {
    yieldAll(root.itemsBefore(withSelf = true))

    root.parentItem?.let {
        yieldAll(addExtraDefNamesForParent(it, name, useIndex))
        yieldAll(traverse(it, name, useIndex))
    }
}

/**
 * Implements the lookup procedure for plain identifier values.
 */
class TableGenIdentifierReference(element: TableGenIdentifierValueNode) :
    PsiReferenceBase.Poly<TableGenIdentifierValueNode>(element) {

    override fun getVariants(): Array<out Any?> {
        return localResolveSequence(false).filterIsInstance<TableGenIdentifierElement>().map {
            createLookupElement(it, element.identifier)
        }.toList().toTypedArray()
    }

    private fun localResolveSequence(useIndex: Boolean = true): Sequence<PsiElement> {
        var hadTemplateArg = false
        val prefix = mutableListOf<TableGenIdentifierElement>()
        val scopeItem = element.parents(withSelf = true).withPrevious().mapNotNull { (it, prev) ->
            when (it) {
                is TableGenTemplateArgDecl -> hadTemplateArg = true
                is TableGenForeachOperatorValueNode ->
                    if (prev == it.body)
                        it.iterator?.let { iterator -> prefix.add(iterator) }

                is TableGenFoldlOperatorValueNode -> {
                    if (prev == it.body) {
                        it.iterator?.let { it1 -> prefix.add(it1) }
                        it.accmulator?.let { it1 -> prefix.add(it1) }
                    }
                }
            }
            it as? TableGenScopeItem
        }.firstOrNull() ?: return emptySequence()

        // Implement different sequences depending on where in the Psi we are.
        var sequence = traverse(scopeItem, element, useIndex)
        when (scopeItem) {
            is TableGenClassStatement ->
                // If coming from the template decl, we do not care to add other template arguments to the search.
                // Otherwise, i.e., coming from the parent class list, we must add any template arguments.
                if (!hadTemplateArg) sequence = addExtraDefNamesForParent(scopeItem, element, useIndex) + sequence

            // Definitions should be skipped.
            is TableGenIdentifierElement -> sequence = sequence.drop(1)
        }
        return prefix.asSequence() + sequence
    }

    @RequiresReadLock
    override fun multiResolve(incompleteCode: Boolean): Array<out ResolveResult> =
        CachedValuesManager.getProjectPsiDependentCache(element) { element ->
            val name = element.identifier.text

            val def = TableGenIdentifierScopeNode.getParentScope(element)?.run {
                idMap[name]?.let { list ->
                    // Find the last element that occurs before 'element'.
                    // We can use binary search due to the lexicographical ordering.
                    var index = list.binarySearch {
                        it.compareTo(element)
                    }
                    // A positive value is an exact match.
                    if (index > 0)
                        return@let list[index]

                    // Otherwise, an inverse insertion point is returned that points to the last element before
                    // 'element'.
                    index = -(index + 1) - 1

                    // Not found cases.
                    if (index >= list.size || index < 0)
                        null
                    // Special case: If the found element is an ancestor of 'element', and directly nested within its
                    // parent scope, then it should be skipped. This avoids cases such as returning a 'defvar i = i;'
                    // statement when resolving the identifier in the initialization.
                    else if (list[index].occurrence.parent == this && list[index].occurrence.isAncestor(element)) {
                        if (index == 0)
                            null
                        else
                            list[index - 1]
                    } else list[index]
                }?.element
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
