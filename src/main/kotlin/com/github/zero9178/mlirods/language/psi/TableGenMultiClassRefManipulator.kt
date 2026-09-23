package com.github.zero9178.mlirods.language.psi

import com.github.zero9178.mlirods.language.generated.psi.TableGenMultiClassRef
import com.intellij.openapi.util.TextRange
import com.intellij.psi.AbstractElementManipulator

internal class TableGenMultiClassRefManipulator : AbstractElementManipulator<TableGenMultiClassRef>() {
    override fun handleContentChange(
        element: TableGenMultiClassRef, range: TextRange, newContent: String
    ): TableGenMultiClassRef? {
        val identifier = element.identifier

        val newName = range.shiftLeft(getRangeInElement(element).startOffset).replace(identifier.text, newContent)
        identifier.replace(createIdentifier(element.project, newName))
        return element
    }

    override fun getRangeInElement(element: TableGenMultiClassRef): TextRange {
        return element.identifier.textRangeInParent
    }
}
