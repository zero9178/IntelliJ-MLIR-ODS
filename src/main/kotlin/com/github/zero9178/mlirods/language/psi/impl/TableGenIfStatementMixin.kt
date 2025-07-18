package com.github.zero9178.mlirods.language.psi.impl

import com.github.zero9178.mlirods.language.generated.psi.TableGenIfStatement
import com.github.zero9178.mlirods.language.stubs.impl.TableGenStatementStub
import com.intellij.extapi.psi.StubBasedPsiElementBase
import com.intellij.lang.ASTNode
import com.intellij.psi.stubs.IStubElementType

abstract class TableGenIfStatementMixin : StubBasedPsiElementBase<TableGenStatementStub>,
    TableGenIfStatement {

    constructor(node: ASTNode) : super(node)

    constructor(stub: TableGenStatementStub, stubType: IStubElementType<*, *>) : super(stub, stubType)
}