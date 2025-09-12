package com.github.zero9178.mlirods.language.stubs.impl

import com.github.zero9178.mlirods.index.ALL_IDENTIFIERS_INDEX
import com.github.zero9178.mlirods.index.IDENTIFIER_INDEX
import com.github.zero9178.mlirods.index.MAY_DERIVE_CLASS_INDEX
import com.github.zero9178.mlirods.language.generated.psi.TableGenDefStatement
import com.github.zero9178.mlirods.language.generated.psi.impl.TableGenDefStatementImpl
import com.github.zero9178.mlirods.language.generated.psi.impl.TableGenDefvarStatementImpl
import com.github.zero9178.mlirods.language.generated.psi.impl.TableGenForeachIteratorImpl
import com.github.zero9178.mlirods.language.psi.TableGenIdentifierElement
import com.github.zero9178.mlirods.language.stubs.TableGenFileStub
import com.github.zero9178.mlirods.language.stubs.TableGenStubElementType
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.*

/**
 * Stub interface for [TableGenIdentifierElement] elements.
 */
sealed interface TableGenIdentifierElementStub : StubElement<TableGenIdentifierElement> {
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

private class TableGenIdentifierElementStubImpl(
    override val name: String?,
    parent: StubElement<out PsiElement>?,
    elementType: TableGenAbstractIdentifierElementStubElementType,
) : StubBase<TableGenIdentifierElement>(
    parent, elementType
), TableGenIdentifierElementStub

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
                is TableGenFileStub -> {
                    sink.occurrence(IDENTIFIER_INDEX, it)
                    sink.occurrence(ALL_IDENTIFIERS_INDEX, 0)
                }
            }
        }
    }
}

class TableGenForeachIteratorStubElementType(debugName: String) : TableGenAbstractIdentifierElementStubElementType(
    debugName,
    ::TableGenForeachIteratorImpl
)

sealed interface TableGenDefStatementStub : TableGenIdentifierElementStub {
    val baseClassNames: List<String>
}

class TableGenDefStatementStubElementType(debugName: String) :
    TableGenStubElementType<TableGenDefStatementStub, TableGenDefStatement>(
    debugName,
    ::TableGenDefStatementImpl
) {
    override fun createStub(
        psi: TableGenDefStatement, parentStub: StubElement<out PsiElement?>?
    ): TableGenDefStatementStub {
        return TableGenDefStatementElementStubImpl(psi.name, psi.classRefList.map { it.className }, parentStub, this)
    }

    override fun serialize(
        stub: TableGenDefStatementStub, dataStream: StubOutputStream
    ) {
        dataStream.writeName(stub.name)
        dataStream.writeVarInt(stub.baseClassNames.size)
        stub.baseClassNames.forEach {
            dataStream.writeName(it)
        }
    }

    override fun deserialize(
        dataStream: StubInputStream, parentStub: StubElement<*>?
    ): TableGenDefStatementStub {
        return TableGenDefStatementElementStubImpl(
            dataStream.readNameString(),
            buildList {
                repeat(dataStream.readVarInt()) {
                    add(dataStream.readNameString() ?: return@repeat)
                }
            }, parentStub, this
        )
    }

    override fun indexStub(
        stub: TableGenDefStatementStub,
        sink: IndexSink
    ) {
        stub.name?.let {
            sink.occurrence(IDENTIFIER_INDEX, it)
        }
        sink.occurrence(ALL_IDENTIFIERS_INDEX, 0)
        stub.baseClassNames.forEach {
            sink.occurrence(MAY_DERIVE_CLASS_INDEX, it)
        }
    }
}

private class TableGenDefStatementElementStubImpl(
    override val name: String?,
    override val baseClassNames: List<String>,
    parent: StubElement<out PsiElement>?,
    elementType: TableGenDefStatementStubElementType,
) : StubBase<TableGenIdentifierElement>(
    parent, elementType
), TableGenDefStatementStub
