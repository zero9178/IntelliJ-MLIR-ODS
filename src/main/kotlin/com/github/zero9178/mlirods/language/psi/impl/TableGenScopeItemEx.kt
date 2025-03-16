package com.github.zero9178.mlirods.language.psi.impl

import com.github.zero9178.mlirods.language.generated.psi.TableGenClassStatement
import com.github.zero9178.mlirods.language.generated.psi.TableGenScopeItem
import com.intellij.psi.PsiElement
import com.intellij.psi.util.parentsOfType
import com.intellij.psi.util.siblings

/**
 * Interface used to extend inject methods into [TableGenScopeItem]
 */
interface TableGenScopeItemEx : PsiElement {

    private fun itemSiblings(forward: Boolean, withSelf: Boolean) =
        siblings(forward, withSelf).filterIsInstance<TableGenScopeItem>()

    /**
     * Returns a sequence all items that are defined before this one within the same [com.github.zero9178.mlirods.language.psi.TableGenIdentifierScopeNode].
     * The sequence is in reverse order starting at either this, or its predecessor, and ending in the first item
     * syntactically.
     */
    fun itemsBefore(withSelf: Boolean = false) = itemSiblings(forward = false, withSelf)

    /**
     * Specialization of [itemsBefore] for [TableGenClassStatement]s.
     * May be implemented faster using only stub interfaces.
     */
    fun classStatementsBefore(withSelf: Boolean = false) =
        itemsBefore(withSelf).filterIsInstance<TableGenClassStatement>()

    /**
     * Returns a sequence of all scope items that are a parent of this starting from the immediate parent.
     */
    val parentItems: Sequence<TableGenScopeItem>
        get() = parentsOfType<TableGenScopeItem>(withSelf = false)

    /**
     * Returns the scope item that is immediate parent of this or null if there is none.
     */
    val parentItem: TableGenScopeItem?
        get() = parentItems.firstOrNull()
}