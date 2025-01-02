package com.github.zero9178.mlirods.language

import com.github.zero9178.mlirods.language.generated.TableGenLexer
import com.intellij.lexer.FlexAdapter

class TableGenLexerAdapter : FlexAdapter(TableGenLexer(null))