package com.github.zero9178.mlirods.language

import com.github.zero9178.mlirods.language.generated.TableGenLexer
import com.intellij.lexer.FlexAdapter
import com.intellij.lexer.MergingLexerAdapter
import com.intellij.psi.tree.TokenSet

class TableGenLexerAdapter : MergingLexerAdapter(FlexAdapter(TableGenLexer(null)), TokenSet.WHITE_SPACE)