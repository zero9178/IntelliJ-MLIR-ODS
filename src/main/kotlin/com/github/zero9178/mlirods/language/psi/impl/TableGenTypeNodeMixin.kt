package com.github.zero9178.mlirods.language.psi.impl

import com.github.zero9178.mlirods.language.generated.psi.TableGenTypeNode
import com.github.zero9178.mlirods.language.stubs.impl.TableGenTypeNodeStub
import com.intellij.extapi.psi.StubBasedPsiElementBase
import com.intellij.lang.ASTNode
import com.intellij.psi.stubs.IStubElementType

abstract class TableGenTypeNodeMixin : StubBasedPsiElementBase<TableGenTypeNodeStub>, TableGenTypeNode {

    constructor(node: ASTNode) : super(node)

    constructor(stub: TableGenTypeNodeStub, stubType: IStubElementType<*, *>) : super(stub, stubType)
}