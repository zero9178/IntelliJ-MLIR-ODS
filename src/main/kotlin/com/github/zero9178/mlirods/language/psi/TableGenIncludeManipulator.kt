package com.github.zero9178.mlirods.language.psi

import com.github.zero9178.mlirods.language.generated.psi.TableGenIncludeDirective
import com.intellij.openapi.util.TextRange
import com.intellij.psi.AbstractElementManipulator

class TableGenIncludeManipulator : AbstractElementManipulator<TableGenIncludeDirective>() {
    override fun handleContentChange(
        element: TableGenIncludeDirective,
        range: TextRange,
        newContent: String?
    ): TableGenIncludeDirective? {
        TODO("Not yet implemented")
    }

    override fun getRangeInElement(element: TableGenIncludeDirective): TextRange {
        return element.string?.textRangeInParent ?: TextRange.EMPTY_RANGE
    }
}