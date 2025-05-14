package com.github.zero9178.mlirods.language

import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet
import org.jetbrains.annotations.NonNls
import com.github.zero9178.mlirods.language.generated.TableGenTypes

class TableGenTokenType(@NonNls debugName: String) : IElementType(debugName, TableGenLanguage.INSTANCE)

class TableGenElementType(@NonNls debugName: String) : IElementType(debugName, TableGenLanguage.INSTANCE)

val BANG_VALUES = TokenSet.create(
    TableGenTypes.BANG_OPERATOR_VALUE,
    TableGenTypes.FOREACH_OPERATOR_VALUE,
    TableGenTypes.FOLDL_OPERATOR_VALUE,
)
val BANG_OPERATORS_TOKENS = TokenSet.create(
    TableGenTypes.BANG_OPERATOR,
    TableGenTypes.BANG_FOREACH,
    TableGenTypes.BANG_FOLDL,
    TableGenTypes.BANG_COND,
)
val VALUES = TokenSet.create(
    TableGenTypes.BANG_OPERATOR_VALUE,
    TableGenTypes.FOREACH_OPERATOR_VALUE,
    TableGenTypes.FOLDL_OPERATOR_VALUE,
    TableGenTypes.UNDEF_VALUE,
    TableGenTypes.STRING_VALUE,
    TableGenTypes.SLICE_ACCESS_VALUE,
    TableGenTypes.LIST_INIT_VALUE,
    TableGenTypes.INTEGER_VALUE,
    TableGenTypes.IDENTIFIER_VALUE,
    TableGenTypes.FIELD_ACCESS_VALUE,
    TableGenTypes.DAG_INIT_VALUE,
    TableGenTypes.CONCAT_VALUE,
    TableGenTypes.BLOCK_STRING_VALUE,
    TableGenTypes.CLASS_INSTANTIATION_VALUE,
)
val VALUES_TOKEN = TokenSet.orSet(
    BANG_OPERATORS_TOKENS, TokenSet.create(
        TableGenTypes.QUESTION_MARK,
        TableGenTypes.LINE_STRING_LITERAL,
        TableGenTypes.LINE_STRING_LITERAL_BAD,
        TableGenTypes.LBRACKET,
        TableGenTypes.LPAREN,
        TableGenTypes.INTEGER,
        TableGenTypes.IDENTIFIER,
        TableGenTypes.BLOCK_STRING_VALUE,
    )
)
val RECORDS = TokenSet.create(
    TableGenTypes.CLASS_STATEMENT,
    TableGenTypes.DEF_STATEMENT,
    TableGenTypes.MULTICLASS_STATEMENT,
)
val COMMENTS = TokenSet.create(TableGenTypes.LINE_COMMENT, TableGenTypes.BLOCK_COMMENT)
val STRING_LITERALS = TokenSet.create(
    TableGenTypes.BLOCK_STRING_LITERAL,
    TableGenTypes.LINE_STRING_LITERAL,
    TableGenTypes.LINE_STRING_LITERAL_BAD
)
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
val PREPROCESSOR_TOKENS = TokenSet.create(
    TableGenTypes.HASHTAG_IFDEF,
    TableGenTypes.HASHTAG_IFNDEF,
    TableGenTypes.HASHTAG_ELSE,
    TableGenTypes.HASHTAG_DEFINE,
    TableGenTypes.HASHTAG_ENDIF,
)