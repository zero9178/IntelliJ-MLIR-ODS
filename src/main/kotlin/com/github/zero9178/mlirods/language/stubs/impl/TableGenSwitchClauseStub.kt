package com.github.zero9178.mlirods.language.stubs.impl

import com.github.zero9178.mlirods.language.generated.psi.TableGenSwitchClause
import com.github.zero9178.mlirods.language.generated.psi.impl.TableGenSwitchClauseImpl
import com.github.zero9178.mlirods.language.stubs.TableGenStubElementType
import com.github.zero9178.mlirods.language.stubs.TableGenStubElementTypes
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.StubBase
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream

/**
 * Stub interface for [TableGenSwitchClause].
 */
interface TableGenSwitchClauseStub : StubElement<TableGenSwitchClause> {
    /**
     * See [com.github.zero9178.mlirods.language.psi.impl.TableGenSwitchClauseEx.hasColon].
     * Stored as it is otherwise only derivable from the tokens in the AST.
     */
    val hasColon: Boolean
}

class TableGenSwitchClauseStubElementType(debugName: String) :
    TableGenStubElementType<TableGenSwitchClauseStub, TableGenSwitchClause>(
        debugName, ::TableGenSwitchClauseImpl
    ) {

    override fun createStub(
        psi: TableGenSwitchClause, parentStub: StubElement<out PsiElement?>?
    ): TableGenSwitchClauseStub = TableGenSwitchClauseStubImpl(psi.hasColon, parentStub)

    override fun serialize(stub: TableGenSwitchClauseStub, dataStream: StubOutputStream) {
        dataStream.writeBoolean(stub.hasColon)
    }

    override fun deserialize(
        dataStream: StubInputStream, parentStub: StubElement<*>?
    ): TableGenSwitchClauseStub = TableGenSwitchClauseStubImpl(dataStream.readBoolean(), parentStub)

    // The case key and value are stubbed value node children.
    override fun isAlwaysLeaf(root: StubBase<*>) = false
}

private class TableGenSwitchClauseStubImpl(
    override val hasColon: Boolean, parent: StubElement<out PsiElement>?
) : StubBase<TableGenSwitchClause>(
    parent, TableGenStubElementTypes.SWITCH_CLAUSE
), TableGenSwitchClauseStub
