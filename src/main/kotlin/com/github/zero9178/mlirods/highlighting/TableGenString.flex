package com.github.zero9178.mlirods.highlighting.generated;

import com.intellij.lexer.FlexLexer;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.StringEscapesTokenTypes;

%%

// Lexer use for syntax highlighting escapes in string literals.
%class TableGenStringLexer
%implements FlexLexer
%unicode
%public
%function advance
%type IElementType
%eof{  return;
%eof}

%{
  private final IElementType tokenType;
%}

%ctorarg IElementType tokenType

%init{
  this.tokenType = tokenType;
%init}

ESCAPES=("\\n"|"\\\\"|"\\\""|"\\t"|"\\'")

%%

({ESCAPES})                      { return StringEscapesTokenTypes.VALID_STRING_ESCAPE_TOKEN; }

[^]                              { return tokenType; }