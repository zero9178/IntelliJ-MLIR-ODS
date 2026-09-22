package com.github.zero9178.mlirods.language.psi

import com.github.zero9178.mlirods.language.generated.psi.TableGenBlockStringValue
import com.intellij.openapi.util.TextRange
import com.intellij.psi.AbstractElementManipulator

internal class TableGenBlockStringValueManipulator : AbstractElementManipulator<TableGenBlockStringValue>() {
    /**
     * Replaces [range] within the element with [newContent]. Block strings have no escape sequences, so the content is
     * inserted as is.
     */
    override fun handleContentChange(
        element: TableGenBlockStringValue, range: TextRange, newContent: String
    ) = element.updateText(range.replace(element.text, newContent)) as TableGenBlockStringValue

    /**
     * Returns the range of the string content, excluding the `[{` and `}]` delimiters.
     */
    override fun getRangeInElement(element: TableGenBlockStringValue): TextRange {
        val delimiterLength = 2
        return TextRange(delimiterLength, element.textLength - delimiterLength)
    }
}
