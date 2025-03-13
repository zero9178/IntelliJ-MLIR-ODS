package com.github.zero9178.mlirods.language.psi

import com.github.zero9178.mlirods.index.DEF_INDEX
import com.github.zero9178.mlirods.language.generated.psi.*
import com.github.zero9178.mlirods.model.TableGenIncludedSearchScope
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.IndexNotReadyException
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.PsiReferenceBase
import com.intellij.psi.ResolveResult
import com.intellij.psi.stubs.StubIndex
import com.intellij.psi.util.parents
import com.intellij.util.concurrency.annotations.RequiresReadLock

/**
 * Add extra names that are defined by the parent but visible only within its scope.
 * These must act as if defined at the very beginning of that scope (i.e. are found only after any elements within the
 * body).
 */
private fun addExtraDefNamesForParent(parent: TableGenScopeItem, name: String) = sequence<PsiElement> {
    // Lookup is done for fields before template arguments.
    if (parent is TableGenFieldScopeNode)
        parent.fields[name]?.let {
            yield(it)
        }

    if (parent is TableGenAbstractClassStatement)
        yieldAll(parent.templateArgDeclList.asReversed())
}

/**
 * Returns all [TableGenDefNameIdentifierOwner] by performing a backwards traversal starting from [root] and walking up
 * parents whenever the start has been reached.
 */
private fun traverse(root: TableGenScopeItem, name: String): Sequence<PsiElement> = sequence {
    yieldAll(root.itemsBefore(withSelf = true))

    root.parentItem?.let {
        yieldAll(addExtraDefNamesForParent(it, name))
        yieldAll(traverse(it, name))
    }
}

/**
 * Implements the lookup procedure for 'def's.
 */
class TableGenDefReference(element: TableGenIdentifierValue) : PsiReferenceBase.Poly<TableGenIdentifierValue>(element) {

    @RequiresReadLock
    override fun multiResolve(incompleteCode: Boolean): Array<out ResolveResult> {
        val name = element.identifier.text

        val def = run {
            var hadTemplateArg = false
            val scopeItem = element.parents(withSelf = false).find {
                when (it) {
                    is TableGenTemplateArgDecl -> hadTemplateArg = true
                }
                it is TableGenScopeItem
            } as? TableGenScopeItem ?: return@run emptySequence()

            // Implement different sequences depending on where in the Psi we are.
            var sequence = traverse(scopeItem, name)
            when (scopeItem) {
                is TableGenClassStatement ->
                    // If coming from the template decl, we do not care to add other template arguments to the search.
                    // Otherwise, i.e., coming from the parent class list, we must add any template arguments.
                    if (!hadTemplateArg) sequence = addExtraDefNamesForParent(scopeItem, name) + sequence

                // Definitions should be skipped.
                is TableGenDefNameIdentifierOwner -> sequence = sequence.drop(1)
            }
            sequence
        }.firstNotNullOfOrNull {
            if (it is TableGenDefNameIdentifierOwner || it is TableGenFieldBodyItem)
                if (it.name == name)
                    return@firstNotNullOfOrNull it

            null
        }

        // Lookup in the same file succeeded.
        if (def != null) return arrayOf(PsiElementResolveResult(def))

        val project = element.project
        if (DumbService.isDumb(project)) throw IndexNotReadyException.create()

        // Otherwise, use the index to search in TableGen files included by this file.
        return StubIndex.getElements(
            DEF_INDEX,
            name,
            project,
            TableGenIncludedSearchScope(element, project),
            TableGenDefNameIdentifierOwner::class.java
        ).map { PsiElementResolveResult(it) }.toTypedArray()
    }
}
