package com.github.zero9178.mlirods.language.stubs.impl

import com.github.zero9178.mlirods.language.generated.psi.TableGenTemplateArgDecl
import com.github.zero9178.mlirods.language.generated.psi.impl.TableGenTemplateArgDeclImpl
import com.github.zero9178.mlirods.language.stubs.TableGenStubElementType
import com.github.zero9178.mlirods.language.stubs.TableGenStubElementTypes
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.StubBase
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream

/**
 * Stub interface for [TableGenTemplateArgDecl].
 */
interface TableGenTemplateArgDeclStub : StubElement<TableGenTemplateArgDecl> {
    val name: String?
    val hasDefault: Boolean
}

class TableGenTemplateArgDeclStubElementType(debugName: String) :
    TableGenStubElementType<TableGenTemplateArgDeclStub, TableGenTemplateArgDecl>(
        debugName, ::TableGenTemplateArgDeclImpl
    ) {

    override fun createStub(
        psi: TableGenTemplateArgDecl, parentStub: StubElement<out PsiElement?>?
    ): TableGenTemplateArgDeclStub {
        return TableGenTemplateArgDeclStubImpl(psi.name, psi.valueNode != null, parentStub)
    }

    override fun serialize(
        stub: TableGenTemplateArgDeclStub, dataStream: StubOutputStream
    ) {
        dataStream.writeBoolean(stub.name != null)
        stub.name?.let { dataStream.writeUTFFast(it) }
        dataStream.writeBoolean(stub.hasDefault)
    }

    override fun deserialize(
        dataStream: StubInputStream, parentStub: StubElement<*>?
    ): TableGenTemplateArgDeclStub {
        val hasName = dataStream.readBoolean()
        val name = if (hasName) {
            dataStream.readUTFFast()
        } else null
        val hasDefault = dataStream.readBoolean()
        return TableGenTemplateArgDeclStubImpl(name, hasDefault, parentStub)
    }
}

private class TableGenTemplateArgDeclStubImpl(
    override val name: String?, override val hasDefault: Boolean, parent: StubElement<out PsiElement>?
) : StubBase<TableGenTemplateArgDecl>(
    parent, TableGenStubElementTypes.TEMPLATE_ARG_DECL
), TableGenTemplateArgDeclStub
