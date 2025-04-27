package com.github.zero9178.mlirods.highlighting

import com.github.zero9178.mlirods.language.generated.TableGenTypes
import com.intellij.lang.BracePair
import com.intellij.lang.PairedBraceMatcher
import com.intellij.psi.PsiFile
import com.intellij.psi.tree.IElementType

private val PAIRS = arrayOf(
    BracePair(TableGenTypes.LBRACE, TableGenTypes.RBRACE, /*structural=*/true),
    BracePair(TableGenTypes.LBRACKET, TableGenTypes.RBRACKET, /*structural=*/true),
    BracePair(TableGenTypes.LANGLE, TableGenTypes.RANGLE, /*structural=*/true),
    BracePair(TableGenTypes.LPAREN, TableGenTypes.RPAREN, /*structural=*/true),
)

private class TableGenBraceMatcher : PairedBraceMatcher {

    override fun getPairs(): Array<BracePair> {
        return PAIRS
    }

    override fun isPairedBracesAllowedBeforeType(lbraceType: IElementType, contextType: IElementType?) = true

    override fun getCodeConstructStart(file: PsiFile?, openingBraceOffset: Int): Int {
        return openingBraceOffset
    }
}