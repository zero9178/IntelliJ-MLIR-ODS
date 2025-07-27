package com.github.zero9178.mlirods.language.psi.impl

import com.github.zero9178.mlirods.language.generated.psi.TableGenArgValueItem
import com.github.zero9178.mlirods.language.generated.psi.TableGenClassTypeNode
import com.github.zero9178.mlirods.language.generated.psi.impl.TableGenTypeNodeImpl
import com.github.zero9178.mlirods.language.stubs.impl.TableGenTypeNodeStub
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.IStubElementType

abstract class TableGenClassTypeNodeMixin : TableGenTypeNodeImpl, TableGenClassTypeNode {

    constructor(node: ASTNode) : super(node)

    constructor(stub: TableGenTypeNodeStub, stubType: IStubElementType<*, *>) : super(stub, stubType)

    override fun getArgValueItemList(): List<TableGenArgValueItem> = emptyList()

    override fun getLAngle(): PsiElement? = null

    override fun getRAngle(): PsiElement? = null
}