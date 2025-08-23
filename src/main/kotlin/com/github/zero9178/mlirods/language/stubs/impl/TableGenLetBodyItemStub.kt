package com.github.zero9178.mlirods.language.stubs.impl


import com.github.zero9178.mlirods.language.generated.psi.TableGenLetBodyItem
import com.github.zero9178.mlirods.language.generated.psi.impl.TableGenLetBodyItemImpl
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
}

class TableGenLetBodyItemStubElementType(debugName: String) :
    TableGenStubElementType<TableGenLetBodyItemStub, TableGenLetBodyItem>(debugName, ::TableGenLetBodyItemImpl) {

    override fun createStub(
        psi: TableGenLetBodyItem, parentStub: StubElement<out PsiElement?>?
    ): TableGenLetBodyItemStub {
        return TableGenLetBodyItemStubImpl(psi.fieldName, parentStub)
    }

    override fun serialize(
        stub: TableGenLetBodyItemStub, dataStream: StubOutputStream
    ) {
        dataStream.writeName(stub.name)
    }

    override fun deserialize(
        dataStream: StubInputStream, parentStub: StubElement<*>?
    ): TableGenLetBodyItemStub {
        return TableGenLetBodyItemStubImpl(dataStream.readNameString(), parentStub)
    }
}

private class TableGenLetBodyItemStubImpl(
    override val name: String?, parent: StubElement<out PsiElement>?
) : StubBase<TableGenLetBodyItem>(
    parent, TableGenStubElementTypes.LET_BODY_ITEM
), TableGenLetBodyItemStub
