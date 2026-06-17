package com.github.zero9178.mlirods.language.psi.impl

import com.github.zero9178.mlirods.language.generated.psi.TableGenBoolValueNode
import com.github.zero9178.mlirods.language.stubs.impl.TableGenBoolValueNodeStub
import com.github.zero9178.mlirods.language.stubs.impl.TableGenValueNodeStub
import com.intellij.extapi.psi.StubBasedPsiElementBase
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.IStubElementType

abstract class TableGenBoolValueNodeMixin : StubBasedPsiElementBase<TableGenBoolValueNodeStub>,
    TableGenBoolValueNode, PsiElement {

    constructor(node: ASTNode) : super(node)

    constructor(
        stub: TableGenValueNodeStub,
        stubType: IStubElementType<*, *>
    ) : super(stub as TableGenBoolValueNodeStub, stubType)
}
