package com.github.zero9178.mlirods.language.completion

import com.github.zero9178.mlirods.language.generated.TableGenTypes.BINARY_INTEGER
import com.github.zero9178.mlirods.language.generated.TableGenTypes.IDENTIFIER
import com.github.zero9178.mlirods.language.generated.TableGenTypes.INTEGER
import com.intellij.codeInsight.completion.CompletionConfidence
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.tree.TokenSet
import com.intellij.psi.util.elementType
import com.intellij.psi.util.startOffset
import com.intellij.util.ThreeState

/**
 * Tokens that everything typed so far may be part of while it is still undecided whether an integer literal or an
 * identifier is being written. The literal prefixes '0x' and '0b' lex as identifiers until the first digit follows.
 */
private val AMBIGUOUS_TOKENS = TokenSet.create(INTEGER, BINARY_INTEGER, IDENTIFIER)

/**
 * Text that a longer integer literal may still start with.
 */
private val INTEGER_PREFIX = Regex("\\+?[0-9]+|0x[0-9a-fA-F]*|0b[01]*")

/**
 * TableGen identifiers may start with digits, which makes the platform offer completion the moment the first digit of
 * what is going to be an integer literal is typed.
 *
 * Suppress the automatic popup for as long as the text typed so far can still grow into an integer literal. Since no
 * integer literal is a prefix of another literal's syntax, the popup appears again as soon as a character is typed that
 * makes an identifier out of it. Completion requested explicitly is unaffected and still offers the identifiers
 * starting with the digits typed so far.
 */
internal class TableGenCompletionConfidence : CompletionConfidence() {
    override fun shouldSkipAutopopup(
        editor: Editor, contextElement: PsiElement, psiFile: PsiFile, offset: Int
    ): ThreeState {
        if (contextElement.elementType !in AMBIGUOUS_TOKENS) return ThreeState.UNSURE

        // Only the text up to the caret has been typed by the user; anything behind it was already there and does not
        // tell us what is currently being written.
        val typed = contextElement.text.substring(0, offset - contextElement.startOffset)
        return if (INTEGER_PREFIX.matches(typed)) ThreeState.YES else ThreeState.UNSURE
    }
}
