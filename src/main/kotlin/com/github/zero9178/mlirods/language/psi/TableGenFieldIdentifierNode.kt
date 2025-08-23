package com.github.zero9178.mlirods.language.psi

import com.intellij.psi.NavigatablePsiElement
import com.intellij.psi.PsiElement

interface TableGenFieldIdentifierNode : NavigatablePsiElement, PsiElement {
    /**
     * Returns the identifier referring to the name of a field or null if no such identifier exists.
     * The latter may be due to an AST containing an error for example.
     */
    val fieldIdentifier: PsiElement?

    /**
     * Returns the name of the field being referenced.
     * May be implemented only using stub interfaces.
     */
    val fieldName: String?
        get() = fieldIdentifier?.text
}
