package com.github.zero9178.mlirods.language.stubs.impl

import com.github.zero9178.mlirods.language.generated.psi.TableGenClassRef
import com.github.zero9178.mlirods.language.generated.psi.impl.TableGenClassRefImpl
import com.github.zero9178.mlirods.language.stubs.TableGenStubElementType
import com.github.zero9178.mlirods.language.stubs.TableGenStubElementTypes
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.StubBase
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream

/**
 * Stub interface for [TableGenClassRef].
 */
interface TableGenClassRefStub : StubElement<TableGenClassRef> {
    val name: String
}

class TableGenClassRefStubElementType(debugName: String) :
    TableGenStubElementType<TableGenClassRefStub, TableGenClassRef>(
        debugName,
        ::TableGenClassRefImpl
    ) {

    override fun createStub(
        psi: TableGenClassRef, parentStub: StubElement<out PsiElement?>?
    ): TableGenClassRefStub {
        return TableGenClassRefStubImpl(psi.className, parentStub)
    }

    override fun serialize(
        stub: TableGenClassRefStub, dataStream: StubOutputStream
    ) {
        dataStream.writeUTFFast(stub.name)
    }

    override fun deserialize(
        dataStream: StubInputStream, parentStub: StubElement<*>?
    ): TableGenClassRefStub {
        return TableGenClassRefStubImpl(dataStream.readUTFFast(), parentStub)
    }
}

private class TableGenClassRefStubImpl(
    override val name: String,
    parent: StubElement<out PsiElement>?
) : StubBase<TableGenClassRef>(
    parent, TableGenStubElementTypes.CLASS_REF
), TableGenClassRefStub
