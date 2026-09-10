package com.github.zero9178.mlirods.language

import com.github.zero9178.mlirods.language.generated.TableGenLexer
import com.intellij.lexer.FlexAdapter
import com.intellij.lexer.MergingLexerAdapter
import com.intellij.psi.tree.TokenSet

/**
 * Adapter making sure the lexer's "beginning of line" flag matches the restart position.
 *
 * Preprocessor directives may only appear at the beginning of a line, which the lexer expresses with '^'. JFlex assumes
 * a restart always happens at the beginning of a line, causing e.g. a '#endif' in the middle of a line to lex as a
 * directive when the lexer is restarted right before it.
 */
private class TableGenFlexAdapter(private val lexer: TableGenLexer = TableGenLexer(null)) : FlexAdapter(lexer) {
    override fun start(buffer: CharSequence, startOffset: Int, endOffset: Int, initialState: Int) {
        super.start(buffer, startOffset, endOffset, initialState)
        lexer.syncAtBOL()
    }
}

class TableGenLexerAdapter : MergingLexerAdapter(TableGenFlexAdapter(), TokenSet.WHITE_SPACE)
