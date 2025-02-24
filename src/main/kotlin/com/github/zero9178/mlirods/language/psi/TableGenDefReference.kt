package com.github.zero9178.mlirods.language.psi

import com.github.zero9178.mlirods.index.DEF_INDEX
import com.github.zero9178.mlirods.language.generated.psi.TableGenIdentifierValue
import com.github.zero9178.mlirods.language.generated.psi.TableGenScopeItem
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.IndexNotReadyException
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.PsiReferenceBase
import com.intellij.psi.ResolveResult
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.stubs.StubIndex
import com.intellij.psi.util.descendants
import com.intellij.psi.util.parentOfType
import com.intellij.util.concurrency.annotations.RequiresReadLock

/**
 * Perform a traversal visiting all [PsiElement]s before [root] that are within the same scope, before traversing up to
 * the next parent scope item.
 */
private suspend fun SequenceScope<PsiElement>.upwardsDefSearchTraversal(root: PsiElement) {
    var before: PsiElement? = root
    while (before != null) {
        yieldAll(before.descendants {
            // Do not traverse into sub-scopes.
            it !is TableGenIdentifierScopeNode
        })
        before = before.prevSibling
    }
    val nextParent = root.parentOfType<TableGenScopeItem>(withSelf = false) ?: return
    upwardsDefSearchTraversal(nextParent)
}

/**
 * Implements the lookup procedure for 'def's.
 */
class TableGenDefReference(element: TableGenIdentifierValue) : PsiReferenceBase.Poly<TableGenIdentifierValue>(element) {

    @RequiresReadLock
    override fun multiResolve(incompleteCode: Boolean): Array<out ResolveResult> {
        val name = element.identifier.text

        val def = sequence {
            val scopeItem = element.parentOfType<TableGenScopeItem>()

            // Start search at the psi element immediately before this one.
            val prevSibling = scopeItem?.prevSibling
            if (prevSibling != null) {
                upwardsDefSearchTraversal(prevSibling)
                return@sequence
            }

            // If it is the first element in the scope, traverse up to the parent instead.
            val parent = scopeItem?.parentOfType<TableGenScopeItem>() ?: return@sequence
            upwardsDefSearchTraversal(parent)
        }.filterIsInstance<TableGenDefNameIdentifierOwner>().find {
            it.name == name
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
            // TODO: We should only be searching in files included by the contained file.
            GlobalSearchScope.allScope(project),
            TableGenDefNameIdentifierOwner::class.java
        ).map { PsiElementResolveResult(it) }.toTypedArray()
    }
}
