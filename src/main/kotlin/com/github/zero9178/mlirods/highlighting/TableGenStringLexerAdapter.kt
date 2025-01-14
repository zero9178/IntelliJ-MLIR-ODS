package com.github.zero9178.mlirods.highlighting

import com.github.zero9178.mlirods.highlighting.generated.TableGenStringLexer
import com.intellij.lexer.FlexAdapter
import com.intellij.psi.tree.IElementType

class TableGenStringLexerAdapter(tokenType: IElementType) :
    FlexAdapter(TableGenStringLexer(null, tokenType))