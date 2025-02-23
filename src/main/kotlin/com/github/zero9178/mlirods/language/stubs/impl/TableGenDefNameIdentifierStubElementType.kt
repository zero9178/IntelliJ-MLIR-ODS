package com.github.zero9178.mlirods.language.stubs.impl

import com.github.zero9178.mlirods.index.DEF_INDEX
import com.github.zero9178.mlirods.language.TableGenFile
import com.github.zero9178.mlirods.language.generated.TableGenTypes
import com.github.zero9178.mlirods.language.generated.psi.TableGenDefStatement
import com.github.zero9178.mlirods.language.generated.psi.impl.TableGenDefStatementImpl
import com.github.zero9178.mlirods.language.generated.psi.impl.TableGenDefvarStatementImpl
import com.github.zero9178.mlirods.language.psi.TableGenDefNameIdentifierOwner
import com.github.zero9178.mlirods.language.psi.TableGenIdentifierScopeNode
import com.github.zero9178.mlirods.language.stubs.TableGenStubElementType
import com.github.zero9178.mlirods.language.stubs.TableGenStubElementTypes.Companion.DEFVAR_STATEMENT
import com.github.zero9178.mlirods.language.stubs.TableGenStubElementTypes.Companion.DEF_STATEMENT
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.IndexSink
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.intellij.psi.util.parentsOfType

class TableGenDefNameIdentifierStubElementType(debugName: String) :
    TableGenStubElementType<TableGenDefNameIdentifierStub, TableGenDefNameIdentifierOwner>(debugName) {
    override fun createPsi(stub: TableGenDefNameIdentifierStub): TableGenDefNameIdentifierOwner? {
        return when {
            this === DEFVAR_STATEMENT -> TableGenDefvarStatementImpl(stub, this)
            this === DEF_STATEMENT -> TableGenDefStatementImpl(stub, this)
            else -> error("Unexpected stub type")
        }
    }

    override fun shouldCreateStub(node: ASTNode): Boolean {
        val psi = TableGenTypes.Factory.createElement(node)
        return when (psi) {
            // 'def' statements are always globally visible if they have a name.
            is TableGenDefStatement -> psi.name != null
            // Other elements are only visible if top-level.
            else -> psi.parentsOfType<TableGenIdentifierScopeNode>(withSelf = false).all { it is TableGenFile }
        }
    }

    override fun createStub(
        psi: TableGenDefNameIdentifierOwner, parentStub: StubElement<out PsiElement?>?
    ): TableGenDefNameIdentifierStub {
        // Name non-nullness guaranteed by [shouldCreateStub].
        return TableGenDefNameIdentifierStubImpl(psi.name!!, parentStub, this)
    }

    override fun serialize(
        stub: TableGenDefNameIdentifierStub, dataStream: StubOutputStream
    ) {
        dataStream.writeUTFFast(stub.name)
    }

    override fun deserialize(
        dataStream: StubInputStream, parentStub: StubElement<*>?
    ): TableGenDefNameIdentifierStub {
        return TableGenDefNameIdentifierStubImpl(dataStream.readUTFFast(), parentStub, this)
    }

    override fun indexStub(stub: TableGenDefNameIdentifierStub, sink: IndexSink) {
        sink.occurrence(DEF_INDEX, stub.name)
    }
}
