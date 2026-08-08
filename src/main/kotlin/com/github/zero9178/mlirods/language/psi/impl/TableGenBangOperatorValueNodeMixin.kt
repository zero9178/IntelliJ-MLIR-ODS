package com.github.zero9178.mlirods.language.psi.impl

import com.github.zero9178.mlirods.language.generated.psi.TableGenBangOperatorValueNode
import com.github.zero9178.mlirods.language.stubs.impl.TableGenBangOperatorValueNodeStub
import com.github.zero9178.mlirods.language.stubs.impl.TableGenValueNodeStub
import com.intellij.extapi.psi.StubBasedPsiElementBase
import com.intellij.lang.ASTNode
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.IStubElementType
import com.intellij.psi.util.endOffset
import com.intellij.psi.util.startOffset

abstract class TableGenBangOperatorValueNodeMixin : StubBasedPsiElementBase<TableGenBangOperatorValueNodeStub>,
    TableGenBangOperatorValueNode, PsiElement {

    constructor(node: ASTNode) : super(node)

    constructor(
        stub: TableGenValueNodeStub,
        stubType: IStubElementType<*, *>
    ) : super(stub as TableGenBangOperatorValueNodeStub, stubType)

    override val operatorName: String
        get() = greenStub?.operator ?: bangOperator.text

    override val typeArgumentRange: TextRange?
        get() {
            val typeRange = typeNode?.textRange ?: return null
            return TextRange(
                leftAngle?.startOffset ?: typeRange.startOffset,
                rightAngle?.endOffset ?: typeRange.endOffset,
            )
        }
}
