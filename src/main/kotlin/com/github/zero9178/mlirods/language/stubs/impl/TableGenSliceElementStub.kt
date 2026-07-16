package com.github.zero9178.mlirods.language.stubs.impl

import com.github.zero9178.mlirods.language.generated.psi.TableGenSingleSliceElement
import com.github.zero9178.mlirods.language.generated.psi.TableGenSliceElement
import com.github.zero9178.mlirods.language.generated.psi.TableGenSliceElementRange
import com.github.zero9178.mlirods.language.generated.psi.impl.TableGenSingleSliceElementImpl
import com.github.zero9178.mlirods.language.generated.psi.impl.TableGenSliceElementRangeImpl
import com.github.zero9178.mlirods.language.stubs.TableGenStubElementType
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.IStubElementType
import com.intellij.psi.stubs.StubBase
import com.intellij.psi.stubs.StubElement

/**
 * Super type of all stub [TableGenSliceElement] instances.
 */
sealed interface TableGenSliceElementStub : StubElement<TableGenSliceElement>

/**
 * Stub implementation for any slice element. A slice element consists purely of value nodes, all of which are stubbed
 * themselves, and therefore needs to serialize no data of its own.
 */
private class TableGenSliceElementStubImpl(
    parent: StubElement<out PsiElement>?,
    elementType: IStubElementType<*, *>,
) : StubBase<TableGenSliceElement>(
    parent, elementType
), TableGenSliceElementStub

sealed class TableGenAbstractSliceElementStubElementType<PsiT : PsiElement>(
    debugName: String,
    psiConstructor: (TableGenSliceElementStub, TableGenStubElementType<TableGenSliceElementStub, PsiT>) -> PsiT
) : TableGenSingletonStubElementType<TableGenSliceElementStub, PsiT>(
    debugName, psiConstructor, ::TableGenSliceElementStubImpl
) {
    // Slice elements contain the value nodes making up the indices as children.
    override fun isAlwaysLeaf(root: StubBase<*>) = false
}

class TableGenSingleSliceElementStubElementType(debugName: String) :
    TableGenAbstractSliceElementStubElementType<TableGenSingleSliceElement>(
        debugName, ::TableGenSingleSliceElementImpl
    )

class TableGenSliceElementRangeStubElementType(debugName: String) :
    TableGenAbstractSliceElementStubElementType<TableGenSliceElementRange>(
        debugName, ::TableGenSliceElementRangeImpl
    )
