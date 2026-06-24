package com.github.zero9178.mlirods.language.psi.impl

import com.github.zero9178.mlirods.language.generated.TableGenTypes
import com.github.zero9178.mlirods.language.generated.psi.TableGenValueNode
import com.github.zero9178.mlirods.language.psi.TableGenFieldAssignmentNode
import com.intellij.psi.PsiElement
import com.intellij.psi.util.elementType

enum class LetMode {
    Prepend, Append,
}

interface TableGenAbstractLetItem : PsiElement, TableGenFieldAssignmentNode {
    /**
     * Returns the value node assigned by this let item.
     */
    val valueNode: TableGenValueNode?

    override val assignedValueNode: TableGenValueNode?
        get() = valueNode
    /**
     * Returns the identifier representing the 'let'-mode if present.
     * Note that it does not verify whether the identifier is one of "prepend" or "append".
     */
    val letModeIdentifier: PsiElement?
        get() {
            val identifier = fieldIdentifier
            var child = firstChild
            return generateSequence {
                if (child == null) null
                else {
                    val previous = child
                    child = previous.nextSibling
                    previous
                }
            }.filter { it.elementType == TableGenTypes.IDENTIFIER }.takeWhile { it != identifier }.singleOrNull()
        }

    /**
     * Returns the current let-mode or null if present and not unknown.
     */
    val letMode: LetMode?
        get() = letModeIdentifier?.run {
            if (textMatches("prepend")) LetMode.Prepend
            else if (textMatches("append")) LetMode.Append
            else null
        }
}