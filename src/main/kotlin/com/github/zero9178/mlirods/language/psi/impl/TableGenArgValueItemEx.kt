package com.github.zero9178.mlirods.language.psi.impl

import com.intellij.psi.PsiElement

/**
 * Interface used to inject methods into [com.github.zero9178.mlirods.language.generated.psi.TableGenArgValueItem].
 */
interface TableGenArgValueItemEx : PsiElement {

    /**
     * Returns whether this argument is a named argument of the form `name = value` rather than a positional argument.
     */
    val isNamedArgument: Boolean

    /**
     * Returns true if this argument us a positional argument. Note that this is mutually exclusive with
     * [isNamedArgument].
     */
    val isPositionalArgument: Boolean
        get() = !isNamedArgument
}
