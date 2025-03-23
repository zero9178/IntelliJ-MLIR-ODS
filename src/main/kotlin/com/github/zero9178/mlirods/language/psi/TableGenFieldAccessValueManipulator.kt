package com.github.zero9178.mlirods.language.psi

import com.github.zero9178.mlirods.language.generated.psi.TableGenFieldAccessValue
import com.intellij.openapi.util.TextRange
import com.intellij.psi.AbstractElementManipulator

private class TableGenFieldAccessValueManipulator :
    AbstractElementManipulator<TableGenFieldAccessValue>() {
    override fun handleContentChange(
        element: TableGenFieldAccessValue, range: TextRange, newContent: String
    ): TableGenFieldAccessValue? {
        val fieldIdentifier = element.fieldIdentifier ?: return null

        val newName = range.shiftLeft(getRangeInElement(element).startOffset).replace(fieldIdentifier.text, newContent)
        fieldIdentifier.replace(createIdentifier(element.project, newName))
        return element
    }

    override fun getRangeInElement(element: TableGenFieldAccessValue): TextRange {
        return element.fieldIdentifier?.textRangeInParent ?: TextRange.EMPTY_RANGE
    }
}
