package com.github.zero9178.mlirods.language.psi.impl

import com.github.zero9178.mlirods.language.generated.psi.TableGenClassInstantiationValueNode
import com.github.zero9178.mlirods.language.stubs.impl.TableGenClassInstantiationValueNodeStub
import com.github.zero9178.mlirods.language.stubs.impl.TableGenValueNodeStub
import com.github.zero9178.mlirods.language.values.TableGenRecordValue
import com.github.zero9178.mlirods.language.values.TableGenValue
import com.intellij.extapi.psi.StubBasedPsiElementBase
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.IStubElementType

abstract class TableGenClassInstantiationValueNodeMixin : StubBasedPsiElementBase<TableGenClassInstantiationValueNodeStub>,
    TableGenClassInstantiationValueNode, PsiElement {

    constructor(node: ASTNode) : super(node)

    constructor(
        stub: TableGenValueNodeStub,
        stubType: IStubElementType<*, *>
    ) : super(stub as TableGenClassInstantiationValueNodeStub, stubType)

    override suspend fun evaluateInner(context: TableGenEvaluationContext): TableGenValue =
        TableGenRecordValue(this, context)
}
