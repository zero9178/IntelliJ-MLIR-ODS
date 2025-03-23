package com.github.zero9178.mlirods.language.psi

import com.github.zero9178.mlirods.language.generated.psi.TableGenAbstractClassRef
import com.github.zero9178.mlirods.language.generated.psi.TableGenClassInstantiationValue
import com.github.zero9178.mlirods.language.generated.psi.TableGenClassRef
import com.github.zero9178.mlirods.language.generated.psi.TableGenClassTypeNode
import com.intellij.openapi.util.TextRange
import com.intellij.psi.AbstractElementManipulator

private abstract class TableGenAbstractClassRefManipulator<T : TableGenAbstractClassRef> :
    AbstractElementManipulator<T>() {
    override fun handleContentChange(
        element: T,
        range: TextRange,
        newContent: String
    ): T? {
        val newClassName =
            range.shiftLeft(getRangeInElement(element).startOffset).replace(element.className, newContent)
        element.classIdentifier.replace(createIdentifier(element.project, newClassName))
        return element
    }

    override fun getRangeInElement(element: T): TextRange {
        return element.classIdentifier.textRangeInParent
    }
}

private class TableGenClassRefManipulator : TableGenAbstractClassRefManipulator<TableGenClassRef>()
private class TableGenClassInstantiationValueManipulator :
    TableGenAbstractClassRefManipulator<TableGenClassInstantiationValue>()

private class TableGenClassTypeNodeManipulator : TableGenAbstractClassRefManipulator<TableGenClassTypeNode>()
