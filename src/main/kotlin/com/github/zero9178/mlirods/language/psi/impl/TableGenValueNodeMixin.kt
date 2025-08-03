package com.github.zero9178.mlirods.language.psi.impl

import com.github.zero9178.mlirods.language.generated.psi.TableGenValueNode
import com.github.zero9178.mlirods.language.stubs.impl.TableGenValueNodeStub
import com.intellij.extapi.psi.StubBasedPsiElementBase
import com.intellij.lang.ASTNode
import com.intellij.psi.stubs.IStubElementType

abstract class TableGenValueNodeMixin : StubBasedPsiElementBase<TableGenValueNodeStub>, TableGenValueNode {

    constructor(node: ASTNode) : super(node)

    constructor(stub: TableGenValueNodeStub, stubType: IStubElementType<*, *>) : super(stub, stubType)
}

