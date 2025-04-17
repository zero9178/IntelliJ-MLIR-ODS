package com.github.zero9178.mlirods.language.psi.impl

import com.github.zero9178.mlirods.language.generated.psi.TableGenClassRef
import com.github.zero9178.mlirods.language.stubs.impl.TableGenClassRefStub
import com.intellij.extapi.psi.StubBasedPsiElementBase
import com.intellij.lang.ASTNode
import com.intellij.psi.stubs.IStubElementType

abstract class TableGenClassRefMixin : StubBasedPsiElementBase<TableGenClassRefStub>, TableGenClassRef {

    constructor(node: ASTNode) : super(node)

    constructor(stub: TableGenClassRefStub, stubType: IStubElementType<*, *>) : super(stub, stubType)
}