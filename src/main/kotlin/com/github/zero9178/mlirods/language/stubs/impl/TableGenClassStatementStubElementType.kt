package com.github.zero9178.mlirods.language.stubs.impl

import com.github.zero9178.mlirods.index.CLASS_INDEX
import com.github.zero9178.mlirods.language.generated.psi.TableGenClassStatement
import com.github.zero9178.mlirods.language.generated.psi.impl.TableGenClassStatementImpl
import com.github.zero9178.mlirods.language.stubs.TableGenStubElementType
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.IndexSink
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream

class TableGenClassStatementStubElementType(debugName: String) :
    TableGenStubElementType<TableGenClassStatementStub, TableGenClassStatement>(debugName) {
    override fun createPsi(stub: TableGenClassStatementStub): TableGenClassStatement? {
        return TableGenClassStatementImpl(stub, this)
    }

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
