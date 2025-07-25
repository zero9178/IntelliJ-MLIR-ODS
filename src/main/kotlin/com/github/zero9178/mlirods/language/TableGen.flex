// Copyright 2000-2022 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.github.zero9178.mlirods.language.generated;

import com.ibm.icu.impl.UResource;import com.intellij.lexer.FlexLexer;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.TokenType;

%%

%class TableGenLexer
%implements FlexLexer
%unicode
%public
%function advance
%type IElementType
%eof{  return;
%eof}

CRLF=\R
WHITE_SPACE=[\ \t\f]
ESCAPES=("\\n"|"\\\\"|"\\\""|"\\t"|"\\'")
LINE_COMMENT=("//")[^\r\n]*
BLOCK_COMMENT=("/*")!([^]* "*/" [^]*)("*/")
WHITE_SPACE_OR_CC_COMMENT=({WHITE_SPACE}|{BLOCK_COMMENT})

%%
("+")                                           { return TableGenTypes.PLUS; }
("-")                                           { return TableGenTypes.MINUS; }
("[")                                           { return TableGenTypes.LBRACKET; }
("]")                                           { return TableGenTypes.RBRACKET; }
("{")                                           { return TableGenTypes.LBRACE; }
("}")                                           { return TableGenTypes.RBRACE; }
("<")                                           { return TableGenTypes.LANGLE; }
(">")                                           { return TableGenTypes.RANGLE; }
(":")                                           { return TableGenTypes.COLON; }
(";")                                           { return TableGenTypes.SEMICOLON; }
(".")                                           { return TableGenTypes.DOT; }
("...")                                         { return TableGenTypes.ELLIPSE; }
("=")                                           { return TableGenTypes.EQUALS; }
("?")                                           { return TableGenTypes.QUESTION_MARK; }
("#")                                           { return TableGenTypes.HASHTAG; }
(",")                                           { return TableGenTypes.COMMA; }
("(")                                           { return TableGenTypes.LPAREN; }
(")")                                           { return TableGenTypes.RPAREN; }

("def")                                         { return TableGenTypes.DEF; }
("assert")                                      { return TableGenTypes.ASSERT; }
("bit")                                         { return TableGenTypes.BIT; }
("bits")                                        { return TableGenTypes.BITS; }
("class")                                       { return TableGenTypes.CLASS; }
("code")                                        { return TableGenTypes.CODE; }
("dag")                                         { return TableGenTypes.DAG; }
("def")                                         { return TableGenTypes.DEF; }
("dump")                                        { return TableGenTypes.DUMP; }
("else")                                        { return TableGenTypes.ELSE; }
("false")                                       { return TableGenTypes.FALSE; }
("foreach")                                     { return TableGenTypes.FOREACH; }
("defm")                                        { return TableGenTypes.DEFM; }
("defset")                                      { return TableGenTypes.DEFSET; }
("defvar")                                      { return TableGenTypes.DEFVAR; }
("field")                                       { return TableGenTypes.FIELD; }
("if")                                          { return TableGenTypes.IF; }
("in")                                          { return TableGenTypes.IN; }
("include")                                     { return TableGenTypes.INCLUDE; }
("int")                                         { return TableGenTypes.INT; }
("let")                                         { return TableGenTypes.LET; }
("list")                                        { return TableGenTypes.LIST; }
("multiclass")                                  { return TableGenTypes.MULTICLASS; }
("string")                                      { return TableGenTypes.STRING; }
("then")                                        { return TableGenTypes.THEN; }
("true")                                        { return TableGenTypes.TRUE; }
("!cond")                                       { return TableGenTypes.BANG_COND; }
("!foreach")                                    { return TableGenTypes.BANG_FOREACH; }
("!foldl")                                      { return TableGenTypes.BANG_FOLDL; }
("!"[a-zA-Z]+)                                  { return TableGenTypes.BANG_OPERATOR; }
^({WHITE_SPACE_OR_CC_COMMENT}*"#ifdef")         { return TableGenTypes.HASHTAG_IFDEF; }
^({WHITE_SPACE_OR_CC_COMMENT}*"#ifndef")        { return TableGenTypes.HASHTAG_IFNDEF; }
^({WHITE_SPACE_OR_CC_COMMENT}*"#define")        { return TableGenTypes.HASHTAG_DEFINE; }
^({WHITE_SPACE_OR_CC_COMMENT}*"#else")          { return TableGenTypes.HASHTAG_ELSE; }
^({WHITE_SPACE_OR_CC_COMMENT}*"#endif")         { return TableGenTypes.HASHTAG_ENDIF; }

((\+)?[0-9]+)                                   { return TableGenTypes.INTEGER; }
(0x[0-9a-fA-F]+)                                { return TableGenTypes.INTEGER; }
(0b[01]+)                                       { return TableGenTypes.INTEGER; }

([0-9]*[a-zA-Z_][a-zA-Z_0-9]*)                  { return TableGenTypes.IDENTIFIER; }
(\$[a-zA-Z_][a-zA-Z_0-9]*)                      { return TableGenTypes.VAR_IDENTIFIER; }

(("[{")!([^]* "}]" [^]*)("}]"))                 { return TableGenTypes.BLOCK_STRING_LITERAL; }
(("\"")(([^\"\r\n])|{ESCAPES})*("\""))          { return TableGenTypes.LINE_STRING_LITERAL; }
(("\"")(([^\"\r\n])|{ESCAPES})*)                { return TableGenTypes.LINE_STRING_LITERAL_BAD; }

({LINE_COMMENT})                                { return TableGenTypes.LINE_COMMENT; }

// Do not merge the state for newline and other whitespace. This is required to make the greedy longest token search
// select the preprocessor tokens that can only start at the beginning of a line. We use a post-processor for the lexer
// to merge these afterwards.
({CRLF})+                                       { return TokenType.WHITE_SPACE; }
({WHITE_SPACE})+                                { return TokenType.WHITE_SPACE; }

(("/*")!([^]* "*/" [^]*)("*/"))                 { return TableGenTypes.BLOCK_COMMENT; }

[^]                                             { return TableGenTypes.OTHER; }