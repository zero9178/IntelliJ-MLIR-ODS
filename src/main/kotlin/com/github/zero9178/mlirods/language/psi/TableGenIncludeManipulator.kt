package com.github.zero9178.mlirods.language.psi

import com.github.zero9178.mlirods.language.generated.psi.TableGenIncludeDirective
import com.intellij.openapi.util.TextRange
import com.intellij.psi.AbstractElementManipulator

private class TableGenIncludeManipulator : AbstractElementManipulator<TableGenIncludeDirective>() {
    /**
     * Implements renaming of the string referring to the file being included.
     * [newContent] should be not-yet string-literal encoded string.
     * [range] refers to the text range being replaced and must be a subset of the range returned by [getRangeInElement]
     */
    override fun handleContentChange(
        element: TableGenIncludeDirective, range: TextRange, newContent: String?
    ): TableGenIncludeDirective? {
        val stringElement = element.string ?: return null
        val pureStringRange = getRangeInElement(element)
        if (!pureStringRange.contains(range)) return null

        val existingString = element.includeSuffix
        val replaceRange = range.shiftLeft(pureStringRange.startOffset)
        // TODO: This does not respect string literal encoding.
        val newString = replaceRange.replace(existingString, newContent ?: return null)

        stringElement.replace(createLineStringLiteral(element.project, newString))
        return element
    }

    /**
     * Returns the text range that can be manipulated.
     * Returns the range of the string literal excluding quotes.
     */
    override fun getRangeInElement(element: TableGenIncludeDirective): TextRange {
        val string = element.string ?: return TextRange.EMPTY_RANGE
        val quoteLength = 1
        return TextRange(
            string.textRangeInParent.endOffset - string.textLength + quoteLength,
            string.textRangeInParent.endOffset - quoteLength
        )
    }
}