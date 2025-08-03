package com.github.zero9178.mlirods.language.psi.impl

import com.github.zero9178.mlirods.language.generated.psi.TableGenStringValueNode
import com.github.zero9178.mlirods.language.stubs.impl.TableGenStringValueNodeStub
import com.github.zero9178.mlirods.language.stubs.impl.TableGenValueNodeStub
import com.intellij.extapi.psi.StubBasedPsiElementBase
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.IStubElementType

abstract class TableGenStringValueNodeMixin : StubBasedPsiElementBase<TableGenStringValueNodeStub>,
    TableGenStringValueNode, PsiElement {

    constructor(node: ASTNode) : super(node)

    constructor(
        stub: TableGenValueNodeStub,
        stubType: IStubElementType<*, *>
    ) : super(stub as TableGenStringValueNodeStub, stubType)
}

