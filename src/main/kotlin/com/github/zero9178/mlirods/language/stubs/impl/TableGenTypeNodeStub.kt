package com.github.zero9178.mlirods.language.stubs.impl

import com.github.zero9178.mlirods.language.generated.psi.TableGenBitTypeNode
import com.github.zero9178.mlirods.language.generated.psi.TableGenBitsTypeNode
import com.github.zero9178.mlirods.language.generated.psi.TableGenClassTypeNode
import com.github.zero9178.mlirods.language.generated.psi.TableGenCodeTypeNode
import com.github.zero9178.mlirods.language.generated.psi.TableGenDagTypeNode
import com.github.zero9178.mlirods.language.generated.psi.TableGenIntTypeNode
import com.github.zero9178.mlirods.language.generated.psi.TableGenListTypeNode
import com.github.zero9178.mlirods.language.generated.psi.TableGenStringTypeNode
import com.github.zero9178.mlirods.language.generated.psi.TableGenTypeNode
import com.github.zero9178.mlirods.language.generated.psi.impl.TableGenBitTypeNodeImpl
import com.github.zero9178.mlirods.language.generated.psi.impl.TableGenBitsTypeNodeImpl
import com.github.zero9178.mlirods.language.generated.psi.impl.TableGenClassTypeNodeImpl
import com.github.zero9178.mlirods.language.generated.psi.impl.TableGenCodeTypeNodeImpl
import com.github.zero9178.mlirods.language.generated.psi.impl.TableGenDagTypeNodeImpl
import com.github.zero9178.mlirods.language.generated.psi.impl.TableGenIntTypeNodeImpl
import com.github.zero9178.mlirods.language.generated.psi.impl.TableGenListTypeNodeImpl
import com.github.zero9178.mlirods.language.generated.psi.impl.TableGenStringTypeNodeImpl
import com.github.zero9178.mlirods.language.psi.impl.TableGenPsiImplUtil.Companion.getIntegerValue
import com.github.zero9178.mlirods.language.stubs.TableGenStubElementType
import com.github.zero9178.mlirods.language.stubs.TableGenStubElementTypes
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.StubBase
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream

/**
 * Super type of all stub [TableGenTypeNode] instances.
 */
sealed interface TableGenTypeNodeStub : StubElement<TableGenTypeNode>

/**
 * Abstract base class of stub file types which do not contain any data.
 */
abstract class TableGenSingletonTypeNodeStubElementType<StubT : StubElement<*>, PsiT : PsiElement>(
    debugName: String,
    psiConstructor: (StubT, TableGenStubElementType<StubT, PsiT>) -> PsiT,
    private val myStubConstructor: (StubElement<*>?) -> StubT
) : TableGenStubElementType<StubT, PsiT>(
    debugName, psiConstructor
) {
    final override fun createStub(
        psi: PsiT, parentStub: StubElement<out PsiElement?>?
    ): StubT {
        return myStubConstructor.invoke(parentStub)
    }

    final override fun serialize(stub: StubT, dataStream: StubOutputStream) {}

    final override fun deserialize(
        dataStream: StubInputStream, parentStub: StubElement<*>?
    ): StubT {
        return myStubConstructor.invoke(parentStub)
    }

    override fun isAlwaysLeaf(root: StubBase<*>) = true
}

/**
 * Stub interface of [TableGenBitTypeNode].
 */
interface TableGenBitTypeNodeStub : TableGenTypeNodeStub

class TableGenBitTypeNodeStubElementType(debugName: String) :
    TableGenSingletonTypeNodeStubElementType<TableGenBitTypeNodeStub, TableGenBitTypeNode>(
        debugName, ::TableGenBitTypeNodeImpl, ::TableGenBitTypeNodeStubImpl
    )

private class TableGenBitTypeNodeStubImpl(
    parent: StubElement<out PsiElement>?
) : StubBase<TableGenTypeNode>(
    parent, TableGenStubElementTypes.BIT_TYPE_NODE
), TableGenBitTypeNodeStub

/**
 * Stub interface of [TableGenIntTypeNode].
 */
interface TableGenIntTypeNodeStub : TableGenTypeNodeStub

class TableGenIntTypeNodeStubElementType(debugName: String) :
    TableGenSingletonTypeNodeStubElementType<TableGenIntTypeNodeStub, TableGenIntTypeNode>(
        debugName, ::TableGenIntTypeNodeImpl, ::TableGenIntTypeNodeStubImpl
    )

private class TableGenIntTypeNodeStubImpl(
    parent: StubElement<out PsiElement>?
) : StubBase<TableGenTypeNode>(
    parent, TableGenStubElementTypes.INT_TYPE_NODE
), TableGenIntTypeNodeStub

/**
 * Stub interface of [TableGenStringTypeNode].
 */
interface TableGenStringTypeNodeStub : TableGenTypeNodeStub

class TableGenStringTypeNodeStubElementType(debugName: String) :
    TableGenSingletonTypeNodeStubElementType<TableGenStringTypeNodeStub, TableGenStringTypeNode>(
        debugName, ::TableGenStringTypeNodeImpl, ::TableGenStringTypeNodeStubImpl
    )

private class TableGenStringTypeNodeStubImpl(
    parent: StubElement<out PsiElement>?
) : StubBase<TableGenTypeNode>(
    parent, TableGenStubElementTypes.STRING_TYPE_NODE
), TableGenStringTypeNodeStub


/**
 * Stub interface of [TableGenDagTypeNode].
 */
interface TableGenDagTypeNodeStub : TableGenTypeNodeStub

class TableGenDagTypeNodeStubElementType(debugName: String) :
    TableGenSingletonTypeNodeStubElementType<TableGenDagTypeNodeStub, TableGenDagTypeNode>(
        debugName, ::TableGenDagTypeNodeImpl, ::TableGenDagTypeNodeStubImpl
    )

private class TableGenDagTypeNodeStubImpl(
    parent: StubElement<out PsiElement>?
) : StubBase<TableGenTypeNode>(
    parent, TableGenStubElementTypes.DAG_TYPE_NODE
), TableGenDagTypeNodeStub

/**
 * Stub interface of [TableGenCodeTypeNode].
 */
interface TableGenCodeTypeNodeStub : TableGenTypeNodeStub

class TableGenCodeTypeNodeStubElementType(debugName: String) :
    TableGenSingletonTypeNodeStubElementType<TableGenCodeTypeNodeStub, TableGenCodeTypeNode>(
        debugName, ::TableGenCodeTypeNodeImpl, ::TableGenCodeTypeNodeStubImpl
    )

private class TableGenCodeTypeNodeStubImpl(
    parent: StubElement<out PsiElement>?
) : StubBase<TableGenTypeNode>(
    parent, TableGenStubElementTypes.CODE_TYPE_NODE
), TableGenCodeTypeNodeStub

/**
 * Stub interface of [TableGenBitsTypeNode].
 */
interface TableGenBitsTypeNodeStub : TableGenTypeNodeStub {
    val bits: Int?
}

class TableGenBitsTypeNodeStubElementType(
    debugName: String
) : TableGenStubElementType<TableGenBitsTypeNodeStub, TableGenBitsTypeNode>(
    debugName, ::TableGenBitsTypeNodeImpl
) {
    override fun createStub(
        psi: TableGenBitsTypeNode, parentStub: StubElement<out PsiElement?>?
    ): TableGenBitsTypeNodeStub {
        return TableGenBitsTypeNodeStubImpl(getIntegerValue(psi.integer), parentStub)
    }

    override fun serialize(stub: TableGenBitsTypeNodeStub, dataStream: StubOutputStream) {
        dataStream.writeInt(stub.bits ?: -1)
    }

    override fun deserialize(
        dataStream: StubInputStream, parentStub: StubElement<*>?
    ): TableGenBitsTypeNodeStub {
        val i = dataStream.readInt()
        return when (i) {
            -1 -> TableGenBitsTypeNodeStubImpl(null, parentStub)
            else -> TableGenBitsTypeNodeStubImpl(i, parentStub)
        }
    }

    override fun isAlwaysLeaf(root: StubBase<*>) = true
}

private class TableGenBitsTypeNodeStubImpl(
    override val bits: Int?, parent: StubElement<out PsiElement>?
) : StubBase<TableGenTypeNode>(
    parent, TableGenStubElementTypes.BITS_TYPE_NODE
), TableGenBitsTypeNodeStub

/**
 * Stub interface of [TableGenListTypeNode].
 */
interface TableGenListTypeNodeStub : TableGenTypeNodeStub

class TableGenListTypeNodeStubElementType(debugName: String) :
    TableGenSingletonTypeNodeStubElementType<TableGenListTypeNodeStub, TableGenListTypeNode>(
        debugName, ::TableGenListTypeNodeImpl, ::TableGenListTypeNodeStubImpl
    ) {
    override fun isAlwaysLeaf(root: StubBase<*>) = false
}

private class TableGenListTypeNodeStubImpl(
    parent: StubElement<out PsiElement>?
) : StubBase<TableGenTypeNode>(
    parent, TableGenStubElementTypes.LIST_TYPE_NODE
), TableGenListTypeNodeStub


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