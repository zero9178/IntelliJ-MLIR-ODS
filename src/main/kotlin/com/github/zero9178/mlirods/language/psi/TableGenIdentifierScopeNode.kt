package com.github.zero9178.mlirods.language.psi

import com.intellij.psi.PsiElement
import com.intellij.psi.util.descendants

/**
 * Interface implemented by any [PsiElement] which creates a scope of elements found by 'def' lookup.
 */
interface TableGenIdentifierScopeNode : PsiElement {

    /**
     * Returns a sequence of all [TableGenDefNameIdentifierOwner] that are contained within this scope.
     */
    val defs
        get() = descendants {
            it === this || it !is TableGenIdentifierScopeNode
        }.drop(1).filterIsInstance<TableGenDefNameIdentifierOwner>()
}
