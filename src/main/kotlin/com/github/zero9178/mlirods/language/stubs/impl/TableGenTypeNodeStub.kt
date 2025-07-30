package com.github.zero9178.mlirods.language.stubs.impl

import com.github.zero9178.mlirods.language.generated.psi.*
import com.github.zero9178.mlirods.language.generated.psi.impl.*
import com.github.zero9178.mlirods.language.psi.impl.TableGenPsiImplUtil.Companion.getIntegerValue
import com.github.zero9178.mlirods.language.stubs.TableGenStubElementType
import com.github.zero9178.mlirods.language.stubs.TableGenStubElementTypes
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.*

/**
 * Super type of all stub [TableGenTypeNode] instances.
 */
sealed interface TableGenTypeNodeStub : StubElement<TableGenTypeNode>

/**
 * Base class for file element types for any Psi class that does not need to extend [TableGenTypeNodeStub].
 */
sealed class TableGenAbstractTypeNodeStubElementType<PsiT : PsiElement>(
    debugName: String,
    psiConstructor: (TableGenTypeNodeStub, TableGenStubElementType<TableGenTypeNodeStub, PsiT>) -> PsiT
) : TableGenSingletonStubElementType<TableGenTypeNodeStub, PsiT>(
    debugName, psiConstructor, ::TableGenTypeNodeStubImpl
) {
    override fun isAlwaysLeaf(root: StubBase<*>) = true
}

class TableGenBitTypeNodeStubElementType(debugName: String) :
    TableGenAbstractTypeNodeStubElementType<TableGenBitTypeNode>(
        debugName, ::TableGenBitTypeNodeImpl
    )

class TableGenIntTypeNodeStubElementType(debugName: String) :
    TableGenAbstractTypeNodeStubElementType<TableGenIntTypeNode>(
        debugName, ::TableGenIntTypeNodeImpl
    )

class TableGenStringTypeNodeStubElementType(debugName: String) :
    TableGenAbstractTypeNodeStubElementType<TableGenStringTypeNode>(
        debugName, ::TableGenStringTypeNodeImpl
    )

class TableGenDagTypeNodeStubElementType(debugName: String) :
    TableGenAbstractTypeNodeStubElementType<TableGenDagTypeNode>(
        debugName, ::TableGenDagTypeNodeImpl
    )

class TableGenCodeTypeNodeStubElementType(debugName: String) :
    TableGenAbstractTypeNodeStubElementType<TableGenCodeTypeNode>(
        debugName, ::TableGenCodeTypeNodeImpl
    )

/**
 * Stub interface of [TableGenBitsTypeNode].
 */
interface TableGenBitsTypeNodeStub : TableGenTypeNodeStub {
    val bits: Long?
}

class TableGenBitsTypeNodeStubElementType(
    debugName: String
) : TableGenStubElementType<TableGenBitsTypeNodeStub, TableGenBitsTypeNode>(
    debugName, ::TableGenBitsTypeNodeImpl
) {
    override fun createStub(
        psi: TableGenBitsTypeNode, parentStub: StubElement<out PsiElement?>?
    ): TableGenBitsTypeNodeStub {
        return TableGenBitsTypeNodeStubImpl(psi.integer?.let { getIntegerValue(it) }, parentStub)
    }

    override fun serialize(stub: TableGenBitsTypeNodeStub, dataStream: StubOutputStream) {
        dataStream.writeLong(stub.bits ?: -1)
    }

    override fun deserialize(
        dataStream: StubInputStream, parentStub: StubElement<*>?
    ): TableGenBitsTypeNodeStub {
        val i = dataStream.readLong()
        return when (i) {
            -1L -> TableGenBitsTypeNodeStubImpl(null, parentStub)
            else -> TableGenBitsTypeNodeStubImpl(i, parentStub)
        }
    }

    override fun isAlwaysLeaf(root: StubBase<*>) = true
}

private class TableGenBitsTypeNodeStubImpl(
    override val bits: Long?, parent: StubElement<out PsiElement>?
) : StubBase<TableGenTypeNode>(
    parent, TableGenStubElementTypes.BITS_TYPE_NODE
), TableGenBitsTypeNodeStub

class TableGenListTypeNodeStubElementType(debugName: String) :
    TableGenAbstractTypeNodeStubElementType<TableGenListTypeNode>(
        debugName, ::TableGenListTypeNodeImpl
    ) {
    override fun isAlwaysLeaf(root: StubBase<*>) = false
}

/**
 * Stub interface of [TableGenClassTypeNode].
 */
interface TableGenClassTypeNodeStub : TableGenTypeNodeStub {
    val className: String
}

class TableGenClassTypeNodeStubElementType(
    debugName: String
) : TableGenStubElementType<TableGenClassTypeNodeStub, TableGenClassTypeNode>(
    debugName, ::TableGenClassTypeNodeImpl
) {
    override fun createStub(
        psi: TableGenClassTypeNode, parentStub: StubElement<out PsiElement?>?
    ): TableGenClassTypeNodeStub {
        return TableGenClassTypeNodeStubImpl(psi.classIdentifier.text, parentStub)
    }

    override fun serialize(stub: TableGenClassTypeNodeStub, dataStream: StubOutputStream) {
        dataStream.writeUTFFast(stub.className)
    }

    override fun deserialize(
        dataStream: StubInputStream, parentStub: StubElement<*>?
    ): TableGenClassTypeNodeStub = TableGenClassTypeNodeStubImpl(dataStream.readUTFFast(), parentStub)

    override fun isAlwaysLeaf(root: StubBase<*>) = true
}

private class TableGenClassTypeNodeStubImpl(
    override val className: String, parent: StubElement<out PsiElement>?
) : StubBase<TableGenTypeNode>(
    parent, TableGenStubElementTypes.CLASS_TYPE_NODE
), TableGenClassTypeNodeStub

/**
 * Stub node implementation for any class that does not need to extend [TableGenTypeNode].
 */
private class TableGenTypeNodeStubImpl(
    parent: StubElement<out PsiElement>?,
    elementType: IStubElementType<*, *>,
) : StubBase<TableGenTypeNode>(
    parent, elementType
), TableGenTypeNodeStub
