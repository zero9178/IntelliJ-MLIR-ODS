package com.github.zero9178.mlirods.language.format

import com.github.zero9178.mlirods.language.BANG_VALUES
import com.github.zero9178.mlirods.language.RECORDS
import com.github.zero9178.mlirods.language.VALUES
import com.github.zero9178.mlirods.language.generated.TableGenTypes
import com.intellij.formatting.*
import com.intellij.lang.ASTNode
import com.intellij.lang.tree.util.children
import com.intellij.lang.tree.util.siblings
import com.intellij.psi.TokenType.WHITE_SPACE
import com.intellij.psi.formatter.common.AbstractBlock
import com.intellij.psi.tree.IElementType
import com.jetbrains.rd.util.getOrCreate

private fun ASTNode.nonWhitespaceChildren() = children().filter {
    it.elementType != WHITE_SPACE
}

private class AlignmentCache {
    private val cache = mutableMapOf<Triple<Boolean, Alignment.Anchor, Any?>, Alignment>()

    fun createAlignment(): Alignment = createAlignment(false, Alignment.Anchor.LEFT)

    fun createAlignment(elementType: IElementType): Alignment =
        createAlignment(false, Alignment.Anchor.LEFT, elementType)

    fun createAlignment(allowBackwardShift: Boolean): Alignment =
        createAlignment(allowBackwardShift, Alignment.Anchor.LEFT)

    fun createAlignment(allowBackwardShift: Boolean, anchor: Alignment.Anchor, extraKey: Any? = null): Alignment =
        cache.getOrCreate(Triple(allowBackwardShift, anchor, extraKey)) {
            Alignment.createAlignment(allowBackwardShift, anchor)
        }
}

private fun isWithinBody(
    node: ASTNode, leftDelimiter: IElementType, rightDelimiter: IElementType, index: Int
): Boolean = node.nonWhitespaceChildren().withIndex().dropWhile { it.value.elementType != leftDelimiter }.drop(1).find {
    it.index == index || it.value.elementType == rightDelimiter
}?.index == index

private fun isWithinBody(
    node: ASTNode,
    leftDelimiter: IElementType,
    rightDelimiter: IElementType,
): Boolean = node.treeParent?.children().orEmpty().dropWhile { it.elementType != leftDelimiter }.drop(1).takeWhile {
    it.elementType != rightDelimiter
}.contains(node)

private fun TableGenBlock.createBlock(
    node: ASTNode, cache: AlignmentCache
): TableGenBlock {
    when (node.elementType) {
        TableGenTypes.COLON -> return TableGenBlock(
            node, spacingBuilder,
            Wrap.createWrap(WrapType.NORMAL, false),
            indent = Indent.getNormalIndent()
        )

        TableGenTypes.LANGLE -> return TableGenBlock(
            node, spacingBuilder,
            Wrap.createWrap(WrapType.NONE, false),
            indent = Indent.getContinuationIndent()
        )

        TableGenTypes.LBRACE -> return TableGenBlock(
            node, spacingBuilder,
            Wrap.createWrap(WrapType.NONE, false)
        )

        TableGenTypes.TEMPLATE_ARG_DECL, TableGenTypes.CLASS_REF, TableGenTypes.ARG_VALUE_ITEM, TableGenTypes.DAG_ARG -> {

            val alignment = when {
                node.textContains('\n') -> null
                node.elementType == TableGenTypes.TEMPLATE_ARG_DECL || node.elementType == TableGenTypes.CLASS_REF ->
                    cache.createAlignment(node.elementType)

                else -> cache.createAlignment()
            }
            return TableGenBlock(
                node, spacingBuilder, Wrap.createWrap(WrapType.NORMAL, false), alignment, Indent.getContinuationIndent()
            )
        }

        TableGenTypes.LPAREN -> if (node.treeParent?.elementType in BANG_VALUES) {
            return TableGenBlock(
                node,
                spacingBuilder,
                Wrap.createWrap(WrapType.NONE, false),
                indent = Indent.getContinuationIndent()
            )
        }

        TableGenTypes.RPAREN -> if (node.treeParent?.elementType in BANG_VALUES) {
            return TableGenBlock(node, spacingBuilder, alignment = cache.createAlignment())
        }

        in VALUES -> when (node.treeParent?.elementType) {
            in BANG_VALUES -> {
                val alignment = cache.createAlignment()
                return TableGenBlock(
                    node,
                    spacingBuilder,
                    Wrap.createWrap(WrapType.NORMAL, false),
                    alignment,
                    Indent.getContinuationIndent()
                )
            }

            TableGenTypes.LIST_INIT_VALUE -> {
                return TableGenBlock(
                    node,
                    spacingBuilder,
                    Wrap.createWrap(WrapType.NORMAL, false),
                    indent = Indent.getContinuationIndent()
                )
            }
        }

        TableGenTypes.BLOCK_COMMENT -> {
            val next = node.siblings(forward = true, withSelf = false).firstOrNull { it.elementType != WHITE_SPACE }
            when (next?.elementType) {
                TableGenTypes.ARG_VALUE_ITEM, in VALUES -> {
                    return TableGenBlock(
                        node, spacingBuilder,
                        Wrap.createWrap(WrapType.NORMAL, false),
                        cache.createAlignment(),
                        Indent.getContinuationIndent()
                    )
                }
            }
        }

        TableGenTypes.LINE_COMMENT -> {
            if (isWithinBody(node, TableGenTypes.LANGLE, TableGenTypes.RANGLE))
                return TableGenBlock(
                    node, spacingBuilder,
                    Wrap.createWrap(WrapType.NORMAL, false),
                    indent = Indent.getContinuationIndent()
                )
        }
    }

    if (node.treeParent?.elementType in RECORDS && isWithinBody(
            node, TableGenTypes.LBRACE, TableGenTypes.RBRACE
        )
    ) return TableGenBlock(
        node, spacingBuilder, Wrap.createWrap(WrapType.ALWAYS, false), indent = Indent.getNormalIndent()
    )

    return TableGenBlock(node, spacingBuilder)
}

class TableGenBlock(
    node: ASTNode,
    val spacingBuilder: SpacingBuilder,
    wrap: Wrap? = null,
    alignment: Alignment? = null,
    private val indent: Indent = Indent.getNoneIndent(),
) : AbstractBlock(node, wrap, alignment) {

    override fun getIndent() = indent

    override fun buildChildren(): List<Block> {
        val alignmentCache = AlignmentCache()
        return node.nonWhitespaceChildren().map {
            createBlock(it, alignmentCache)
        }.toList()
    }

    override fun getSpacing(
        child1: Block?, child2: Block
    ): Spacing? {
        return spacingBuilder.getSpacing(this, child1, child2)
    }

    override fun isLeaf() = false

    override fun getSubBlocks(): List<TableGenBlock> {
        // Note: Could be an unchecked cast and would then avoid the list copy.
        return super.getSubBlocks().filterIsInstance<TableGenBlock>()
    }

    override fun getChildAttributes(newChildIndex: Int): ChildAttributes {
        if (node.elementType in RECORDS && isWithinBody(
                node, TableGenTypes.LBRACE, TableGenTypes.RBRACE, newChildIndex
            )
        ) {
            return ChildAttributes(
                Indent.getNormalIndent(),
                this.subBlocks.asSequence().dropWhile { it.node.elementType != TableGenTypes.LBRACE }.drop(1)
                    .takeWhile {
                        it.node.elementType != TableGenTypes.RBRACE
                    }.firstOrNull()?.alignment
            )
        }

        return ChildAttributes(Indent.getNoneIndent(), null)
    }
}

