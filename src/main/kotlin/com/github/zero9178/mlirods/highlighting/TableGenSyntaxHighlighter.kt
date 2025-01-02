package com.github.zero9178.mlirods.highlighting

import com.github.zero9178.mlirods.language.TableGenLexerAdapter
import com.github.zero9178.mlirods.language.generated.TableGenTypes
import com.intellij.lexer.Lexer
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase
import com.intellij.psi.tree.IElementType

internal class TableGenSyntaxHighlighter : SyntaxHighlighterBase() {

    /**
     * Returns the lexer used for highlighting the file. The lexer is invoked incrementally when the file is changed, so it must be
     * capable of saving/restoring state and resuming lexing from the middle of the file.
     *
     * @return The lexer implementation.
     */
    override fun getHighlightingLexer(): Lexer {
        return TableGenLexerAdapter()
    }

    /**
     * Returns the list of text attribute keys used for highlighting the specified token type. The attributes of all attribute keys
     * returned for the token type are successively merged to obtain the color and attributes of the token.
     *
     * @param tokenType The token type for which the highlighting is requested.
     * @return The array of text attribute keys.
     */
    override fun getTokenHighlights(tokenType: IElementType?): Array<TextAttributesKey> = when (tokenType) {
        TableGenTypes.STRING_LITERAL -> arrayOf(DefaultLanguageHighlighterColors.STRING)
        TableGenTypes.INTEGER -> arrayOf(DefaultLanguageHighlighterColors.NUMBER)
        TableGenTypes.BLOCK_COMMENT -> arrayOf(DefaultLanguageHighlighterColors.BLOCK_COMMENT)
        TableGenTypes.LINE_COMMENT -> arrayOf(DefaultLanguageHighlighterColors.LINE_COMMENT)
        TableGenTypes.LBRACE, TableGenTypes.RBRACE -> arrayOf(DefaultLanguageHighlighterColors.BRACES)
        TableGenTypes.DEF -> arrayOf(DefaultLanguageHighlighterColors.KEYWORD)
        else -> emptyArray()
    }
}
