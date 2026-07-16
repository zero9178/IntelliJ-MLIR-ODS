package com.github.zero9178.mlirods.language.stubs.impl

import com.github.zero9178.mlirods.language.generated.psi.TableGenBitRange
import com.github.zero9178.mlirods.language.generated.psi.TableGenRangePiece
import com.github.zero9178.mlirods.language.generated.psi.TableGenSingleBit
import com.github.zero9178.mlirods.language.generated.psi.impl.TableGenBitRangeImpl
import com.github.zero9178.mlirods.language.generated.psi.impl.TableGenSingleBitImpl
import com.github.zero9178.mlirods.language.stubs.TableGenStubElementType
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.IStubElementType
import com.intellij.psi.stubs.StubBase
import com.intellij.psi.stubs.StubElement

/**
 * Super type of all stub [TableGenRangePiece] instances.
 */
sealed interface TableGenRangePieceStub : StubElement<TableGenRangePiece>

/**
 * Stub implementation for any range piece. A range piece consists purely of value nodes, all of which are stubbed
 * themselves, and therefore needs to serialize no data of its own.
 */
private class TableGenRangePieceStubImpl(
    parent: StubElement<out PsiElement>?,
    elementType: IStubElementType<*, *>,
) : StubBase<TableGenRangePiece>(
    parent, elementType
), TableGenRangePieceStub

sealed class TableGenAbstractRangePieceStubElementType<PsiT : PsiElement>(
    debugName: String,
    psiConstructor: (TableGenRangePieceStub, TableGenStubElementType<TableGenRangePieceStub, PsiT>) -> PsiT
) : TableGenSingletonStubElementType<TableGenRangePieceStub, PsiT>(
    debugName, psiConstructor, ::TableGenRangePieceStubImpl
) {
    // Range pieces contain the value nodes making up the bit indices as children.
    override fun isAlwaysLeaf(root: StubBase<*>) = false
}

class TableGenSingleBitStubElementType(debugName: String) :
    TableGenAbstractRangePieceStubElementType<TableGenSingleBit>(
        debugName, ::TableGenSingleBitImpl
    )

class TableGenBitRangeStubElementType(debugName: String) :
    TableGenAbstractRangePieceStubElementType<TableGenBitRange>(
        debugName, ::TableGenBitRangeImpl
    )
