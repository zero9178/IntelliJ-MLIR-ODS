package com.github.zero9178.mlirods.language.stubs.impl

import com.github.zero9178.mlirods.language.generated.psi.impl.TableGenDefineDirectiveImpl
import com.github.zero9178.mlirods.language.generated.psi.impl.TableGenIfdefIfndefDirectiveImpl
import com.github.zero9178.mlirods.language.psi.impl.TableGenMacroDirectiveEx
import com.github.zero9178.mlirods.language.stubs.TableGenStubElementType
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.*

/**
 * Stub interface for the preprocessor directives carrying a macro name ('#define', '#ifdef' and '#ifndef').
 */
sealed interface TableGenMacroDirectiveStub : StubElement<TableGenMacroDirectiveEx> {
    /**
     * Name of the macro defined or tested by the directive.
     */
    val macroName: String?
}

abstract class TableGenAbstractMacroDirectiveStubElementType(
    debugName: String,
    constructor: (TableGenMacroDirectiveStub, IStubElementType<*, *>) -> TableGenMacroDirectiveEx
) : TableGenStubElementType<TableGenMacroDirectiveStub, TableGenMacroDirectiveEx>(debugName, constructor) {

    override fun createStub(
        psi: TableGenMacroDirectiveEx, parentStub: StubElement<out PsiElement?>?
    ): TableGenMacroDirectiveStub = TableGenMacroDirectiveStubImpl(psi.macroName, parentStub, this)

    override fun serialize(
        stub: TableGenMacroDirectiveStub, dataStream: StubOutputStream
    ) {
        dataStream.writeName(stub.macroName)
    }

    override fun deserialize(
        dataStream: StubInputStream, parentStub: StubElement<*>?
    ): TableGenMacroDirectiveStub = TableGenMacroDirectiveStubImpl(dataStream.readNameString(), parentStub, this)

    override fun isAlwaysLeaf(root: StubBase<*>) = true
}

private class TableGenMacroDirectiveStubImpl(
    override val macroName: String?,
    parent: StubElement<out PsiElement>?,
    elementType: TableGenAbstractMacroDirectiveStubElementType,
) : StubBase<TableGenMacroDirectiveEx>(parent, elementType), TableGenMacroDirectiveStub

class TableGenDefineDirectiveStubElementType(debugName: String) : TableGenAbstractMacroDirectiveStubElementType(
    debugName, ::TableGenDefineDirectiveImpl
)

class TableGenIfdefIfndefDirectiveStubElementType(debugName: String) : TableGenAbstractMacroDirectiveStubElementType(
    debugName, ::TableGenIfdefIfndefDirectiveImpl
)
