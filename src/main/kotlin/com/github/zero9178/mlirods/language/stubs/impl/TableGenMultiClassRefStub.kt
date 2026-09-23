package com.github.zero9178.mlirods.language.stubs.impl

import com.github.zero9178.mlirods.language.generated.psi.TableGenMultiClassRef
import com.github.zero9178.mlirods.language.generated.psi.impl.TableGenMultiClassRefImpl
import com.github.zero9178.mlirods.language.stubs.TableGenStubElementType
import com.github.zero9178.mlirods.language.stubs.TableGenStubElementTypes
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.StubBase
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream

/**
 * Stub interface for [TableGenMultiClassRef].
 */
interface TableGenMultiClassRefStub : StubElement<TableGenMultiClassRef> {
    val name: String
}

class TableGenMultiClassRefStubElementType(debugName: String) :
    TableGenStubElementType<TableGenMultiClassRefStub, TableGenMultiClassRef>(
        debugName,
        ::TableGenMultiClassRefImpl
    ) {

    override fun createStub(
        psi: TableGenMultiClassRef, parentStub: StubElement<out PsiElement?>?
    ): TableGenMultiClassRefStub {
        return TableGenMultiClassRefStubImpl(psi.className, parentStub)
    }

    override fun serialize(
        stub: TableGenMultiClassRefStub, dataStream: StubOutputStream
    ) {
        dataStream.writeUTFFast(stub.name)
    }

    override fun deserialize(
        dataStream: StubInputStream, parentStub: StubElement<*>?
    ): TableGenMultiClassRefStub {
        return TableGenMultiClassRefStubImpl(dataStream.readUTFFast(), parentStub)
    }
}

private class TableGenMultiClassRefStubImpl(
    override val name: String,
    parent: StubElement<out PsiElement>?
) : StubBase<TableGenMultiClassRef>(
    parent, TableGenStubElementTypes.MULTI_CLASS_REF
), TableGenMultiClassRefStub
