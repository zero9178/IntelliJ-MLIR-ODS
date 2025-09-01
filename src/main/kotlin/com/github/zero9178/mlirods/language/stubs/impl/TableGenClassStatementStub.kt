package com.github.zero9178.mlirods.language.stubs.impl

import com.github.zero9178.mlirods.index.ALL_CLASSES_INDEX
import com.github.zero9178.mlirods.index.CLASS_INDEX
import com.github.zero9178.mlirods.index.MAY_DERIVE_CLASS_INDEX
import com.github.zero9178.mlirods.language.generated.psi.TableGenClassStatement
import com.github.zero9178.mlirods.language.generated.psi.impl.TableGenClassStatementImpl
import com.github.zero9178.mlirods.language.stubs.TableGenStubElementType
import com.github.zero9178.mlirods.language.stubs.TableGenStubElementTypes
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.*

/**
 * Stub interface for [TableGenClassStatement].
 */
interface TableGenClassStatementStub : StubElement<TableGenClassStatement> {
    val name: String
    val hasBody: Boolean
    val baseClassNames: List<String>
}

class TableGenClassStatementStubElementType(debugName: String) :
    TableGenStubElementType<TableGenClassStatementStub, TableGenClassStatement>(
        debugName,
        ::TableGenClassStatementImpl
    ) {

    override fun shouldCreateStub(node: ASTNode?): Boolean {
        return TableGenClassStatementImpl(node).identifier != null
    }

    override fun createStub(
        psi: TableGenClassStatement, parentStub: StubElement<out PsiElement?>?
    ): TableGenClassStatementStub {
        return TableGenClassStatementStubImpl(psi.identifier!!.text, psi.hasBody, psi.classRefList.map {
            it.className
        }, parentStub)
    }

    override fun serialize(
        stub: TableGenClassStatementStub, dataStream: StubOutputStream
    ) {
        dataStream.writeUTFFast(stub.name)
        dataStream.writeBoolean(true)
        dataStream.writeVarInt(stub.baseClassNames.size)
        stub.baseClassNames.forEach {
            dataStream.writeName(it)
        }
    }

    override fun deserialize(
        dataStream: StubInputStream, parentStub: StubElement<*>?
    ): TableGenClassStatementStub {
        return TableGenClassStatementStubImpl(
            dataStream.readUTFFast(), dataStream.readBoolean(),
            buildList {
                repeat(dataStream.readVarInt()) { _ -> add(dataStream.readNameString() ?: return@repeat) }
            }, parentStub
        )
    }

    override fun indexStub(stub: TableGenClassStatementStub, sink: IndexSink) {
        sink.occurrence(CLASS_INDEX, stub.name)
        sink.occurrence(ALL_CLASSES_INDEX, 0)
        stub.baseClassNames.forEach {
            sink.occurrence(MAY_DERIVE_CLASS_INDEX, it)
        }
    }
}

private class TableGenClassStatementStubImpl(
    override val name: String,
    override val hasBody: Boolean,
    override val baseClassNames: List<String>,
    parent: StubElement<out PsiElement>?
) : StubBase<TableGenClassStatement>(
    parent, TableGenStubElementTypes.CLASS_STATEMENT
), TableGenClassStatementStub
