package com.github.zero9178.mlirods.language.psi

import com.intellij.grazie.utils.parents
import com.intellij.psi.PsiElement
import com.intellij.psi.util.childLeafs
import com.jetbrains.rd.util.forEachReversed

/**
 * Interface implemented by any [PsiElement] which creates a scope of elements found by 'def' lookup.
 */
interface TableGenIdentifierScopeNode : PsiElement {

    /**
     * Returns a sequence of all [TableGenDefNameIdentifierOwner] that is an immediate child of this element in reverse
     * order.
     */
    val defs
        get() = sequence {
            children.forEachReversed {
                yield(it)
            }
        }.filterIsInstance<TableGenDefNameIdentifierOwner>()
}
