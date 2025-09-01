package com.github.zero9178.mlirods.language.psi

import com.intellij.psi.PsiNameIdentifierOwner

/**
 * Common base interface for all kinds of records. This includes both 'def' statements and class statements.
 */
interface TableGenRecord : TableGenFieldScopeNode, PsiNameIdentifierOwner
