package com.github.zero9178.mlirods.language.psi

import com.github.zero9178.mlirods.language.generated.psi.TableGenIfdefIfndefDirective
import com.intellij.openapi.util.TextRange
import com.intellij.psi.AbstractElementManipulator

internal class TableGenIfdefIfndefDirectiveManipulator :
    AbstractElementManipulator<TableGenIfdefIfndefDirective>() {
    override fun handleContentChange(
        element: TableGenIfdefIfndefDirective, range: TextRange, newContent: String
    ): TableGenIfdefIfndefDirective? {
        val identifier = element.identifier ?: return null

        val newName = range.shiftLeft(getRangeInElement(element).startOffset).replace(identifier.text, newContent)
        identifier.replace(createIdentifier(element.project, newName))
        return element
    }

    override fun getRangeInElement(element: TableGenIfdefIfndefDirective): TextRange {
        return element.identifier?.textRangeInParent ?: TextRange.EMPTY_RANGE
    }
}
