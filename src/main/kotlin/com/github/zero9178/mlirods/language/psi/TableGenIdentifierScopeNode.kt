package com.github.zero9178.mlirods.language.psi

import com.intellij.psi.PsiElement

/**
 * Interface implemented by any [PsiElement] which creates a scope of elements found by 'def' lookup.
 */
interface TableGenIdentifierScopeNode : PsiElement {
    /**
     * Returns a map containing all elements that can be found by def lookup directly nested within this scope.
     * Elements with the same name
     */
    val defMap: Map<String, List<TableGenIdentifierElement>>
        get() = TODO()
}
