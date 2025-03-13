package com.github.zero9178.mlirods.language.stubs.impl

import com.github.zero9178.mlirods.index.CLASS_INDEX
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
        return TableGenClassStatementStubImpl(psi.identifier!!.text, parentStub)
    }

    override fun serialize(
        stub: TableGenClassStatementStub, dataStream: StubOutputStream
    ) {
        dataStream.writeUTFFast(stub.name)
    }

    override fun deserialize(
        dataStream: StubInputStream, parentStub: StubElement<*>?
    ): TableGenClassStatementStub {
        return TableGenClassStatementStubImpl(dataStream.readUTFFast(), parentStub)
    }

    override fun indexStub(stub: TableGenClassStatementStub, sink: IndexSink) {
        sink.occurrence(CLASS_INDEX, stub.name)
    }
}

private class TableGenClassStatementStubImpl(
    override val name: String,
    parent: StubElement<out PsiElement>?
) : StubBase<TableGenClassStatement>(
    parent, TableGenStubElementTypes.CLASS_STATEMENT
), TableGenClassStatementStub
