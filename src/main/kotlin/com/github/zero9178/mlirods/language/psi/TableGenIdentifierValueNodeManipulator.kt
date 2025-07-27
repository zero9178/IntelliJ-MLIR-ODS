package com.github.zero9178.mlirods.language.psi


import com.github.zero9178.mlirods.language.generated.psi.TableGenIdentifierValueNode
import com.intellij.openapi.util.TextRange
import com.intellij.psi.AbstractElementManipulator

private class TableGenIdentifierValueNodeManipulator : AbstractElementManipulator<TableGenIdentifierValueNode>() {

    override fun handleContentChange(
        element: TableGenIdentifierValueNode, range: TextRange, newContent: String
    ) = element.replace(
        createIdentifierValueNode(
            element.project,
            range.replace(element.text, newContent)
        )
    ) as TableGenIdentifierValueNode

    override fun getRangeInElement(element: TableGenIdentifierValueNode): TextRange {
        return element.identifier.textRangeInParent
    }
}
