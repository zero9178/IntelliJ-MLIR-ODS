package com.github.zero9178.mlirods.highlighting

import com.github.zero9178.mlirods.language.TableGenLexerAdapter
import com.github.zero9178.mlirods.language.generated.TableGenTypes
import com.intellij.lexer.LayeredLexer

/**
 * Special lexer instance that is only used for syntax highlighting but never for parsing.
 * This lexer may include additional logic such as highlighting for string literals, tokens in inactive preprocessor
 * code etc. that would be harmful for parsing.
 */
class TableGenHighlighterLexer : LayeredLexer(TableGenLexerAdapter()) {
    init {
        registerLayer(TableGenStringLexerAdapter(TableGenTypes.LINE_STRING_LITERAL), TableGenTypes.LINE_STRING_LITERAL)
        registerLayer(
            TableGenStringLexerAdapter(TableGenTypes.LINE_STRING_LITERAL_BAD),
            TableGenTypes.LINE_STRING_LITERAL_BAD
        )
    }
}