package com.github.zero9178.mlirods.language.stubs.impl

import com.github.zero9178.mlirods.language.generated.psi.TableGenFieldBodyItem
import com.github.zero9178.mlirods.language.generated.psi.impl.TableGenFieldBodyItemImpl
import com.github.zero9178.mlirods.language.stubs.TableGenStubElementType
import com.github.zero9178.mlirods.language.stubs.TableGenStubElementTypes
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.*

/**
 * Stub interface for [TableGenFieldBodyItem].
 */
interface TableGenFieldBodyItemStub : StubElement<TableGenFieldBodyItem> {
    val name: String
}

class TableGenFieldBodyItemStubElementType(debugName: String) :
    TableGenStubElementType<TableGenFieldBodyItemStub, TableGenFieldBodyItem>(debugName, ::TableGenFieldBodyItemImpl) {

    override fun shouldCreateStub(node: ASTNode): Boolean {
        return TableGenFieldBodyItemImpl(node).fieldName != null
    }

    override fun createStub(
        psi: TableGenFieldBodyItem, parentStub: StubElement<out PsiElement?>?
    ): TableGenFieldBodyItemStub {
        return TableGenFieldBodyItemStubImpl(psi.fieldName!!, parentStub)
    }

    override fun serialize(
        stub: TableGenFieldBodyItemStub, dataStream: StubOutputStream
    ) {
        dataStream.writeUTFFast(stub.name)
    }

    override fun deserialize(
        dataStream: StubInputStream, parentStub: StubElement<*>?
    ): TableGenFieldBodyItemStub {
        return TableGenFieldBodyItemStubImpl(dataStream.readUTFFast(), parentStub)
    }

    override fun isAlwaysLeaf(root: StubBase<*>) = true
}

private class TableGenFieldBodyItemStubImpl(
    override val name: String, parent: StubElement<out PsiElement>?
) : StubBase<TableGenFieldBodyItem>(
    parent, TableGenStubElementTypes.FIELD_BODY_ITEM
), TableGenFieldBodyItemStub
