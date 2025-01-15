package com.github.zero9178.mlirods.highlighting

import com.github.zero9178.mlirods.highlighting.generated.TableGenStringLexer
import com.intellij.lexer.FlexAdapter
import com.intellij.lexer.MergingLexerAdapter
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet

class TableGenStringLexerAdapter(tokenType: IElementType) :
    MergingLexerAdapter(FlexAdapter(TableGenStringLexer(null, tokenType)), TokenSet.create(tokenType)) {

}