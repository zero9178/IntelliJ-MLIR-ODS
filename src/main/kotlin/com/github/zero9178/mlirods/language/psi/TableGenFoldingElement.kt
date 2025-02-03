package com.github.zero9178.mlirods.language.psi

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.util.endOffset
import com.intellij.psi.util.startOffset

/**
 * Interface used to allow PSI elements to provide folding of contained regions.
 */
interface TableGenFoldingElement : PsiElement {
    /**
     * Returns the string that should be used as the collapsed placeholder.
     * [textRange] corresponds to one of the ranges returned by [getFoldingTextRanges]
     */
    fun getPlaceHolderText(textRange: TextRange): String

    /**
     * Returns a collection of text ranges that should be able to be folded.
     * Defaults to a single range equal to the entire PSI element.
     */
    fun getFoldingTextRanges(): Collection<TextRange> = listOf(this.textRange)
}

private fun textRangeFromPair(left: PsiElement?, right: PsiElement?): TextRange? {
    if (left == null || right == null) return null
    return TextRange(
        left.startOffset,
        right.endOffset
    )
}

/**
 * Specialization of [TableGenFoldingElement] for PSI classes with bodies denoted by braces.
 */
interface TableGenBracedFoldingElement : TableGenFoldingElement {
    val lBrace: PsiElement?

    val rBrace: PsiElement?

    override fun getPlaceHolderText(textRange: TextRange): String = "{...}"

    override fun getFoldingTextRanges(): Collection<TextRange> {
        return listOf(
            textRangeFromPair(
                lBrace,
                rBrace
            ) ?: return emptyList()
        )
    }
}

/**
 * Specialization of [TableGenFoldingElement] for PSI classes with bodies denoted by angle brackets.
 */
interface TableGenAngledFoldingElement : TableGenFoldingElement {
    val lAngle: PsiElement?

    val rAngle: PsiElement?

    override fun getPlaceHolderText(textRange: TextRange): String = "<~>"

    override fun getFoldingTextRanges(): Collection<TextRange> {
        return listOf(
            textRangeFromPair(
                lAngle,
                rAngle
            ) ?: return emptyList()
        )
    }
}