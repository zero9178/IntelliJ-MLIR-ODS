package com.github.zero9178.mlirods.language

import com.github.zero9178.mlirods.language.generated.TableGenTypes
import com.intellij.codeInsight.editorActions.SimpleTokenSetQuoteHandler

private class TableGenQuoteHandler : SimpleTokenSetQuoteHandler(TableGenTypes.STRING_LITERAL)
