package com.github.zero9178.mlirods.language.psi

import com.github.zero9178.mlirods.language.generated.psi.TableGenFieldIdentifier
import com.github.zero9178.mlirods.language.generated.psi.TableGenLetBodyItem
import com.intellij.openapi.util.TextRange
import com.intellij.psi.AbstractElementManipulator

private class TableGenFieldIdentifierManipulator :
    AbstractElementManipulator<TableGenFieldIdentifier>() {
    override fun handleContentChange(
        element: TableGenFieldIdentifier, range: TextRange, newContent: String
    ): TableGenFieldIdentifier? {
        TODO()
    }

    override fun getRangeInElement(element: TableGenFieldIdentifier): TextRange {
        return TextRange(0, element.textLength)
    }
}