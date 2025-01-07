package com.github.zero9178.mlirods.language

import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet
import org.jetbrains.annotations.NonNls
import com.github.zero9178.mlirods.language.generated.TableGenTypes

class TableGenTokenType(@NonNls debugName: String) : IElementType(debugName, TableGenLanguage.INSTANCE)

class TableGenElementType(@NonNls debugName: String) : IElementType(debugName, TableGenLanguage.INSTANCE)

val COMMENTS = TokenSet.create(TableGenTypes.LINE_COMMENT, TableGenTypes.BLOCK_COMMENT)
val STRING_LITERALS = TokenSet.create(TableGenTypes.STRING_LITERAL, TableGenTypes.STRING_LITERAL_BAD)
val KEYWORDS = TokenSet.create(
    TableGenTypes.ASSERT,
    TableGenTypes.BIT,
    TableGenTypes.BITS,
    TableGenTypes.CLASS,
    TableGenTypes.CODE,
    TableGenTypes.DAG,
    TableGenTypes.DEF,
    TableGenTypes.DUMP,
    TableGenTypes.ELSE,
    TableGenTypes.FALSE,
    TableGenTypes.FOREACH,
    TableGenTypes.DEFM,
    TableGenTypes.DEFSET,
    TableGenTypes.DEFVAR,
    TableGenTypes.FIELD,
    TableGenTypes.IF,
    TableGenTypes.IN,
    TableGenTypes.INCLUDE,
    TableGenTypes.INT,
    TableGenTypes.LET,
    TableGenTypes.LIST,
    TableGenTypes.MULTICLASS,
    TableGenTypes.STRING,
    TableGenTypes.THEN,
    TableGenTypes.TRUE,
)
val PUNCTUATION = TokenSet.create(
    TableGenTypes.PLUS,
    TableGenTypes.MINUS,
    TableGenTypes.LBRACKET,
    TableGenTypes.RBRACKET,
    TableGenTypes.LBRACE,
    TableGenTypes.RBRACE,
    TableGenTypes.LANGLE,
    TableGenTypes.RANGLE,
    TableGenTypes.LPAREN,
    TableGenTypes.RPAREN,
    TableGenTypes.COMMA,
    TableGenTypes.COLON,
    TableGenTypes.SEMICOLON,
    TableGenTypes.DOT,
    TableGenTypes.ELLIPSE,
    TableGenTypes.EQUALS,
    TableGenTypes.QUESTION_MARK,
    TableGenTypes.HASHTAG,
)
