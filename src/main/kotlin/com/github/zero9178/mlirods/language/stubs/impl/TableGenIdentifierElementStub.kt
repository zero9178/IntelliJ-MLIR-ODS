package com.github.zero9178.mlirods.language.stubs.impl

import com.github.zero9178.mlirods.index.IDENTIFIER_INDEX
import com.github.zero9178.mlirods.language.psi.TableGenFile
import com.github.zero9178.mlirods.language.generated.TableGenTypes
import com.github.zero9178.mlirods.language.generated.psi.TableGenDefStatement
import com.github.zero9178.mlirods.language.generated.psi.impl.TableGenDefStatementImpl
import com.github.zero9178.mlirods.language.generated.psi.impl.TableGenDefvarStatementImpl
import com.github.zero9178.mlirods.language.psi.TableGenIdentifierElement
import com.github.zero9178.mlirods.language.psi.TableGenIdentifierScopeNode
import com.github.zero9178.mlirods.language.stubs.TableGenStubElementType
import com.github.zero9178.mlirods.language.stubs.TableGenStubElementTypes.Companion.DEFVAR_STATEMENT
import com.github.zero9178.mlirods.language.stubs.TableGenStubElementTypes.Companion.DEF_STATEMENT
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.IndexSink
import com.intellij.psi.stubs.StubBase
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.intellij.psi.util.parentsOfType

/**
 * Stub interface for [TableGenIdentifierElement] elements.
 */
interface TableGenIdentifierElementStub : StubElement<TableGenIdentifierElement> {
    val name: String
}


class TableGenIdentifierElementStubElementType(debugName: String) :
    TableGenStubElementType<TableGenIdentifierElementStub, TableGenIdentifierElement>(
        debugName,
        { stub, elementType ->
            when {
                elementType === DEFVAR_STATEMENT -> TableGenDefvarStatementImpl(stub, elementType)
                elementType === DEF_STATEMENT -> TableGenDefStatementImpl(stub, elementType)
                else -> error("Unexpected stub type")
            }
        }) {

    override fun shouldCreateStub(node: ASTNode): Boolean {
        val psi = TableGenTypes.Factory.createElement(node)
        if (psi !is TableGenIdentifierElement || psi.name == null) return false

        // Def statements are always indexed.
        if (psi is TableGenDefStatement) return true

        return psi.parentsOfType<TableGenIdentifierScopeNode>(withSelf = false).all { it is TableGenFile }
    }

    override fun createStub(
        psi: TableGenIdentifierElement, parentStub: StubElement<out PsiElement?>?
    ): TableGenIdentifierElementStub {
        // Name non-nullness guaranteed by [shouldCreateStub].
        return TableGenIdentifierElementStubImpl(psi.name!!, parentStub, this)
    }

    override fun serialize(
        stub: TableGenIdentifierElementStub, dataStream: StubOutputStream
    ) {
        dataStream.writeUTFFast(stub.name)
    }

    override fun deserialize(
        dataStream: StubInputStream, parentStub: StubElement<*>?
    ): TableGenIdentifierElementStub {
        return TableGenIdentifierElementStubImpl(dataStream.readUTFFast(), parentStub, this)
    }

    override fun indexStub(stub: TableGenIdentifierElementStub, sink: IndexSink) {
        sink.occurrence(IDENTIFIER_INDEX, stub.name)
    }
}

private class TableGenIdentifierElementStubImpl(
    override val name: String,
    parent: StubElement<out PsiElement>?,
    elementType: TableGenIdentifierElementStubElementType,
) : StubBase<TableGenIdentifierElement>(
    parent, elementType
), TableGenIdentifierElementStub
