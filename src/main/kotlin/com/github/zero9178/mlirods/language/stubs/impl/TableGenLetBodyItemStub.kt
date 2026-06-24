package com.github.zero9178.mlirods.language.stubs.impl


import com.github.zero9178.mlirods.language.generated.psi.TableGenLetBodyItem
import com.github.zero9178.mlirods.language.generated.psi.impl.TableGenLetBodyItemImpl
import com.github.zero9178.mlirods.language.psi.impl.LetMode
import com.github.zero9178.mlirods.language.stubs.TableGenStubElementType
import com.github.zero9178.mlirods.language.stubs.TableGenStubElementTypes
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.StubBase
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream

/**
 * Stub interface for [TableGenLetBodyItem].
 */
interface TableGenLetBodyItemStub : StubElement<TableGenLetBodyItem> {
    val name: String?

    /**
     * The 'prepend' or 'append' mode of the let, or null if it has none.
     */
    val letMode: LetMode?
}

class TableGenLetBodyItemStubElementType(debugName: String) :
    TableGenStubElementType<TableGenLetBodyItemStub, TableGenLetBodyItem>(debugName, ::TableGenLetBodyItemImpl) {

    override fun createStub(
        psi: TableGenLetBodyItem, parentStub: StubElement<out PsiElement?>?
    ): TableGenLetBodyItemStub {
        return TableGenLetBodyItemStubImpl(psi.fieldName, psi.letMode, parentStub)
    }

    override fun serialize(
        stub: TableGenLetBodyItemStub, dataStream: StubOutputStream
    ) {
        dataStream.writeName(stub.name)
        dataStream.writeBoolean(stub.letMode != null)
        stub.letMode?.let { dataStream.writeByte(it.ordinal) }
    }

    override fun deserialize(
        dataStream: StubInputStream, parentStub: StubElement<*>?
    ): TableGenLetBodyItemStub {
        val name = dataStream.readNameString()
        val letMode = if (dataStream.readBoolean()) LetMode.entries[dataStream.readByte().toInt()] else null
        return TableGenLetBodyItemStubImpl(name, letMode, parentStub)
    }
}

private class TableGenLetBodyItemStubImpl(
    override val name: String?, override val letMode: LetMode?, parent: StubElement<out PsiElement>?
) : StubBase<TableGenLetBodyItem>(
    parent, TableGenStubElementTypes.LET_BODY_ITEM
), TableGenLetBodyItemStub
