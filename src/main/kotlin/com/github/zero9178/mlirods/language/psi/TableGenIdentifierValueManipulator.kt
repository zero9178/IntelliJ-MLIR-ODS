package com.github.zero9178.mlirods.language.psi

import com.github.zero9178.mlirods.language.generated.psi.TableGenIdentifierValue
import com.intellij.openapi.util.TextRange
import com.intellij.psi.AbstractElementManipulator

private class TableGenIdentifierValueManipulator : AbstractElementManipulator<TableGenIdentifierValue>() {

    override fun handleContentChange(
        element: TableGenIdentifierValue, range: TextRange, newContent: String
    ) = element.replace(
        createIdentifierValue(
            element.project,
            range.replace(element.text, newContent)
        )
    ) as TableGenIdentifierValue

    override fun getRangeInElement(element: TableGenIdentifierValue): TextRange {
        return element.identifier.textRangeInParent
    }
}
