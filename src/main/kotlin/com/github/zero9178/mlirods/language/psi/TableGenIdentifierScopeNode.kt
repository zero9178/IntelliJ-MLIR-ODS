package com.github.zero9178.mlirods.language.psi

import com.intellij.psi.PsiElement

/**
 * Interface implemented by any [PsiElement] which creates a scope of elements found by 'def' lookup.
 */
interface TableGenIdentifierScopeNode : PsiElement
