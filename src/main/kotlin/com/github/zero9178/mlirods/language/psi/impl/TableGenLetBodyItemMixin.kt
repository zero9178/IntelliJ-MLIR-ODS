package com.github.zero9178.mlirods.language.psi.impl

import com.github.zero9178.mlirods.language.generated.psi.TableGenLetBodyItem
import com.github.zero9178.mlirods.language.psi.TableGenLetReference
import com.github.zero9178.mlirods.language.stubs.impl.TableGenLetBodyItemStub
import com.intellij.extapi.psi.StubBasedPsiElementBase
import com.intellij.lang.ASTNode
import com.intellij.psi.stubs.IStubElementType

abstract class TableGenLetBodyItemMixin : StubBasedPsiElementBase<TableGenLetBodyItemStub>,
    TableGenLetBodyItem {

    constructor(node: ASTNode) : super(node)

    constructor(stub: TableGenLetBodyItemStub, stubType: IStubElementType<*, *>) : super(stub, stubType)

    override fun getName(): String? {
        greenStub?.let { return it.name }

        return fieldIdentifier?.text
    }

    override val fieldName: String?
        get() = name

    override fun getTextOffset(): Int {
        return fieldIdentifier?.textOffset ?: super.getTextOffset()
    }

    override fun getReference() = TableGenLetReference(this)
}