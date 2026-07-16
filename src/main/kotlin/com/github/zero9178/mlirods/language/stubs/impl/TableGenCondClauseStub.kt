package com.github.zero9178.mlirods.language.stubs.impl

import com.github.zero9178.mlirods.language.generated.psi.TableGenCondClause
import com.github.zero9178.mlirods.language.generated.psi.impl.TableGenCondClauseImpl
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.IStubElementType
import com.intellij.psi.stubs.StubBase
import com.intellij.psi.stubs.StubElement

/**
 * Stub interface for [TableGenCondClause].
 *
 * A cond clause consists purely of value nodes, all of which are stubbed themselves, and therefore needs to serialize
 * no data of its own.
 */
interface TableGenCondClauseStub : StubElement<TableGenCondClause>

private class TableGenCondClauseStubImpl(
    parent: StubElement<out PsiElement>?,
    elementType: IStubElementType<*, *>,
) : StubBase<TableGenCondClause>(
    parent, elementType
), TableGenCondClauseStub

class TableGenCondClauseStubElementType(debugName: String) :
    TableGenSingletonStubElementType<TableGenCondClauseStub, TableGenCondClause>(
        debugName, ::TableGenCondClauseImpl, ::TableGenCondClauseStubImpl
    ) {
    // The condition and value are stubbed value node children.
    override fun isAlwaysLeaf(root: StubBase<*>) = false
}
