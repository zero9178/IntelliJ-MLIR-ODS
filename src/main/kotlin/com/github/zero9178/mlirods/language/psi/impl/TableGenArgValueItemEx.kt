package com.github.zero9178.mlirods.language.psi.impl

import com.github.zero9178.mlirods.language.generated.psi.TableGenTemplateArgDecl
import com.github.zero9178.mlirods.language.generated.psi.TableGenValueNode
import com.intellij.psi.NavigatablePsiElement
import com.intellij.psi.PsiElement

/**
 * Interface used to inject methods into [com.github.zero9178.mlirods.language.generated.psi.TableGenArgValueItem].
 */
interface TableGenArgValueItemEx : PsiElement, NavigatablePsiElement {

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

    /**
     * Returns the value that is assigned to a given argument if present.
     */
    val valueNode: TableGenValueNode?

    /**
     * Returns the name that is assigned to a given argument if present.
     * Note that a name may also just be an identifier instead.
     */
    val nameNode: TableGenValueNode?

    /**
     * If this is a named argument with an identifier as name, returns the name from that identifier.
     */
    val identifierName: String?

    /**
     * Returns the [TableGenTemplateArgDecl] that this item assigns a value to if possible.
     */
    val referencedTemplateArgDecl: TableGenTemplateArgDecl?
}
