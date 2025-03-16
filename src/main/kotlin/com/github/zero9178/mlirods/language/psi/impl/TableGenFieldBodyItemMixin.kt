package com.github.zero9178.mlirods.language.psi.impl

import com.github.zero9178.mlirods.language.generated.psi.TableGenFieldBodyItem
import com.github.zero9178.mlirods.language.stubs.impl.TableGenFieldBodyItemStub
import com.intellij.extapi.psi.StubBasedPsiElementBase
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.IStubElementType

abstract class TableGenFieldBodyItemMixin : StubBasedPsiElementBase<TableGenFieldBodyItemStub>,
    TableGenFieldBodyItem {

    constructor(node: ASTNode) : super(node)

    constructor(stub: TableGenFieldBodyItemStub, stubType: IStubElementType<*, *>) : super(stub, stubType)

    override fun getName(): String? {
        greenStub?.let { return it.name }

        return nameIdentifier?.text
    }

    override fun setName(name: String): PsiElement {
        TODO("not implemented")
    }

    override fun getNameIdentifier(): PsiElement? {
        return fieldIdentifier
    }

    override val fieldName: String?
        get() = name

    override fun getTextOffset(): Int {
        return nameIdentifier?.textOffset ?: super.getTextOffset()
    }
}