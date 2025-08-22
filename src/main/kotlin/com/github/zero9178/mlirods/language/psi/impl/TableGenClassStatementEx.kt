package com.github.zero9178.mlirods.language.psi.impl

import com.intellij.psi.PsiElement

interface TableGenClassStatementEx : PsiElement {
    /**
     * Is true if this class is a declaration.
     * A declaration is a class statement that defines no template arguments, parent class list nor has a body.
     * Declarations can be re-redefined once or declared multiple times.
     */
    val isDeclaration: Boolean

    /**
     * Is true if this class has a body.
     */
    val hasBody: Boolean
}