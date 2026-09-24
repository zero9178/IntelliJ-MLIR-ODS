package com.github.zero9178.mlirods.language.documentation

import com.github.zero9178.mlirods.color.IDENTIFIER
import com.github.zero9178.mlirods.language.TableGenLanguage
import com.github.zero9178.mlirods.language.generated.TableGenTypes
import com.github.zero9178.mlirods.language.generated.psi.TableGenAbstractRef
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
import com.github.zero9178.mlirods.language.psi.TableGenReferencingElement
import com.github.zero9178.mlirods.model.TableGenCompilationContext
import com.intellij.codeInsight.documentation.DocumentationManagerProtocol
import com.intellij.codeInsight.documentation.DocumentationManagerUtil
import com.intellij.lang.documentation.DocumentationMarkup
import com.intellij.lang.documentation.QuickDocHighlightingHelper.appendStyledSignatureFragment
import com.intellij.markdown.utils.doc.DocMarkdownToHtmlConverter
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
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
 * A name within the text of a [Definition] at [range] that links to the documentation of [target].
 */
private class DefinitionLink(val range: TextRange, val target: PsiElement)

/**
 * The header of an element rendered in normal syntax, which is shown as the definition part of its documentation.
 * [links] are the names within [text] that link to the documented elements they refer to, in text order.
 */
private class Definition(val text: String, val links: List<DefinitionLink>)

/**
 * Builds a [Definition] piece by piece, recording the range of every linked name as it is appended. Names are resolved
 * within [context].
 */
private class DefinitionBuilder(private val context: TableGenCompilationContext) {
    private val text = StringBuilder()
    private val links = mutableListOf<DefinitionLink>()

    // Whitespace within source is only appended once text follows it, dropping any at the end of a definition.
    private var pendingSpace = false

    fun append(text: String): DefinitionBuilder {
        if (text.isEmpty()) return this

        if (pendingSpace) this.text.append(' ')
        pendingSpace = false
        this.text.append(text)
        return this
    }

    /**
     * Appends [name], linking it to [target] if that is a documented element.
     */
    fun appendName(name: String, target: PsiElement?): DefinitionBuilder {
        append(name)
        if (name.isNotEmpty() && target != null && isDocumentedTableGenElement(target))
            links += DefinitionLink(TextRange(text.length - name.length, text.length), target)
        return this
    }

    /**
     * Appends [name], linking it to the definition [element] refers to if that is a documented element. Nothing is
     * resolved in dumb mode.
     */
    fun appendReference(name: String, element: TableGenReferencingElement): DefinitionBuilder =
        appendName(
            name, if (DumbService.isDumb(element.project)) null else element.referencedDefinitionBlocking(context)
        )

    /**
     * Appends the source of [element] with all whitespace collapsed to a single space, linking every name within it
     * that refers to a documented element.
     */
    fun appendSource(element: PsiElement): DefinitionBuilder {
        var child = element.firstChild
        if (child == null) {
            appendSourceLeaf(element)
            return this
        }
        while (child != null) {
            appendSource(child)
            child = child.nextSibling
        }
        return this
    }

    private fun appendSourceLeaf(leaf: PsiElement) {
        if (leaf is PsiWhiteSpace) {
            pendingSpace = text.isNotEmpty()
            return
        }

        val leafText = StringUtil.collapseWhiteSpace(leaf.text)
        // Only the name within a referencing element is linked, e.g. not the angle brackets of a class instantiation.
        val referencing = (leaf.parent as? TableGenReferencingElement)?.takeIf { parent ->
            parent.references.any { it.absoluteRange.contains(leaf.textRange) }
        }
        if (referencing == null) append(leafText)
        else appendReference(leafText, referencing)
    }

    fun build() = Definition(text.toString(), links.toList())
}

/**
 * Appends the header of a record-like element in normal syntax: the introducing keyword, name and template argument
 * declarations, followed by its parent classes. Template argument declarations are rendered as plain `<type> <name>`
 * with any default value elided. [parents] associates each parent class name with the reference to it; any template
 * arguments passed to it are elided as `<...>`.
 */
private fun DefinitionBuilder.appendRecordHeader(
    keyword: String,
    name: String?,
    templateArgs: List<TableGenTemplateArgDecl>,
    parents: List<Pair<String, TableGenAbstractRef>>
) {
    append(keyword)
    name?.let { append(" ").append(it) }
    templateArgs.forEachIndexed { index, arg ->
        append(if (index == 0) "<" else ", ")
        appendSource(arg.typeNode)
        arg.name?.let { append(" ").append(it) }
    }
    if (templateArgs.isNotEmpty()) append(">")
    parents.forEachIndexed { index, (parentName, ref) ->
        append(if (index == 0) " : " else ", ")
        appendReference(parentName, ref)
        if (ref.lAngle != null) append("<...>")
    }
}

/**
 * Appends the definition of a field: its type and name, the initializer if it is a single atom, and the record the
 * field is defined in.
 */
private fun DefinitionBuilder.appendFieldDefinition(element: TableGenFieldBodyItem) {
    appendSource(element.typeNode)
    element.fieldName?.let { append(" ").append(it) }
    element.valueNode?.let {
        append(" = ")
        if (isAtomicValue(it)) appendSource(it) else append("...")
    }

    val record = PsiTreeUtil.getParentOfType(element, TableGenRecord::class.java, true)
    val recordName = record?.name
    if (recordName != null) {
        val recordKeyword = if (record is TableGenClassStatement) "class" else "def"
        append("\n  in ").append(recordKeyword).append(" ").appendName(recordName, record)
    }
}

/**
 * Returns the header of [element] shown as the definition part of its documentation. The names within it are resolved
 * in the context [element] is resolved in by default.
 */
private fun definition(element: PsiElement): Definition =
    DefinitionBuilder(TableGenCompilationContext.activeFor(element)).apply {
        when (element) {
            is TableGenClassStatement -> appendRecordHeader(
                "class", element.name, element.templateArgDeclList, element.classRefList.map { it.className to it })

            is TableGenMulticlassStatement -> appendRecordHeader(
                "multiclass", element.name, element.templateArgDeclList,
                element.multiClassRefList.map { it.className to it })

            is TableGenDefStatement -> appendRecordHeader(
                "def", element.name, emptyList(), element.classRefList.map { it.className to it })

            is TableGenFieldBodyItem -> appendFieldDefinition(element)

            else -> {
                // The whole statement except for its terminating semicolon.
                val semicolon = element.lastChild?.takeIf { it.elementType == TableGenTypes.SEMICOLON }
                generateSequence(element.firstChild) { it.nextSibling }.takeWhile { it != semicolon }.forEach {
                    appendSource(it)
                }
            }
        }
    }.build()

/**
 * Appends [code] highlighted as TableGen. Unlike highlighting it as a whole, line breaks at the start and end of [code]
 * are kept.
 */
private fun StringBuilder.appendHighlightedLines(project: Project, code: String) {
    code.split('\n').forEachIndexed { index, line ->
        if (index > 0) append('\n')
        appendStyledSignatureFragment(project, TableGenLanguage.INSTANCE, line)
    }
}

/**
 * Appends the linked [name] highlighted as an identifier. Unlike the platform's link fragments, the name has the same
 * color as it would have if it wasn't a link, falling back to the default foreground rather than the link color.
 */
private fun StringBuilder.appendLinkLabel(name: String): StringBuilder {
    val scheme = EditorColorsManager.getInstance().globalScheme
    val attributes = scheme.getAttributes(IDENTIFIER)?.clone() ?: TextAttributes()
    if (attributes.foregroundColor == null) attributes.foregroundColor = scheme.defaultForeground
    return appendStyledSignatureFragment(StringUtil.escapeXmlEntities(name), attributes)
}

/**
 * Appends [definition] highlighted as TableGen, turning its links into hyperlinks whose reference text is the index of
 * the link, see [resolveDocumentationLink].
 */
private fun StringBuilder.appendDefinition(project: Project, definition: Definition) {
    var start = 0
    definition.links.forEachIndexed { index, link ->
        appendHighlightedLines(project, definition.text.substring(start, link.range.startOffset))
        val label = StringBuilder().appendLinkLabel(link.range.substring(definition.text))
        DocumentationManagerUtil.createHyperlink(this, index.toString(), label.toString(), true)
        start = link.range.endOffset
    }
    appendHighlightedLines(project, definition.text.substring(start))
}

/**
 * Returns the element the hyperlink with [url] in the documentation of [element] leads to or null if the
 * documentation has no such hyperlink.
 */
internal fun resolveDocumentationLink(element: PsiElement, url: String): PsiElement? {
    if (!url.startsWith(DocumentationManagerProtocol.PSI_ELEMENT_PROTOCOL)) return null

    val index = url.removePrefix(DocumentationManagerProtocol.PSI_ELEMENT_PROTOCOL).toIntOrNull() ?: return null
    return definition(element).links.getOrNull(index)?.target
}

/**
 * Renders the quick documentation HTML for [element], consisting of its definition and the comment block directly
 * preceding it. Names within the definition link to the documentation of the elements they refer to. Returns null for
 * elements no documentation should be shown for.
 */
internal fun generateDocumentation(element: PsiElement): String? {
    if (!isDocumentedTableGenElement(element)) return null

    return buildString {
        append(DocumentationMarkup.DEFINITION_START)
        appendDefinition(element.project, definition(element))
        append(DocumentationMarkup.DEFINITION_END)

        documentationMarkdown(element)?.let {
            append(DocumentationMarkup.CONTENT_START)
            append(DocMarkdownToHtmlConverter.convert(element.project, it, TableGenLanguage.INSTANCE))
            append(DocumentationMarkup.CONTENT_END)
        }
    }
}
