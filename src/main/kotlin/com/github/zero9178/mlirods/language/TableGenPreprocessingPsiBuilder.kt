package com.github.zero9178.mlirods.language

import com.github.zero9178.mlirods.language.generated.TableGenTypes.*
import com.intellij.lang.LighterASTNode
import com.intellij.lang.PsiBuilder
import com.intellij.lang.impl.DelegateMarker
import com.intellij.lang.impl.PsiBuilderAdapter
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.IFileElementType
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf

/**
 * Special [PsiBuilder] which interprets Preprocessor directives when advancing the lexer.
 * Corresponding Psi elements are built and inserted for every Preprocessor directive.
 *
 * Directives following a token are interpreted while advancing past that token, i.e. while the markers of the elements
 * ending with that token are still open. Markers returned by this builder therefore complete before these trailing
 * directives, placing the directives within the innermost element that also contains the next token.
 */
class TableGenPreprocessingPsiBuilder(delegate: PsiBuilder) : PsiBuilderAdapter(delegate) {
    private var myDefines = persistentSetOf<String>()
    private var myFirst = true

    private enum class State {
        YIELD_TILL_ELSE_OR_ENDIF,
        YIELD_TILL_ENDIF,
    }

    private var myPreprocessorStateStack = persistentListOf<State>()

    /**
     * First marker of the directives interpreted after the last token, or null if there were none.
     */
    private var myTrailingDirectivesStart: PsiBuilder.Marker? = null

    /**
     * Last marker of the directives interpreted after the last token, or null if there were none.
     */
    private var myTrailingDirectivesEnd: PsiBuilder.Marker? = null

    /**
     * Latest marker completed before [myTrailingDirectivesStart], or null if there is none.
     */
    private var myLatestDoneBeforeTrailingDirectives: PsiBuilder.Marker? = null

    /**
     * Ensures the current token of the lexer points to the first non-preprocessor token.
     */
    private fun initIfFirst() {
        if (myFirst) {
            myFirst = false
            parseAndSkip()
        }
    }

    override fun getTokenType(): IElementType? {
        initIfFirst()
        return super.tokenType
    }

    /**
     * Implements a skipped code block in the preprocessor.
     * Consumes and skips all tokens until a '#endif' or '#else' (if [allowElse] is true) is found.
     */
    private fun skipCodeBlock(allowElse: Boolean) {
        var marker = super.mark()
        // Balance level for '#if(n)def's.
        var level = 0
        while (!eof()) {
            when (super.tokenType) {
                HASHTAG_IFNDEF, HASHTAG_IFDEF -> {
                    level++
                }

                HASHTAG_ENDIF -> {
                    if (level == 0) {
                        marker.doneDirective(SKIPPED_CODE_BLOCK)
                        marker = super.mark()
                        super.advanceLexer()
                        marker.doneDirective(ENDIF_DIRECTIVE)
                        return
                    }
                    level--
                }

                HASHTAG_ELSE -> {
                    if (level == 0 && allowElse) {
                        // Top level '#else' terminates the current skip and enables the next section to be included.
                        marker.doneDirective(SKIPPED_CODE_BLOCK)
                        marker = super.mark()
                        super.advanceLexer()
                        marker.doneDirective(ELSE_DIRECTIVE)
                        myPreprocessorStateStack = myPreprocessorStateStack.adding(State.YIELD_TILL_ENDIF)
                        return
                    }
                }
            }
            super.advanceLexer()
        }
        marker.errorDirective("Expected '#endif' before end of file")
    }

    private fun PsiBuilder.Marker.doneDirective(type: IElementType) {
        done(type)
        addTrailingDirective(this)
    }

    private fun PsiBuilder.Marker.errorDirective(message: String) {
        error(message)
        addTrailingDirective(this)
    }

    private fun addTrailingDirective(marker: PsiBuilder.Marker) {
        if (myTrailingDirectivesStart == null) {
            myTrailingDirectivesStart = marker
        }
        myTrailingDirectivesEnd = marker
    }

    override fun advanceLexer() {
        initIfFirst()

        myTrailingDirectivesStart = null
        myTrailingDirectivesEnd = null
        myLatestDoneBeforeTrailingDirectives = null
        super.advanceLexer()
        parseAndSkip()
    }

    /**
     * Interpret all preprocessor directives at the current lexer position until none are found.
     */
    private fun parseAndSkip() {
        while (!eof()) {
            when (super.tokenType) {
                HASHTAG_DEFINE -> {
                    val marker = super.mark()
                    super.advanceLexer()
                    if (super.tokenType != IDENTIFIER) {
                        // TODO: Consider emitting a nice error message here?
                        marker.rollbackTo()
                        return
                    }

                    tokenText?.let { myDefines = myDefines.adding(it) }

                    super.advanceLexer()
                    marker.doneDirective(DEFINE_DIRECTIVE)
                    continue
                }

                HASHTAG_ENDIF -> {
                    // Found an '#endif' without a corresponding '#if(n)def'.
                    if (myPreprocessorStateStack.isEmpty()) {
                        val marker = super.mark()
                        super.advanceLexer()
                        marker.errorDirective("Unexpected '#endif'")
                        return
                    }

                    val marker = super.mark()
                    myPreprocessorStateStack = myPreprocessorStateStack.removingAt(myPreprocessorStateStack.size - 1)
                    super.advanceLexer()
                    marker.doneDirective(ENDIF_DIRECTIVE)
                    continue
                }

                HASHTAG_ELSE -> {
                    // Found an '#endif' without a corresponding '#if(n)def' or after a '#else' had already been found.
                    if (myPreprocessorStateStack.lastOrNull() != State.YIELD_TILL_ELSE_OR_ENDIF) {
                        val marker = super.mark()
                        super.advanceLexer()
                        marker.errorDirective("Unexpected '#else'")
                        return
                    }

                    val marker = super.mark()
                    super.advanceLexer()
                    marker.doneDirective(ELSE_DIRECTIVE)
                    skipCodeBlock(allowElse = false)
                    myPreprocessorStateStack = myPreprocessorStateStack.removingAt(myPreprocessorStateStack.size - 1)
                    continue
                }

                HASHTAG_IFNDEF, HASHTAG_IFDEF -> {
                    val negate = super.tokenType == HASHTAG_IFNDEF

                    val marker = super.mark()
                    super.advanceLexer()
                    if (super.tokenType != IDENTIFIER) {
                        marker.rollbackTo()
                        return
                    }
                    // The token was just checked to be an IDENTIFIER, which always has text.
                    val define = checkNotNull(tokenText) { "IDENTIFIER token without text" }
                    super.advanceLexer()
                    marker.doneDirective(IFDEF_IFNDEF_DIRECTIVE)

                    if (myDefines.contains(define) == negate) {
                        skipCodeBlock(allowElse = true)
                        continue
                    }
                    myPreprocessorStateStack = myPreprocessorStateStack.adding(State.YIELD_TILL_ELSE_OR_ENDIF)
                    continue
                }

                INCLUDE -> {
                    val marker = super.mark()
                    super.advanceLexer()

                    if (super.tokenType != LINE_STRING_LITERAL) {
                        marker.errorDirective("Expected string literal after 'include'")
                        return
                    }
                    super.advanceLexer()

                    marker.doneDirective(INCLUDE_DIRECTIVE)
                    continue
                }

                else -> return
            }
        }
    }

    override fun mark(): PsiBuilder.Marker = Marker(super.mark())

    override fun getLatestDoneMarker(): LighterASTNode? {
        val latest = super.getLatestDoneMarker() ?: return null
        // Elements completed before the trailing directives are placed in front of them, but the directives remain
        // the latest done markers of the delegate. Grammar-Kit uses the latest done marker as the left operand of
        // left-recursive rules, which must be the element rather than a directive.
        val marker = myLatestDoneBeforeTrailingDirectives?.takeIf { latest === myTrailingDirectivesEnd }
            ?: latest as PsiBuilder.Marker
        return Marker(marker)
    }

    /**
     * Returns a function restoring the current state of the preprocessor.
     */
    private fun saveState(): () -> Unit {
        val defines = myDefines
        val stack = myPreprocessorStateStack
        val trailingDirectivesStart = myTrailingDirectivesStart
        val trailingDirectivesEnd = myTrailingDirectivesEnd
        val latestDoneBeforeTrailingDirectives = myLatestDoneBeforeTrailingDirectives
        return {
            myDefines = defines
            myPreprocessorStateStack = stack
            myTrailingDirectivesStart = trailingDirectivesStart
            myTrailingDirectivesEnd = trailingDirectivesEnd
            myLatestDoneBeforeTrailingDirectives = latestDoneBeforeTrailingDirectives
        }
    }

    private inner class Marker(delegate: PsiBuilder.Marker) : DelegateMarker(delegate) {
        private val myRestoreState = saveState()

        override fun rollbackTo() {
            super.rollbackTo()
            myRestoreState()
        }

        override fun precede(): PsiBuilder.Marker = Marker(delegate.precede())

        override fun drop() {
            if (delegate === myLatestDoneBeforeTrailingDirectives) {
                myLatestDoneBeforeTrailingDirectives = null
            }
            super.drop()
        }

        override fun done(type: IElementType) {
            val before = trailingDirectivesStart(type) ?: return super.done(type)
            delegate.doneBefore(type, before)
            myLatestDoneBeforeTrailingDirectives = delegate
        }

        override fun error(message: String) {
            val before = trailingDirectivesStart(TokenType.ERROR_ELEMENT) ?: return super.error(message)
            delegate.errorBefore(message, before)
            myLatestDoneBeforeTrailingDirectives = delegate
        }

        /**
         * Returns the marker before which this marker must be completed with [type] to exclude the trailing
         * directives, or null if it does not contain any.
         */
        private fun trailingDirectivesStart(type: IElementType): PsiBuilder.Marker? {
            // The file must contain all tokens, including any directives at its end.
            if (type is IFileElementType) return null

            // Markers started after the trailing directives do not contain them.
            return myTrailingDirectivesStart?.takeIf {
                delegate.startIndex < it.startIndex
            }
        }
    }

    override fun rawTokenIndex(): Int {
        initIfFirst()
        return super.rawTokenIndex()
    }

    override fun getCurrentOffset(): Int {
        initIfFirst()
        return super.getCurrentOffset()
    }
}