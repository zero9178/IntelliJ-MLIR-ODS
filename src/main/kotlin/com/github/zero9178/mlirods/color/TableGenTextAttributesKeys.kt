package com.github.zero9178.mlirods.color

import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey.createTextAttributesKey

val LINE_COMMENT = createTextAttributesKey("TABLEGEN_LINE_COMMENT", DefaultLanguageHighlighterColors.LINE_COMMENT)
val BLOCK_COMMENT = createTextAttributesKey("TABLEGEN_BLOCK_COMMENT", DefaultLanguageHighlighterColors.BLOCK_COMMENT)
val NUMBER = createTextAttributesKey("TABLEGEN_NUMBER", DefaultLanguageHighlighterColors.NUMBER)
val BRACES = createTextAttributesKey("TABLEGEN_BRACES", DefaultLanguageHighlighterColors.BRACES)
val PARENTHESES = createTextAttributesKey("TABLEGEN_PARENTHESES", DefaultLanguageHighlighterColors.PARENTHESES)
val BRACKETS = createTextAttributesKey("TABLEGEN_BRACKETS", DefaultLanguageHighlighterColors.BRACKETS)
val IDENTIFIER = createTextAttributesKey("TABLEGEN_IDENTIFIER", DefaultLanguageHighlighterColors.IDENTIFIER)
val COMMA = createTextAttributesKey("TABLEGEN_COMMA", DefaultLanguageHighlighterColors.COMMA)
val SEMICOLON = createTextAttributesKey("TABLEGEN_SEMICOLON", DefaultLanguageHighlighterColors.SEMICOLON)
val DOT = createTextAttributesKey("TABLEGEN_DOT", DefaultLanguageHighlighterColors.DOT)
val STRING_ESCAPE =
    createTextAttributesKey("TABLEGEN_STRING_ESCAPE", DefaultLanguageHighlighterColors.VALID_STRING_ESCAPE)
val KEYWORD = createTextAttributesKey("TABLEGEN_KEYWORD", DefaultLanguageHighlighterColors.KEYWORD)
val OPERATION_SIGN = createTextAttributesKey("TABLEGEN_OPERATOR_SIGN", DefaultLanguageHighlighterColors.OPERATION_SIGN)
val BANG_OPERATOR = createTextAttributesKey("TABLEGEN_BANG_OPERATOR", DefaultLanguageHighlighterColors.STATIC_METHOD)
val STRING = createTextAttributesKey("TABLEGEN_STRING", DefaultLanguageHighlighterColors.STRING)

val FIELD = createTextAttributesKey("TABLEGEN_FIELD", DefaultLanguageHighlighterColors.INSTANCE_FIELD)