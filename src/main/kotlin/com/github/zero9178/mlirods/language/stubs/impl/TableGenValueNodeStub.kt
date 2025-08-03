package com.github.zero9178.mlirods.language.stubs.impl

import com.github.zero9178.mlirods.language.generated.psi.TableGenBitsInitValueNode
import com.github.zero9178.mlirods.language.generated.psi.TableGenConcatValueNode
import com.github.zero9178.mlirods.language.generated.psi.TableGenIdentifierValueNode
import com.github.zero9178.mlirods.language.generated.psi.TableGenIntegerValueNode
import com.github.zero9178.mlirods.language.generated.psi.TableGenListInitValueNode
import com.github.zero9178.mlirods.language.generated.psi.TableGenStringValueNode
import com.github.zero9178.mlirods.language.generated.psi.TableGenTypeNode
import com.github.zero9178.mlirods.language.generated.psi.TableGenUndefValueNode
import com.github.zero9178.mlirods.language.generated.psi.TableGenValueNode
import com.github.zero9178.mlirods.language.generated.psi.impl.TableGenBitsInitValueNodeImpl
import com.github.zero9178.mlirods.language.generated.psi.impl.TableGenConcatValueNodeImpl
import com.github.zero9178.mlirods.language.generated.psi.impl.TableGenIntegerValueNodeImpl
import com.github.zero9178.mlirods.language.generated.psi.impl.TableGenListInitValueNodeImpl
import com.github.zero9178.mlirods.language.generated.psi.impl.TableGenUndefValueNodeImpl
import com.github.zero9178.mlirods.language.generated.psi.impl.TableGenIdentifierValueNodeImpl
import com.github.zero9178.mlirods.language.generated.psi.impl.TableGenStringValueNodeImpl
import com.github.zero9178.mlirods.language.stubs.TableGenStubElementType
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.IStubElementType
import com.intellij.psi.stubs.StubBase
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream

/**
 * Super type of all stub [TableGenTypeNode] instances.
 */
sealed interface TableGenValueNodeStub : StubElement<TableGenValueNode>

/**
 * Stub implementation for any stub element type that does not need to serialize any data.
 */
private class TableGenValueNodeStubImpl(
    parent: StubElement<out PsiElement>?,
    elementType: IStubElementType<*, *>,
) : StubBase<TableGenValueNode>(
    parent, elementType
), TableGenValueNodeStub

/**
 * Abstract base class for any stub element type that does not need to serialize any data.
 */
sealed class TableGenAbstractValueNodeStubElementType<PsiT : PsiElement>(
    debugName: String,
    psiConstructor: (TableGenValueNodeStub, TableGenStubElementType<TableGenValueNodeStub, PsiT>) -> PsiT
) : TableGenSingletonStubElementType<TableGenValueNodeStub, PsiT>(
    debugName, psiConstructor, ::TableGenValueNodeStubImpl
) {
    override fun isAlwaysLeaf(root: StubBase<*>) = false
}

//class TableGenConcatValueNodeStubElementType(debugName: String) :
//    TableGenAbstractValueNodeStubElementType<TableGenConcatValueNode>(
//        debugName, ::TableGenConcatValueNodeImpl
//    )
//
//class TableGenUndefValueNodeStubElementType(debugName: String) :
//    TableGenAbstractValueNodeStubElementType<TableGenUndefValueNode>(
//        debugName, ::TableGenUndefValueNodeImpl
//    )
//
//class TableGenBitsInitValueNodeStubElementType(debugName: String) :
//    TableGenAbstractValueNodeStubElementType<TableGenBitsInitValueNode>(
//        debugName, ::TableGenBitsInitValueNodeImpl
//    )
//
//class TableGenListInitValueNodeStubElementType(debugName: String) :
//    TableGenAbstractValueNodeStubElementType<TableGenListInitValueNode>(
//        debugName, ::TableGenListInitValueNodeImpl
//    )

/**
 * Stub interface for [TableGenIntegerValueNode].
 */
interface TableGenIntegerValueNodeStub : TableGenValueNodeStub {
    val value: Long?
}

class TableGenIntegerValueNodeStubElementType(
    debugName: String
) : TableGenStubElementType<TableGenIntegerValueNodeStub, TableGenIntegerValueNode>(
    debugName, ::TableGenIntegerValueNodeImpl,
) {
    override fun createStub(
        psi: TableGenIntegerValueNode, parentStub: StubElement<out PsiElement?>?
    ): TableGenIntegerValueNodeStub {
        return TableGenIntegerValueNodeStubImpl(psi.evaluateAtomic()?.value, parentStub, this)
    }

    override fun serialize(
        stub: TableGenIntegerValueNodeStub, dataStream: StubOutputStream
    ) {
        dataStream.writeBoolean(stub.value != null)
        stub.value?.let { dataStream.writeLong(it) }
    }

    override fun deserialize(
        dataStream: StubInputStream, parentStub: StubElement<*>?
    ): TableGenIntegerValueNodeStub {
        return TableGenIntegerValueNodeStubImpl(
            if (dataStream.readBoolean()) dataStream.readLong() else null, parentStub, this
        )
    }

    override fun isAlwaysLeaf(root: StubBase<*>) = true
}

private class TableGenIntegerValueNodeStubImpl(
    override val value: Long?,
    parent: StubElement<out PsiElement>?,
    elementType: IStubElementType<*, *>,
) : StubBase<TableGenValueNode>(
    parent, elementType
), TableGenIntegerValueNodeStub

/**
 * Stub interface for [TableGenIdentifierValueNode].
 */
interface TableGenIdentifierValueNodeStub : TableGenValueNodeStub {
    val identifier: String
}

class TableGenIdentifierValueNodeStubElementType(
    debugName: String
) : TableGenStubElementType<TableGenIdentifierValueNodeStub, TableGenIdentifierValueNode>(
    debugName, ::TableGenIdentifierValueNodeImpl,
) {
    override fun createStub(
        psi: TableGenIdentifierValueNode, parentStub: StubElement<out PsiElement?>?
    ): TableGenIdentifierValueNodeStub {
        return TableGenIdentifierValueNodeStubImpl(psi.text, parentStub, this)
    }

    override fun serialize(
        stub: TableGenIdentifierValueNodeStub, dataStream: StubOutputStream
    ) {
        dataStream.writeName(stub.identifier)
    }

    override fun deserialize(
        dataStream: StubInputStream, parentStub: StubElement<*>?
    ): TableGenIdentifierValueNodeStub {
        return TableGenIdentifierValueNodeStubImpl(dataStream.readNameString()!!, parentStub, this)
    }

    override fun isAlwaysLeaf(root: StubBase<*>) = true
}

private class TableGenIdentifierValueNodeStubImpl(
    override val identifier: String,
    parent: StubElement<out PsiElement>?,
    elementType: IStubElementType<*, *>,
) : StubBase<TableGenValueNode>(
    parent, elementType
), TableGenIdentifierValueNodeStub


/**
 * Stub interface for [TableGenStringValueNode].
 */
interface TableGenStringValueNodeStub : TableGenValueNodeStub {
    val value: String
}

class TableGenStringValueNodeStubElementType(
    debugName: String
) : TableGenStubElementType<TableGenStringValueNodeStub, TableGenStringValueNode>(
    debugName, ::TableGenStringValueNodeImpl,
) {
    override fun createStub(
        psi: TableGenStringValueNode, parentStub: StubElement<out PsiElement?>?
    ): TableGenStringValueNodeStub {
        return TableGenStringValueNodeStubImpl(psi.text, parentStub, this)
    }

    override fun serialize(
        stub: TableGenStringValueNodeStub, dataStream: StubOutputStream
    ) {
        dataStream.writeUTFFast(stub.value)
    }

    override fun deserialize(
        dataStream: StubInputStream, parentStub: StubElement<*>?
    ): TableGenStringValueNodeStub {
        return TableGenStringValueNodeStubImpl(dataStream.readUTFFast(), parentStub, this)
    }

    override fun isAlwaysLeaf(root: StubBase<*>) = true
}

private class TableGenStringValueNodeStubImpl(
    override val value: String,
    parent: StubElement<out PsiElement>?,
    elementType: IStubElementType<*, *>,
) : StubBase<TableGenValueNode>(
    parent, elementType
), TableGenStringValueNodeStub