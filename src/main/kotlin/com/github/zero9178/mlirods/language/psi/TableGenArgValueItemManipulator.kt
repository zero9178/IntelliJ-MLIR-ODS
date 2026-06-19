package com.github.zero9178.mlirods.language.psi

import com.github.zero9178.mlirods.language.generated.psi.TableGenArgValueItem
import com.intellij.openapi.util.TextRange
import com.intellij.psi.AbstractElementManipulator

internal class TableGenArgValueItemManipulator : AbstractElementManipulator<TableGenArgValueItem>() {

    override fun handleContentChange(
        element: TableGenArgValueItem, range: TextRange, newContent: String
    ): TableGenArgValueItem? {
        val identifier = element.identifier ?: return null

        val newName = range.shiftLeft(getRangeInElement(element).startOffset).replace(identifier.text, newContent)
        identifier.replace(createIdentifier(element.project, newName))
        return element
    }

    /**
     * Returns the text range that can be manipulated.
     * Returns the range of the string literal excluding quotes.
     */
    override fun getRangeInElement(element: TableGenArgValueItem): TextRange {
        return element.identifier?.textRangeInParent ?: TextRange.EMPTY_RANGE
    }
}