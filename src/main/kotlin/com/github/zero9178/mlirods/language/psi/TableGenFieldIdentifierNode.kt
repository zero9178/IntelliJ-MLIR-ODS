package com.github.zero9178.mlirods.language.psi

import com.intellij.psi.PsiElement

interface TableGenFieldIdentifierNode : PsiElement {
    val fieldIdentifier: PsiElement
}