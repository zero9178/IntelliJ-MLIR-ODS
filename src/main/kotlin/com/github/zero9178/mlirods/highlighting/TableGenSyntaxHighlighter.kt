package com.github.zero9178.mlirods.highlighting

import com.github.zero9178.mlirods.color.BANG_OPERATOR
import com.github.zero9178.mlirods.color.BLOCK_COMMENT
import com.github.zero9178.mlirods.color.BRACES
import com.github.zero9178.mlirods.color.BRACKETS
import com.github.zero9178.mlirods.color.COMMA
import com.github.zero9178.mlirods.color.DOT
import com.github.zero9178.mlirods.color.IDENTIFIER
import com.github.zero9178.mlirods.color.KEYWORD
import com.github.zero9178.mlirods.color.LINE_COMMENT
import com.github.zero9178.mlirods.color.NUMBER
import com.github.zero9178.mlirods.color.OPERATION_SIGN
import com.github.zero9178.mlirods.color.PARENTHESES
import com.github.zero9178.mlirods.color.SEMICOLON
import com.github.zero9178.mlirods.color.STRING
import com.github.zero9178.mlirods.color.STRING_ESCAPE
import com.github.zero9178.mlirods.language.KEYWORDS
import com.github.zero9178.mlirods.language.PUNCTUATION
import com.github.zero9178.mlirods.language.STRING_LITERALS
import com.github.zero9178.mlirods.language.generated.TableGenTypes
import com.intellij.lexer.Lexer
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase
import com.intellij.psi.StringEscapesTokenTypes
import com.intellij.psi.tree.IElementType

internal class TableGenSyntaxHighlighter : SyntaxHighlighterBase() {

    /**
     * Returns the lexer used for highlighting the file. The lexer is invoked incrementally when the file is changed, so it must be
     * capable of saving/restoring state and resuming lexing from the middle of the file.
     *
     * @return The lexer implementation.
     */
    override fun getHighlightingLexer(): Lexer {
        return TableGenHighlighterLexer()
    }

    /**
     * Returns the list of text attribute keys used for highlighting the specified token type. The attributes of all attribute keys
     * returned for the token type are successively merged to obtain the color and attributes of the token.
     *
     * @param tokenType The token type for which the highlighting is requested.
     * @return The array of text attribute keys.
     */
    override fun getTokenHighlights(tokenType: IElementType?): Array<TextAttributesKey> = when (tokenType) {
        TableGenTypes.INTEGER -> arrayOf(NUMBER)
        TableGenTypes.BLOCK_COMMENT -> arrayOf(BLOCK_COMMENT)
        TableGenTypes.LINE_COMMENT -> arrayOf(LINE_COMMENT)
        TableGenTypes.LBRACE, TableGenTypes.RBRACE -> arrayOf(BRACES)
        TableGenTypes.LPAREN, TableGenTypes.RPAREN -> arrayOf(PARENTHESES)
        TableGenTypes.LBRACKET, TableGenTypes.RBRACKET -> arrayOf(BRACKETS)
        TableGenTypes.IDENTIFIER -> arrayOf(IDENTIFIER)
        TableGenTypes.COMMA -> arrayOf(COMMA)
        TableGenTypes.SEMICOLON -> arrayOf(SEMICOLON)
        TableGenTypes.DOT -> arrayOf(DOT)
        TableGenTypes.BANG_OPERATOR, TableGenTypes.BANG_COND -> arrayOf(BANG_OPERATOR)
        StringEscapesTokenTypes.VALID_STRING_ESCAPE_TOKEN -> arrayOf(STRING_ESCAPE)
        else -> when {
            KEYWORDS.contains(tokenType) -> arrayOf(KEYWORD)
            PUNCTUATION.contains(tokenType) -> arrayOf(OPERATION_SIGN)
            STRING_LITERALS.contains(tokenType) -> arrayOf(STRING)
            else -> emptyArray()
        }
    }
}
