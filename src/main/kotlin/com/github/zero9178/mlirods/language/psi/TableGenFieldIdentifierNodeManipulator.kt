package com.github.zero9178.mlirods.language.psi

import com.github.zero9178.mlirods.language.generated.psi.TableGenLetBodyItem
import com.intellij.openapi.util.TextRange
import com.intellij.psi.AbstractElementManipulator

private abstract class TableGenFieldIdentifierNodeManipulator<T : TableGenFieldIdentifierNode> :
    AbstractElementManipulator<T>() {
    override fun handleContentChange(
        element: T, range: TextRange, newContent: String
    ): T? {
        val fieldIdentifier = element.fieldIdentifier ?: return null
        val text = fieldIdentifier.text
        val newClassName =
            range.shiftLeft(getRangeInElement(element).startOffset).replace(text, newContent)
        fieldIdentifier.replace(createIdentifier(element.project, newClassName))
        return element
    }

    override fun getRangeInElement(element: T): TextRange {
        return element.fieldIdentifier?.textRangeInParent ?: TextRange.EMPTY_RANGE
    }
}

private class TableGenLetBodyItemManipulator : TableGenFieldIdentifierNodeManipulator<TableGenLetBodyItem>()