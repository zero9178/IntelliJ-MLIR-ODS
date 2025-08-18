package com.github.zero9178.mlirods.language.stubs.impl

import com.github.zero9178.mlirods.language.generated.psi.TableGenForeachStatement
import com.github.zero9178.mlirods.language.generated.psi.TableGenIfStatement
import com.github.zero9178.mlirods.language.generated.psi.TableGenLetStatement
import com.github.zero9178.mlirods.language.generated.psi.impl.TableGenForeachStatementImpl
import com.github.zero9178.mlirods.language.generated.psi.impl.TableGenIfStatementImpl
import com.github.zero9178.mlirods.language.generated.psi.impl.TableGenLetStatementImpl
import com.github.zero9178.mlirods.language.psi.TableGenIdentifierScopeNode
import com.github.zero9178.mlirods.language.stubs.TableGenStubElementType
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.IStubElementType
import com.intellij.psi.stubs.StubBase
import com.intellij.psi.stubs.StubElement

sealed interface TableGenStatementStub : StubElement<TableGenIdentifierScopeNode>


private class TableGenStatementStubImpl(
    parent: StubElement<out PsiElement>?,
    elementType: IStubElementType<*, *>,
) : StubBase<TableGenIdentifierScopeNode>(
    parent, elementType
), TableGenStatementStub

sealed class TableGenAbstractStatementStubElementType<PsiT : PsiElement>(
    debugName: String,
    psiConstructor: (TableGenStatementStub, TableGenStubElementType<TableGenStatementStub, PsiT>) -> PsiT
) : TableGenSingletonStubElementType<TableGenStatementStub, PsiT>(
    debugName, psiConstructor, ::TableGenStatementStubImpl
) {
    override fun isAlwaysLeaf(root: StubBase<*>) = false
}

class TableGenForeachStatementStubElementType(debugName: String) :
    TableGenAbstractStatementStubElementType<TableGenForeachStatement>(
        debugName, ::TableGenForeachStatementImpl
    )

class TableGenIfStatementStubElementType(debugName: String) :
    TableGenAbstractStatementStubElementType<TableGenIfStatement>(
        debugName, ::TableGenIfStatementImpl
    )

class TableGenLetStatementStubElementType(debugName: String) :
    TableGenAbstractStatementStubElementType<TableGenLetStatement>(
        debugName, ::TableGenLetStatementImpl
    )
