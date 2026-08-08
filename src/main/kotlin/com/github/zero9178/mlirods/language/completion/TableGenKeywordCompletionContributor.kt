package com.github.zero9178.mlirods.language.completion

import com.github.zero9178.mlirods.language.psi.TableGenFile
import com.github.zero9178.mlirods.language.generated.TableGenTypes.IDENTIFIER
import com.github.zero9178.mlirods.language.generated.psi.*
import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.editorActions.TabOutScopesTracker
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.util.Key
import com.intellij.patterns.PatternCondition
import com.intellij.patterns.PlatformPatterns.psiElement
import com.intellij.patterns.StandardPatterns
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiErrorElement
import com.intellij.util.ProcessingContext

private val STATEMENT_START = arrayOf(
    "assert",
    "class",
    "def",
    "defm",
    "defset",
    "deftype",
    "defvar",
    "dump",
    "foreach",
    "if",
    "include",
    "let",
    "multiclass"
)

private val BODY_ITEM_START = arrayOf(
    "assert",
    "let",
    "defvar",
)
private val TYPE_START = arrayOf(
    "bit",
    "int",
    "string",
    "dag",
    "code",
)
private val TYPE_START_ANGLED = arrayOf(
    "list",
    "bits",
)

private fun keywordLookupElement(text: String) =
    LookupElementBuilder.create(text).bold()

private val TYPE_NODE_PARENT = Key.create<TableGenClassTypeNode>("TYPE_NODE_PARENT")
private val TYPE_NODE = psiElement(IDENTIFIER)
    // An arbitrary identifier within a type node is always parsed as a [TableGenClassTypeNode].
    .withParent(psiElement(TableGenClassTypeNode::class.java).save(TYPE_NODE_PARENT))

/**
 * Returns a pattern matching a [TableGenClassTypeNode] within [T].
 */
private inline fun <reified T : PsiElement> typeIn() = TYPE_NODE.with(object : PatternCondition<PsiElement>(null) {
    override fun accepts(
        t: PsiElement,
        context: ProcessingContext
    ): Boolean {
        return context.get(TYPE_NODE_PARENT)?.parent is T
    }
})

private val FIELD_BODY_ITEM_TYPE = typeIn<TableGenFieldBodyItem>()
private val TEMPLATE_ARG_DECL_TYPE = typeIn<TableGenTemplateArgDecl>()
private val DEFSET_STATEMENT_TYPE = typeIn<TableGenDefsetStatement>()

private const val INSERT_SPACE_KEY = "NO_SPACE"

internal class TableGenKeywordCompletionContributor : CompletionContributor(), DumbAware {
    init {
        // Completion for keywords within the top level of a file.
        extend(
            CompletionType.BASIC,
            psiElement(IDENTIFIER)
                .withParents(PsiErrorElement::class.java, TableGenFile::class.java),
            object : CompletionProvider<CompletionParameters>() {
                override fun addCompletions(
                    parameters: CompletionParameters,
                    context: ProcessingContext,
                    result: CompletionResultSet
                ) {
                    STATEMENT_START.forEach {
                        result.addElement(
                            keywordLookupElement(it).withInsertHandler(AddSpaceInsertHandler.INSTANCE)
                        )
                    }
                }
            }
        )

        // Completion for keywords within a class or def.
        // These get initially parsed as an erroneous field body item.
        extend(
            CompletionType.BASIC,
            FIELD_BODY_ITEM_TYPE,
            object : CompletionProvider<CompletionParameters>() {
                override fun addCompletions(
                    parameters: CompletionParameters,
                    context: ProcessingContext,
                    result: CompletionResultSet
                ) {
                    BODY_ITEM_START.forEach {
                        result.addElement(
                            keywordLookupElement(it).withInsertHandler(AddSpaceInsertHandler.INSTANCE)
                        )
                    }
                }
            }
        )

        // Completion for keywords within a type node.
        extend(
            CompletionType.BASIC,
            StandardPatterns.or(
                FIELD_BODY_ITEM_TYPE
                    .save(INSERT_SPACE_KEY),
                TEMPLATE_ARG_DECL_TYPE
                    .save(INSERT_SPACE_KEY),
                DEFSET_STATEMENT_TYPE
                    .save(INSERT_SPACE_KEY), TYPE_NODE
            ),
            object : CompletionProvider<CompletionParameters>() {
                override fun addCompletions(
                    parameters: CompletionParameters,
                    context: ProcessingContext,
                    result: CompletionResultSet
                ) {
                    TYPE_START.forEach {
                        var element = keywordLookupElement(it)
                        // Insert spaces afterwards for any constructs where whitespace is used as a delimiter to the
                        // next token.
                        if (context.get(INSERT_SPACE_KEY) != null)
                            element = element.withInsertHandler(AddSpaceInsertHandler.INSTANCE)

                        result.addElement(element)
                    }
                }
            }
        )

        // Completion for keywords within a type node where angled brackets should be inserted.
        extend(
            CompletionType.BASIC,
            TYPE_NODE,
            object : CompletionProvider<CompletionParameters>() {
                override fun addCompletions(
                    parameters: CompletionParameters,
                    context: ProcessingContext,
                    result: CompletionResultSet
                ) {
                    TYPE_START_ANGLED.forEach {
                        result.addElement(keywordLookupElement(it).withInsertHandler { c, l ->
                            // A '<' the completion was selected with is established by this handler already and must
                            // not be typed into the brackets the caret is placed in.
                            if (c.completionChar == '<') c.setAddCompletionChar(false)

                            val editor = c.editor
                            val angleBrackets = establishAngleBrackets(editor.document, editor.caretModel.offset)
                            // Enter the brackets if their content has yet to be written; otherwise leave the caret
                            // behind the keyword.
                            if (angleBrackets.isEmpty) {
                                editor.caretModel.moveToOffset(angleBrackets.start + 1)
                                TabOutScopesTracker.getInstance().registerEmptyScopeAtCaret(editor)
                            }
                        })
                    }
                }
            }
        )

        // Completion for keywords within a value.
        extend(
            CompletionType.BASIC,
            psiElement(IDENTIFIER).withParent(TableGenIdentifierValueNode::class.java),
            object : CompletionProvider<CompletionParameters>() {
                override fun addCompletions(
                    parameters: CompletionParameters,
                    context: ProcessingContext,
                    result: CompletionResultSet
                ) {
                    arrayOf("true", "false").forEach {
                        result.addElement(keywordLookupElement(it))
                    }
                }
            }
        )

        // TODO: Suggest "then", "else" and "in".
    }
}