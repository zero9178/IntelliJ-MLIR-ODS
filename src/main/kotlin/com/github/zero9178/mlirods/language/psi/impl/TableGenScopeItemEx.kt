package com.github.zero9178.mlirods.language.psi.impl

import com.github.zero9178.mlirods.language.generated.psi.TableGenClassStatement
import com.github.zero9178.mlirods.language.generated.psi.TableGenDefStatement
import com.github.zero9178.mlirods.language.generated.psi.TableGenDefvarStatement
import com.github.zero9178.mlirods.language.generated.psi.TableGenScopeItem
import com.github.zero9178.mlirods.language.generated.psi.TableGenStatement
import com.intellij.psi.PsiElement
import com.intellij.psi.util.parentsOfType
import com.intellij.psi.util.siblings

/**
 * Interface used to extend inject methods into [TableGenScopeItem]
 */
interface TableGenScopeItemEx : PsiElement {

    private val statementOrSelf: PsiElement
        get() = when (this) {
            is TableGenDefStatement, is TableGenDefvarStatement,
            is TableGenClassStatement -> {
                // Should be wrapped in a parent [TableGenStatement].
                parent!!
            }

            else -> this
        }

    private fun itemSiblings(forward: Boolean, withSelf: Boolean) =
        statementOrSelf.siblings(forward, withSelf).filterIsInstance<TableGenScopeItem>().map {
            when (it) {
                is TableGenStatement -> {
                    it.defStatement?.run {
                        return@map this
                    }
                    it.defvarStatement?.run {
                        return@map this
                    }
                    it.classStatement?.run {
                        return@map this
                    }
                }
            }
            return@map it
        }

    /**
     * Returns a sequence all items that are defined before this one within the same [com.github.zero9178.mlirods.language.psi.TableGenIdentifierScopeNode].
     * The sequence is in reverse order starting at either this, or its predecessor, and ending in the first item
     * syntactically.
     */
    fun itemsBefore(withSelf: Boolean = false) = itemSiblings(forward = false, withSelf)

    /**
     * Returns a sequence of all scope items that are a parent of this starting from the immediate parent.
     */
    val parentItems: Sequence<TableGenScopeItem>
        get() = parentsOfType<TableGenScopeItem>(withSelf = false).filter {
            if (it !is TableGenStatement) {
                true
            } else {
                // Both of these statements were the previous items.
                // Filter out this pure container statement in that case.
                it.defvarStatement == null && it.defStatement == null && it.classStatement == null
            }
        }

    /**
     * Returns the scope item that is immediate parent of this or null if there is none.
     */
    val parentItem: TableGenScopeItem?
        get() = parentItems.firstOrNull()
}