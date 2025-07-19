package com.github.zero9178.mlirods.language

import com.github.zero9178.mlirods.language.generated.TableGenTypes
import com.github.zero9178.mlirods.language.generated.TableGenTypes.*
import com.github.zero9178.mlirods.model.TableGenContext
import com.intellij.lang.PsiBuilder
import com.intellij.lang.impl.DelegateMarker
import com.intellij.lang.impl.PsiBuilderAdapter
import com.intellij.psi.tree.IElementType
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentSet

/**
 * Special [PsiBuilder] which interprets Preprocessor directives when advancing the lexer.
 * Corresponding Psi elements are built and inserted for every Preprocessor directive.
 */
class TableGenPreprocessingPsiBuilder(myContext: TableGenContext, delegate: PsiBuilder) : PsiBuilderAdapter(delegate) {
    private var myDefines = myContext.defines.toPersistentSet()
    private var myFirst = true

    private enum class State {
        YIELD_TILL_ELSE_OR_ENDIF,
        YIELD_TILL_ENDIF,
    }

    private var myPreprocessorStateStack = persistentListOf<State>()

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
                        marker.done(SKIPPED_CODE_BLOCK)
                        marker = super.mark()
                        super.advanceLexer()
                        marker.done(ENDIF_DIRECTIVE)
                        return
                    }
                    level--
                }

                HASHTAG_ELSE -> {
                    if (level == 0 && allowElse) {
                        // Top level '#else' terminates the current skip and enables the next section to be included.
                        marker.done(SKIPPED_CODE_BLOCK)
                        marker = super.mark()
                        super.advanceLexer()
                        marker.done(ELSE_DIRECTIVE)
                        myPreprocessorStateStack = myPreprocessorStateStack.add(State.YIELD_TILL_ENDIF)
                        return
                    }
                }
            }
            super.advanceLexer()
        }
        marker.error("Expected '#endif' before end of file")
    }

    override fun advanceLexer() {
        initIfFirst()

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

                    tokenText?.let { myDefines = myDefines.add(it) }

                    super.advanceLexer()
                    marker.done(DEFINE_DIRECTIVE)
                    continue
                }

                HASHTAG_ENDIF -> {
                    // Found an '#endif' without a corresponding '#if(n)def'.
                    if (myPreprocessorStateStack.isEmpty()) {
                        val marker = super.mark()
                        super.advanceLexer()
                        marker.error("Unexpected '#endif'")
                        return
                    }

                    val marker = super.mark()
                    myPreprocessorStateStack = myPreprocessorStateStack.removeAt(myPreprocessorStateStack.size - 1)
                    super.advanceLexer()
                    marker.done(ENDIF_DIRECTIVE)
                    continue
                }

                HASHTAG_ELSE -> {
                    // Found an '#endif' without a corresponding '#if(n)def' or after a '#else' had already been found.
                    if (myPreprocessorStateStack.lastOrNull() != State.YIELD_TILL_ELSE_OR_ENDIF) {
                        val marker = super.mark()
                        super.advanceLexer()
                        marker.error("Unexpected '#else'")
                        return
                    }

                    val marker = super.mark()
                    super.advanceLexer()
                    marker.done(ELSE_DIRECTIVE)
                    skipCodeBlock(allowElse = false)
                    myPreprocessorStateStack = myPreprocessorStateStack.removeAt(myPreprocessorStateStack.size - 1)
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
                    val define = tokenText!!
                    super.advanceLexer()
                    marker.done(IFDEF_IFNDEF_DIRECTIVE)

                    if (myDefines.contains(define) == negate) {
                        skipCodeBlock(allowElse = true)
                        continue
                    }
                    myPreprocessorStateStack = myPreprocessorStateStack.add(State.YIELD_TILL_ELSE_OR_ENDIF)
                    continue
                }

                INCLUDE -> {
                    val marker = super.mark()
                    super.advanceLexer()

                    if (super.tokenType != LINE_STRING_LITERAL) {
                        marker.error("Expected string literal after 'include'")
                        return
                    }
                    super.advanceLexer()

                    marker.done(INCLUDE_DIRECTIVE)
                    continue
                }

                else -> return
            }
        }
    }

    override fun mark(): PsiBuilder.Marker {
        val defines = myDefines
        val stack = myPreprocessorStateStack
        return object : DelegateMarker(super.mark()) {
            override fun rollbackTo() {
                super.rollbackTo()
                myDefines = defines
                myPreprocessorStateStack = stack
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