package com.github.zero9178.mlirods.language.stubs.impl

import com.github.zero9178.mlirods.language.generated.psi.TableGenIncludeDirective
import com.github.zero9178.mlirods.language.generated.psi.impl.TableGenIncludeDirectiveImpl
import com.github.zero9178.mlirods.language.stubs.TableGenStubElementType
import com.github.zero9178.mlirods.language.stubs.TableGenStubElementTypes
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.IndexSink
import com.intellij.psi.stubs.StubBase
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream

/**
 * Interface for include stubs.
 */
interface TableGenIncludeDirectiveStub : StubElement<TableGenIncludeDirective> {
    val includeSuffix: String
}

class TableGenIncludeDirectiveStubElementType(debugName: String) :
    TableGenStubElementType<TableGenIncludeDirectiveStub, TableGenIncludeDirective>(
        debugName, ::TableGenIncludeDirectiveImpl
    ) {
    override fun createStub(
        psi: TableGenIncludeDirective,
        parentStub: StubElement<out PsiElement?>?
    ): TableGenIncludeDirectiveStub = TableGenIncludeDirectiveStubImpl(psi.includeSuffix, parentStub)

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

    override fun isAlwaysLeaf(root: StubBase<*>) = true
}

private class TableGenIncludeDirectiveStubImpl(
    override val includeSuffix: String,
    parent: StubElement<out PsiElement>?
) :
    StubBase<TableGenIncludeDirective>(
        parent, TableGenStubElementTypes.INCLUDE_DIRECTIVE
    ), TableGenIncludeDirectiveStub

