package com.github.zero9178.mlirods.language.psi.impl

import com.github.zero9178.mlirods.language.generated.psi.TableGenArgValueItem
import com.github.zero9178.mlirods.language.stubs.impl.TableGenArgValueItemStub
import com.intellij.extapi.psi.StubBasedPsiElementBase
import com.intellij.lang.ASTNode
import com.intellij.psi.stubs.IStubElementType

abstract class TableGenArgValueItemMixin : StubBasedPsiElementBase<TableGenArgValueItemStub>, TableGenArgValueItem {

    constructor(node: ASTNode) : super(node)

    constructor(stub: TableGenArgValueItemStub, stubType: IStubElementType<*, *>) : super(stub, stubType)

    override val isNamedArgument: Boolean
        get() = greenStub?.isNamedArgument ?: (equalsSign != null)
}
