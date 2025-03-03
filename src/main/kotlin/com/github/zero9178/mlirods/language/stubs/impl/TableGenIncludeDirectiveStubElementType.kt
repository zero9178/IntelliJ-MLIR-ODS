package com.github.zero9178.mlirods.language.stubs.impl

import com.github.zero9178.mlirods.language.generated.psi.TableGenIncludeDirective
import com.github.zero9178.mlirods.language.generated.psi.impl.TableGenIncludeDirectiveImpl
import com.github.zero9178.mlirods.language.stubs.TableGenStubElementType
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.IndexSink
import com.intellij.psi.stubs.StubBase
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream

class TableGenIncludeDirectiveStubElementType(debugName: String) :
    TableGenStubElementType<TableGenIncludeDirectiveStub, TableGenIncludeDirective>(
        debugName
    ) {
    override fun createStub(
        psi: TableGenIncludeDirective,
        parentStub: StubElement<out PsiElement?>?
    ): TableGenIncludeDirectiveStub = TableGenIncludeDirectiveStubImpl(psi.includeSuffix, parentStub)

    override fun createPsi(stub: TableGenIncludeDirectiveStub) = TableGenIncludeDirectiveImpl(stub, this)

    override fun serialize(
        stub: TableGenIncludeDirectiveStub,
        dataStream: StubOutputStream
    ) {
        dataStream.writeUTFFast(stub.includeSuffix)
    }

    override fun deserialize(
        dataStream: StubInputStream,
        parentStub: StubElement<*>?
    ): TableGenIncludeDirectiveStub {
        val includeSuffix = dataStream.readUTFFast()
        return TableGenIncludeDirectiveStubImpl(includeSuffix, parentStub)
    }

    override fun indexStub(
        stub: TableGenIncludeDirectiveStub,
        sink: IndexSink
    ) {
    }

    override fun isAlwaysLeaf(root: StubBase<*>) = true
}
