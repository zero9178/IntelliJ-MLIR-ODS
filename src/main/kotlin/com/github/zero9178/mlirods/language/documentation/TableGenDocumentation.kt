package com.github.zero9178.mlirods.language.documentation

import com.github.zero9178.mlirods.language.TableGenLanguage
import com.github.zero9178.mlirods.language.generated.TableGenTypes
import com.github.zero9178.mlirods.language.generated.psi.TableGenClassStatement
import com.github.zero9178.mlirods.language.generated.psi.TableGenDefStatement
import com.github.zero9178.mlirods.language.generated.psi.TableGenDefvarStatement
import com.github.zero9178.mlirods.language.generated.psi.TableGenFieldBodyItem
import com.github.zero9178.mlirods.language.generated.psi.TableGenBinaryIntegerValueNode
import com.github.zero9178.mlirods.language.generated.psi.TableGenBoolValueNode
import com.github.zero9178.mlirods.language.generated.psi.TableGenIdentifierValueNode
import com.github.zero9178.mlirods.language.generated.psi.TableGenIntegerValueNode
import com.github.zero9178.mlirods.language.generated.psi.TableGenMulticlassStatement
import com.github.zero9178.mlirods.language.generated.psi.TableGenStringValueNode
import com.github.zero9178.mlirods.language.generated.psi.TableGenTemplateArgDecl
import com.github.zero9178.mlirods.language.generated.psi.TableGenUndefValueNode
import com.github.zero9178.mlirods.language.generated.psi.TableGenValueNode
import com.github.zero9178.mlirods.language.psi.TableGenRecord
import com.intellij.lang.documentation.DocumentationMarkup
import com.intellij.lang.documentation.QuickDocHighlightingHelper
import com.intellij.markdown.utils.doc.DocMarkdownToHtmlConverter
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.elementType

/**
 * Returns true if quick documentation should be offered for [element].
 */
internal fun isDocumentedTableGenElement(element: PsiElement): Boolean = when (element) {
    is TableGenClassStatement, is TableGenMulticlassStatement, is TableGenDefStatement,
    is TableGenDefvarStatement, is TableGenFieldBodyItem -> true

    else -> false
}

/**
 * Returns the comments making up the documentation of [element] in source order.
 *
 * These are the comments of the contiguous comment block ending on the line directly above [element]. A blank line
 * detaches a comment block from the element, and a comment sharing a line with preceding code trails that code
 * rather than documenting [element].
 */
private fun documentationComments(element: PsiElement): List<PsiComment> = buildList {
    var current = element.prevSibling
    // Each comment of the block must be separated from what follows it by exactly one line break.
    while (current is PsiWhiteSpace && StringUtil.countNewLines(current.text) == 1) {
        val comment = current.prevSibling as? PsiComment ?: break
        val beforeComment = comment.prevSibling
        if (beforeComment != null && (beforeComment !is PsiWhiteSpace || !beforeComment.textContains('\n'))) break
        add(comment)
        current = beforeComment
    }
}.asReversed()

/**
 * Returns the text lines of [comment] with the comment syntax stripped.
 */
private fun commentContentLines(comment: PsiComment): List<String> =
    if (comment.elementType == TableGenTypes.LINE_COMMENT) {
        listOf(comment.text.trimStart('/').removePrefix(" "))
    } else {
        comment.text.removePrefix("/*").removeSuffix("*/").lines().map {
            it.trim().trimStart('*').removePrefix(" ")
        }
    }

/**
 * Returns the documentation of [element] as Markdown text or null if it has none.
 */
private fun documentationMarkdown(element: PsiElement): String? {
    val lines = documentationComments(element).flatMap(::commentContentLines)
    if (lines.all { it.isBlank() }) return null

    return lines.joinToString("\n")
}

/**
 * Returns true if [value] is a single atom that can be rendered in a definition line as is.
 */
private fun isAtomicValue(value: TableGenValueNode): Boolean = when (value) {
    is TableGenIntegerValueNode, is TableGenBinaryIntegerValueNode, is TableGenBoolValueNode,
    is TableGenUndefValueNode, is TableGenIdentifierValueNode, is TableGenStringValueNode ->
        !value.textContains('\n')

    else -> false
}

/**
 * Returns the header of a record-like element in normal syntax: the introducing keyword, name and template argument
 * declarations, followed by its parent classes. Template argument declarations are rendered as plain `<type> <name>`
 * with any default value elided. [parents] associates each parent class name with whether template arguments are
 * passed to it; these are elided as `<...>`.
 */
private fun recordDefinitionText(
    keyword: String,
    name: String?,
    templateArgs: List<TableGenTemplateArgDecl>,
    parents: List<Pair<String, Boolean>>
): String = buildString {
    append(keyword)
    name?.let { append(' ').append(it) }
    if (templateArgs.isNotEmpty()) {
        templateArgs.joinTo(this, prefix = "<", postfix = ">") {
            listOfNotNull(StringUtil.collapseWhiteSpace(it.typeNode.text), it.name).joinToString(" ")
        }
    }
    if (parents.isNotEmpty()) {
        append(" : ")
        parents.joinTo(this) { (parentName, hasArgs) -> if (hasArgs) "$parentName<...>" else parentName }
    }
}

/**
 * Returns the definition of a field: its type and name, the initializer if it is a single atom, and the record the
 * field is defined in.
 */
private fun fieldDefinitionText(element: TableGenFieldBodyItem): String = buildString {
    append(StringUtil.collapseWhiteSpace(element.typeNode.text))
    element.fieldName?.let { append(' ').append(it) }
    element.valueNode?.let {
        append(" = ")
        append(if (isAtomicValue(it)) it.text else "...")
    }

    val record = PsiTreeUtil.getParentOfType(element, TableGenRecord::class.java, true)
    val recordName = record?.name
    if (recordName != null) {
        val recordKeyword = if (record is TableGenClassStatement) "class" else "def"
        append("\n  in ").append(recordKeyword).append(' ').append(recordName)
    }
}

/**
 * Returns a rendering of the header of [element] shown as the definition part of its documentation.
 */
private fun definitionText(element: PsiElement): String = when (element) {
    is TableGenClassStatement -> recordDefinitionText(
        "class", element.name, element.templateArgDeclList,
        element.classRefList.map { it.className to (it.lAngle != null) })

    is TableGenMulticlassStatement -> recordDefinitionText(
        "multiclass", element.name, element.templateArgDeclList,
        element.multiClassRefList.map { it.identifier.text to (it.lAngle != null) })

    is TableGenDefStatement -> recordDefinitionText(
        "def", element.name, emptyList(),
        element.classRefList.map { it.className to (it.lAngle != null) })

    is TableGenFieldBodyItem -> fieldDefinitionText(element)

    else -> StringUtil.collapseWhiteSpace(element.text).removeSuffix(";").trimEnd()
}

/**
 * Renders the quick documentation HTML for [element], consisting of its definition and the comment block directly
 * preceding it. Returns null for elements no documentation should be shown for.
 */
internal fun generateDocumentation(element: PsiElement): String? {
    if (!isDocumentedTableGenElement(element)) return null

    return buildString {
        append(DocumentationMarkup.DEFINITION_START)
        append(
            QuickDocHighlightingHelper.getStyledSignatureFragment(
                element.project, TableGenLanguage.INSTANCE, definitionText(element)
            )
        )
        append(DocumentationMarkup.DEFINITION_END)

        documentationMarkdown(element)?.let {
            append(DocumentationMarkup.CONTENT_START)
            append(DocMarkdownToHtmlConverter.convert(element.project, it, TableGenLanguage.INSTANCE))
            append(DocumentationMarkup.CONTENT_END)
        }
    }
}
