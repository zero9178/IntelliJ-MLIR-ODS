package com.github.zero9178.mlirods.language

import com.github.zero9178.mlirods.language.psi.TableGenFoldingElement
import com.intellij.lang.ASTNode
import com.intellij.lang.folding.CustomFoldingBuilder
import com.intellij.lang.folding.FoldingDescriptor
import com.intellij.openapi.editor.Document
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil

private class TableGenFoldingBuilder : CustomFoldingBuilder(), DumbAware {
    override fun buildLanguageFoldRegions(
        descriptors: MutableList<FoldingDescriptor>,
        root: PsiElement,
        document: Document,
        quick: Boolean
    ) {
        if (root !is TableGenFile) return

        PsiTreeUtil.findChildrenOfAnyType(root, TableGenFoldingElement::class.java)
            .forEach {
                it.getFoldingTextRanges().forEach { foldingText ->
                    descriptors.add(FoldingDescriptor(it, foldingText))
                }
            }
    }

    override fun getLanguagePlaceholderText(
        node: ASTNode,
        range: TextRange
    ): String? {
        return (node.psi as? TableGenFoldingElement)?.getPlaceHolderText(range)
    }

    override fun isRegionCollapsedByDefault(node: ASTNode): Boolean {
        return false
    }
}