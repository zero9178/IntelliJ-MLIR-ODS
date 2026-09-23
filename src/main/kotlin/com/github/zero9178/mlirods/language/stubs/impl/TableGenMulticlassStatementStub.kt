package com.github.zero9178.mlirods.language.stubs.impl

import com.github.zero9178.mlirods.index.MULTICLASS_INDEX
import com.github.zero9178.mlirods.language.generated.psi.TableGenMulticlassStatement
import com.github.zero9178.mlirods.language.generated.psi.impl.TableGenMulticlassStatementImpl
import com.github.zero9178.mlirods.language.stubs.TableGenStubElementType
import com.github.zero9178.mlirods.language.stubs.TableGenStubElementTypes
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.*

/**
 * Stub interface for [TableGenMulticlassStatement].
 */
interface TableGenMulticlassStatementStub : StubElement<TableGenMulticlassStatement> {
    val name: String?
}

class TableGenMulticlassStatementStubElementType(debugName: String) :
    TableGenStubElementType<TableGenMulticlassStatementStub, TableGenMulticlassStatement>(
        debugName,
        ::TableGenMulticlassStatementImpl
    ) {

    // Unlike a class, a multiclass without a name is still stubbed: it remains the scope of the statements within it,
    // which would otherwise be mistaken for top-level statements.
    override fun createStub(
        psi: TableGenMulticlassStatement, parentStub: StubElement<out PsiElement?>?
    ): TableGenMulticlassStatementStub {
        return TableGenMulticlassStatementStubImpl(psi.identifier?.text, parentStub)
    }

    override fun serialize(
        stub: TableGenMulticlassStatementStub, dataStream: StubOutputStream
    ) {
        dataStream.writeName(stub.name)
    }

    override fun deserialize(
        dataStream: StubInputStream, parentStub: StubElement<*>?
    ): TableGenMulticlassStatementStub {
        return TableGenMulticlassStatementStubImpl(dataStream.readNameString(), parentStub)
    }

    override fun indexStub(stub: TableGenMulticlassStatementStub, sink: IndexSink) {
        stub.name?.let {
            sink.occurrence(MULTICLASS_INDEX, it)
        }
    }
}

private class TableGenMulticlassStatementStubImpl(
    override val name: String?,
    parent: StubElement<out PsiElement>?
) : StubBase<TableGenMulticlassStatement>(
    parent, TableGenStubElementTypes.MULTICLASS_STATEMENT
), TableGenMulticlassStatementStub
