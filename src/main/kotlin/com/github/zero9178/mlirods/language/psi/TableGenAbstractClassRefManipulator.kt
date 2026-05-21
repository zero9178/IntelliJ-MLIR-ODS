package com.github.zero9178.mlirods.language.psi

import com.github.zero9178.mlirods.language.generated.psi.TableGenAbstractClassRef
import com.github.zero9178.mlirods.language.generated.psi.TableGenClassInstantiationValueNode
import com.github.zero9178.mlirods.language.generated.psi.TableGenClassRef
import com.github.zero9178.mlirods.language.generated.psi.TableGenClassTypeNode
import com.intellij.openapi.util.TextRange
import com.intellij.psi.AbstractElementManipulator

internal abstract class TableGenAbstractClassRefManipulator<T : TableGenAbstractClassRef> :
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

internal class TableGenClassRefManipulator : TableGenAbstractClassRefManipulator<TableGenClassRef>()
internal class TableGenClassInstantiationValueNodeManipulator :
    TableGenAbstractClassRefManipulator<TableGenClassInstantiationValueNode>()

internal class TableGenClassTypeNodeManipulator : TableGenAbstractClassRefManipulator<TableGenClassTypeNode>()
