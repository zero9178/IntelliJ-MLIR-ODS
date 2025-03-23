package com.github.zero9178.mlirods.language.psi.impl

import com.github.zero9178.mlirods.language.types.TableGenType
import com.github.zero9178.mlirods.language.types.TableGenUnknownType
import com.intellij.psi.PsiElement

interface TableGenValueEx : PsiElement {
    /**
     * Returns the type of this TableGen expression.
     */
    val type: TableGenType
        get() = TableGenUnknownType
}
