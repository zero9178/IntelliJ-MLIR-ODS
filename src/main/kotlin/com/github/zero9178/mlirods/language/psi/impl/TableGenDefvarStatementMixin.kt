package com.github.zero9178.mlirods.language.psi.impl

import com.github.zero9178.mlirods.language.generated.psi.TableGenDefvarStatement
import com.github.zero9178.mlirods.language.stubs.impl.TableGenDefNameIdentifierStub
import com.intellij.extapi.psi.StubBasedPsiElementBase
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.IStubElementType

abstract class TableGenDefvarStatementMixin : StubBasedPsiElementBase<TableGenDefNameIdentifierStub>,
    TableGenDefvarStatement, PsiElement {

    constructor(node: ASTNode) : super(node)

    constructor(stub: TableGenDefNameIdentifierStub, stubType: IStubElementType<*, *>) : super(stub, stubType)
}