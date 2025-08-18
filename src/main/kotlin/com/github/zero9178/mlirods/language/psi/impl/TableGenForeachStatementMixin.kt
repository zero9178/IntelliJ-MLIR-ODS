package com.github.zero9178.mlirods.language.psi.impl

import com.github.zero9178.mlirods.language.generated.psi.TableGenForeachStatement
import com.github.zero9178.mlirods.language.stubs.impl.TableGenStatementStub
import com.intellij.extapi.psi.StubBasedPsiElementBase
import com.intellij.lang.ASTNode
import com.intellij.psi.stubs.IStubElementType

abstract class TableGenForeachStatementMixin : StubBasedPsiElementBase<TableGenStatementStub>,
    TableGenForeachStatement {

    constructor(node: ASTNode) : super(node)

    constructor(stub: TableGenStatementStub, stubType: IStubElementType<*, *>) : super(stub, stubType)
}