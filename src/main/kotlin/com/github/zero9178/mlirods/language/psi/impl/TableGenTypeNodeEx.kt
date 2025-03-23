package com.github.zero9178.mlirods.language.psi.impl

import com.github.zero9178.mlirods.language.types.TableGenType
import com.intellij.psi.PsiElement

interface TableGenTypeNodeEx : PsiElement {
    /**
     * Returns the value representation of the type AST.
     */
    fun toType(): TableGenType
}
