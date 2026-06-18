package com.github.zero9178.mlirods.language.stubs.impl

import com.github.zero9178.mlirods.language.generated.psi.TableGenArgValueItem
import com.github.zero9178.mlirods.language.generated.psi.impl.TableGenArgValueItemImpl
import com.github.zero9178.mlirods.language.stubs.TableGenStubElementType
import com.github.zero9178.mlirods.language.stubs.TableGenStubElementTypes
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.StubBase
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream

/**
 * Stub interface for [TableGenArgValueItem].
 */
interface TableGenArgValueItemStub : StubElement<TableGenArgValueItem> {
    val isNamedArgument: Boolean
    val identifier: String?
}

class TableGenArgValueItemStubElementType(debugName: String) :
    TableGenStubElementType<TableGenArgValueItemStub, TableGenArgValueItem>(
        debugName, ::TableGenArgValueItemImpl
    ) {
    override fun createStub(
        psi: TableGenArgValueItem, parentStub: StubElement<out PsiElement?>?
    ): TableGenArgValueItemStub = TableGenArgValueItemStubImpl(psi.isNamedArgument, psi.identifier?.text, parentStub)

    override fun serialize(stub: TableGenArgValueItemStub, dataStream: StubOutputStream) {
        dataStream.writeBoolean(stub.isNamedArgument)
        dataStream.writeName(stub.identifier)
    }

    override fun deserialize(
        dataStream: StubInputStream, parentStub: StubElement<*>?
    ): TableGenArgValueItemStub {
        val isNamedArgument = dataStream.readBoolean()
        val identifier = dataStream.readNameString()
        return TableGenArgValueItemStubImpl(isNamedArgument, identifier, parentStub)
    }

    // An argument carries the value node it is assigned, which is itself stubbed.
    override fun isAlwaysLeaf(root: StubBase<*>) = false
}

private class TableGenArgValueItemStubImpl(
    override val isNamedArgument: Boolean, override val identifier: String?, parent: StubElement<out PsiElement>?
) : StubBase<TableGenArgValueItem>(
    parent, TableGenStubElementTypes.ARG_VALUE_ITEM
), TableGenArgValueItemStub
