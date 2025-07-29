package com.github.zero9178.mlirods.language.psi.impl

import com.github.zero9178.mlirods.language.generated.psi.TableGenDefStatement
import com.github.zero9178.mlirods.language.generated.psi.TableGenIdentifierValueNode
import com.github.zero9178.mlirods.language.stubs.impl.TableGenDefNameIdentifierStub
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.IStubElementType

abstract class TableGenDefStatementMixin : TableGenRecordStatementMixin<TableGenDefNameIdentifierStub>,
    TableGenDefStatement, PsiElement {

    constructor(node: ASTNode) : super(node)

    constructor(stub: TableGenDefNameIdentifierStub, stubType: IStubElementType<*, *>) : super(stub, stubType)

    override fun getNameIdentifier(): PsiElement? {
        return valueNode as? TableGenIdentifierValueNode
    }
}
