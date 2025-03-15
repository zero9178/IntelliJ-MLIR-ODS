package com.github.zero9178.mlirods.language.psi.impl

import com.github.zero9178.mlirods.language.generated.psi.TableGenClassStatement
import com.intellij.psi.PsiElement

interface TableGenAbstractClassRefEx : PsiElement {
    /**
     * Returns the class being referenced or null if resolution failed.
     */
    val referencedClass: TableGenClassStatement?
}