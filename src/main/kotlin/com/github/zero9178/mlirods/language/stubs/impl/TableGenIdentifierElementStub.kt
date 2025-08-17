package com.github.zero9178.mlirods.language.stubs.impl

import com.github.zero9178.mlirods.index.IDENTIFIER_INDEX
import com.github.zero9178.mlirods.language.generated.psi.impl.TableGenDefStatementImpl
import com.github.zero9178.mlirods.language.generated.psi.impl.TableGenDefvarStatementImpl
import com.github.zero9178.mlirods.language.psi.TableGenIdentifierElement
import com.github.zero9178.mlirods.language.stubs.TableGenFileStub
import com.github.zero9178.mlirods.language.stubs.TableGenStubElementType
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.*

/**
 * Stub interface for [TableGenIdentifierElement] elements.
 */
interface TableGenIdentifierElementStub : StubElement<TableGenIdentifierElement> {
    val name: String?
}

abstract class TableGenAbstractIdentifierElementStubElementType(
    debugName: String,
    constructor: (TableGenIdentifierElementStub, IStubElementType<*, *>) -> TableGenIdentifierElement
) :
    TableGenStubElementType<TableGenIdentifierElementStub, TableGenIdentifierElement>(
        debugName,
        constructor
    ) {

    override fun createStub(
        psi: TableGenIdentifierElement, parentStub: StubElement<out PsiElement?>?
    ): TableGenIdentifierElementStub {
        return TableGenIdentifierElementStubImpl(psi.name, parentStub, this)
    }

    override fun serialize(
        stub: TableGenIdentifierElementStub, dataStream: StubOutputStream
    ) {
        dataStream.writeName(stub.name)
    }

    override fun deserialize(
        dataStream: StubInputStream, parentStub: StubElement<*>?
    ): TableGenIdentifierElementStub {
        return TableGenIdentifierElementStubImpl(dataStream.readNameString(), parentStub, this)
    }
}

class TableGenDefvarStatementStubElementType(debugName: String) : TableGenAbstractIdentifierElementStubElementType(
    debugName,
    ::TableGenDefvarStatementImpl
) {
    override fun indexStub(
        stub: TableGenIdentifierElementStub,
        sink: IndexSink
    ) {
        // Only top-level 'defvar's should be in the index.
        stub.name?.let {
            when (stub.parentStub) {
                is TableGenFileStub ->
                    sink.occurrence(IDENTIFIER_INDEX, it)
            }
        }
    }
}

class TableGenDefStatementStubElementType(debugName: String) : TableGenAbstractIdentifierElementStubElementType(
    debugName,
    ::TableGenDefStatementImpl
) {
    override fun indexStub(
        stub: TableGenIdentifierElementStub,
        sink: IndexSink
    ) {
        stub.name?.let {
            sink.occurrence(IDENTIFIER_INDEX, it)
        }
    }
}

private class TableGenIdentifierElementStubImpl(
    override val name: String?,
    parent: StubElement<out PsiElement>?,
    elementType: TableGenAbstractIdentifierElementStubElementType,
) : StubBase<TableGenIdentifierElement>(
    parent, elementType
), TableGenIdentifierElementStub
