package com.github.zero9178.mlirods.language.stubs.impl


import com.github.zero9178.mlirods.language.generated.psi.TableGenLetItem
import com.github.zero9178.mlirods.language.generated.psi.impl.TableGenLetItemImpl
import com.github.zero9178.mlirods.language.psi.impl.LetMode
import com.github.zero9178.mlirods.language.stubs.TableGenStubElementType
import com.github.zero9178.mlirods.language.stubs.TableGenStubElementTypes
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.StubBase
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream

/**
 * Stub interface for [TableGenLetItem].
 */
interface TableGenLetItemStub : StubElement<TableGenLetItem> {
    val name: String?

    /**
     * The 'prepend' or 'append' mode of the let, or null if it has none.
     */
    val letMode: LetMode?
}

class TableGenLetItemStubElementType(debugName: String) :
    TableGenStubElementType<TableGenLetItemStub, TableGenLetItem>(debugName, ::TableGenLetItemImpl) {

    override fun createStub(
        psi: TableGenLetItem, parentStub: StubElement<out PsiElement?>?
    ): TableGenLetItemStub {
        return TableGenLetItemStubImpl(psi.fieldName, psi.letMode, parentStub)
    }

    override fun serialize(
        stub: TableGenLetItemStub, dataStream: StubOutputStream
    ) {
        dataStream.writeName(stub.name)
        dataStream.writeBoolean(stub.letMode != null)
        stub.letMode?.let { dataStream.writeByte(it.ordinal) }
    }

    override fun deserialize(
        dataStream: StubInputStream, parentStub: StubElement<*>?
    ): TableGenLetItemStub {
        val name = dataStream.readNameString()
        val letMode = if (dataStream.readBoolean()) LetMode.entries[dataStream.readByte().toInt()] else null
        return TableGenLetItemStubImpl(name, letMode, parentStub)
    }
}

private class TableGenLetItemStubImpl(
    override val name: String?, override val letMode: LetMode?, parent: StubElement<out PsiElement>?
) : StubBase<TableGenLetItem>(
    parent, TableGenStubElementTypes.LET_ITEM
), TableGenLetItemStub
