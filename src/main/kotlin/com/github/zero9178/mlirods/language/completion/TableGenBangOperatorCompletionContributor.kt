package com.github.zero9178.mlirods.language.completion

import com.github.zero9178.mlirods.language.generated.TableGenTypes.BANG_OPERATOR
import com.github.zero9178.mlirods.language.generated.psi.TableGenBangOperatorValueNode
import com.github.zero9178.mlirods.language.psi.TableGenBangOperator
import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.codeInsight.completion.InsertHandler
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.completion.util.ParenthesesInsertHandler
import com.intellij.codeInsight.editorActions.TabOutScopesTracker
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.project.DumbAware
import com.intellij.patterns.PlatformPatterns.psiElement
import com.intellij.psi.util.startOffset
import com.intellij.util.ProcessingContext

/**
 * Inserts the '<type>' argument and parentheses '!cast' requires after the operator, following the conventions of
 * [ParenthesesInsertHandler]: delimiters the user has already written are reused, a missing closing delimiter of a pair
 * the user started is inserted such that the operator's delimiters are always balanced afterwards, and the caret is
 * placed in the leftmost delimiter pair the user has yet to fill in. [TabOutScopesTracker] is told about any empty pair
 * so that the user can tab from the type argument into the parentheses and out of them.
 */
private object TypeAndArgumentsInsertHandler : InsertHandler<LookupElement> {
    override fun handleInsert(
        context: InsertionContext,
        item: LookupElement
    ) {
        val editor = context.editor
        val document = editor.document

        // Insert no delimiters if the user has turned that off, unless the completion was selected by typing the
        // leading one.
        if (context.completionChar != '<' && !editor.settings.isInsertParenthesesAutomatically) return
        // A delimiter the completion was selected with is established by this handler already and must not be typed
        // into the pair the caret is placed in.
        if (context.completionChar == '<' || context.completionChar == '(') context.setAddCompletionChar(false)

        val tail = editor.caretModel.offset

        // Establish a balanced '<...>' right after the operator, reusing as much as the user has already written.
        val angleBrackets = establishAngleBrackets(document, tail)

        // Establish the parentheses right after the '>'.
        val parenOffset = angleBrackets.end + 1
        val insertedParentheses = document.charsSequence.getOrNull(parenOffset) != '('
        if (insertedParentheses) document.insertString(parenOffset, "()")
        val emptyParentheses = document.charsSequence.getOrNull(parenOffset + 1) == ')'

        val tracker = TabOutScopesTracker.getInstance()
        if (angleBrackets.isEmpty) {
            // The type argument has yet to be written: place the caret inside the '<>' and make tab jump behind the
            // opening parenthesis, regardless of how much type text has been typed by then.
            editor.caretModel.moveToOffset(angleBrackets.start + 1)
            tracker.registerEmptyScope(editor, angleBrackets.start + 1, parenOffset + 1)
            // A second scope lets a final tab escape the parentheses. It has to span both pairs: the tracker discards
            // any scope that is edited around, so only a scope containing the type argument survives it being typed.
            if (emptyParentheses) tracker.registerScopeRange(editor, angleBrackets.start + 1, parenOffset + 1)
        } else if (insertedParentheses || angleBrackets.insertedClosing) {
            // The type argument is complete after this insertion: continue with the arguments.
            editor.caretModel.moveToOffset(parenOffset + 1)
            if (emptyParentheses) tracker.registerEmptyScope(editor, parenOffset + 1)
        }
        // Otherwise everything existed already; leave the caret behind the operator.
    }
}

private fun bangOperatorLookupElement(operator: TableGenBangOperator) =
    LookupElementBuilder.create(operator.operatorName).bold().withInsertHandler(
        // The platform handler suffices for operators taking only parentheses.
        if (operator.requiresTypeArgument) TypeAndArgumentsInsertHandler
        else ParenthesesInsertHandler.WITH_PARAMETERS
    )

/**
 * Completion for every bang operator known to the plugin, offered as soon as the '!' starting one has been typed.
 */
internal class TableGenBangOperatorCompletionContributor : CompletionContributor(), DumbAware {
    init {
        extend(
            CompletionType.BASIC,
            // A '!' followed by the completion's dummy identifier lexes as a generic bang operator token, regardless of
            // which operator the user is about to type.
            psiElement(BANG_OPERATOR).withParent(TableGenBangOperatorValueNode::class.java),
            object : CompletionProvider<CompletionParameters>() {
                override fun addCompletions(
                    parameters: CompletionParameters,
                    context: ProcessingContext,
                    result: CompletionResultSet
                ) {
                    // The default prefix ends at the '!' as it is not an identifier character, leaving a prefix that no
                    // operator name can ever match. Use the operator token up to the caret instead.
                    val position = parameters.position
                    val operators =
                        result.withPrefixMatcher(position.text.substring(0, parameters.offset - position.startOffset))

                    TableGenBangOperator.entries.forEach {
                        operators.addElement(bangOperatorLookupElement(it))
                    }
                }
            }
        )
    }
}
