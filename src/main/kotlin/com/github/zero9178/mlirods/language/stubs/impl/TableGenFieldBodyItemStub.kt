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
    TableGenStubElementType<TableGenFieldBodyItemStub, TableGenFieldBodyItem>(debugName) {
    override fun createPsi(stub: TableGenFieldBodyItemStub): TableGenFieldBodyItem? {
        return TableGenFieldBodyItemImpl(stub, this)
    }

    override fun shouldCreateStub(node: ASTNode): Boolean {
        return TableGenFieldBodyItemImpl(node).fieldIdentifier != null
    }

    override fun createStub(
        psi: TableGenFieldBodyItem, parentStub: StubElement<out PsiElement?>?
    ): TableGenFieldBodyItemStub {
        return TableGenFieldBodyItemStubImpl(psi.fieldIdentifier!!.text, parentStub)
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

    override fun indexStub(stub: TableGenFieldBodyItemStub, sink: IndexSink) {}
}

private class TableGenFieldBodyItemStubImpl(
    override val name: String, parent: StubElement<out PsiElement>?
) : StubBase<TableGenFieldBodyItem>(
    parent, TableGenStubElementTypes.FIELD_BODY_ITEM
), TableGenFieldBodyItemStub
