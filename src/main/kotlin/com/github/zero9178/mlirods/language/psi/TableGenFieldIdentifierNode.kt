package com.github.zero9178.mlirods.language.psi

import com.intellij.psi.PsiElement

interface TableGenFieldIdentifierNode : PsiElement {
    /**
     * Returns the identifier referring to the name of a field or null if no such identifier exists.
     * The latter may be due to an AST containing an error for example.
     */
    val fieldIdentifier: PsiElement?
}
