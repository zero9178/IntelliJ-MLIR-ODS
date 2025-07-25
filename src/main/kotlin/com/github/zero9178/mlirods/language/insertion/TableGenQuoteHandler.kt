package com.github.zero9178.mlirods.language.insertion

import com.github.zero9178.mlirods.language.STRING_LITERALS
import com.intellij.codeInsight.editorActions.SimpleTokenSetQuoteHandler

private class TableGenQuoteHandler : SimpleTokenSetQuoteHandler(STRING_LITERALS)
