package com.github.zero9178.mlirods.language.psi.impl

import com.github.zero9178.mlirods.language.generated.TableGenTypes
import com.github.zero9178.mlirods.language.psi.TableGenFieldIdentifierNode
import com.intellij.psi.PsiElement
import com.intellij.psi.util.elementType

enum class LetMode {
    Prepend, Append,
}

interface TableGenAbstractLetItem : PsiElement, TableGenFieldIdentifierNode {
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
            }.filter { it.elementType == TableGenTypes.IDENTIFIER }.takeWhile { it != identifier }.singleOrNull()?.let {
                if (it.textMatches("prepend") || it.textMatches("append")) it
                else null
            }
        }

    val letMode: LetMode?
        get() = letModeIdentifier?.run {
            if (textMatches("prepend")) LetMode.Prepend
            else LetMode.Append
        }
}