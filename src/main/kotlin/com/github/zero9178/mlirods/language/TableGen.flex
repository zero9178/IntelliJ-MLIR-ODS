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
WHITE_SPACE=[\ \n\t\f]
ESCAPES=("\\n"|"\\\\"|"\\\""|"\\t"|"\\'")

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

("def")                                         { return TableGenTypes.DEF; }

((\+|-)?[0-9]+)                                 { return TableGenTypes.INTEGER; }
(0x[0-9a-fA-F]+)                                { return TableGenTypes.INTEGER; }
(0b[01]+)                                       { return TableGenTypes.INTEGER; }

(("[{")!([^]* "}]" [^]*)("}]"))                 { return TableGenTypes.STRING_LITERAL; }
(("\"")(([^\"\r\n])|{ESCAPES})*("\""))          { return TableGenTypes.STRING_LITERAL; }

(("//")[^\r\n]*)                                { return TableGenTypes.LINE_COMMENT; }

({CRLF}|{WHITE_SPACE})+                         { return TokenType.WHITE_SPACE; }

(("/*")!([^]* "*/" [^]*)("*/"))                 { return TableGenTypes.BLOCK_COMMENT; }

[^]                                             { return TableGenTypes.OTHER; }