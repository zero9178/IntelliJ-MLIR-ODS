package com.github.zero9178.mlirods.language.psi

import com.github.zero9178.mlirods.language.generated.psi.TableGenFieldAccessValueNode
import com.intellij.openapi.util.TextRange
import com.intellij.psi.AbstractElementManipulator

private class TableGenFieldAccessValueNodeManipulator :
    AbstractElementManipulator<TableGenFieldAccessValueNode>() {
    override fun handleContentChange(
        element: TableGenFieldAccessValueNode, range: TextRange, newContent: String
    ): TableGenFieldAccessValueNode? {
        val fieldIdentifier = element.fieldIdentifier ?: return null

        val newName = range.shiftLeft(getRangeInElement(element).startOffset).replace(fieldIdentifier.text, newContent)
        fieldIdentifier.replace(createIdentifier(element.project, newName))
        return element
    }

    override fun getRangeInElement(element: TableGenFieldAccessValueNode): TextRange {
        return element.fieldIdentifier?.textRangeInParent ?: TextRange.EMPTY_RANGE
    }
}
