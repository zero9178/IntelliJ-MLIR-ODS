package com.github.zero9178.mlirods.language.psi.impl

import com.github.zero9178.mlirods.language.generated.psi.TableGenIdentifierValueNode
import com.github.zero9178.mlirods.language.generated.psi.TableGenIntegerValueNode
import com.github.zero9178.mlirods.language.stubs.impl.TableGenIdentifierValueNodeStub
import com.github.zero9178.mlirods.language.stubs.impl.TableGenIntegerValueNodeStub
import com.github.zero9178.mlirods.language.stubs.impl.TableGenValueNodeStub
import com.intellij.extapi.psi.StubBasedPsiElementBase
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.IStubElementType

abstract class TableGenIdentifierValueNodeMixin : StubBasedPsiElementBase<TableGenIdentifierValueNodeStub>,
    TableGenIdentifierValueNode, PsiElement {

    constructor(node: ASTNode) : super(node)

    constructor(
        stub: TableGenValueNodeStub,
        stubType: IStubElementType<*, *>
    ) : super(stub as TableGenIdentifierValueNodeStub, stubType)

    override val identifierText: String
        get() = stub?.identifier ?: identifier.text
}

