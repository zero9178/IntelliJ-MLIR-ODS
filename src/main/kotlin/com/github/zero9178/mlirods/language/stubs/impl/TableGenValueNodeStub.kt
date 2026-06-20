package com.github.zero9178.mlirods.language.stubs.impl

import com.github.zero9178.mlirods.language.generated.psi.*
import com.github.zero9178.mlirods.language.generated.psi.impl.*
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

class TableGenConcatValueNodeStubElementType(debugName: String) :
    TableGenAbstractValueNodeStubElementType<TableGenConcatValueNode>(
        debugName, ::TableGenConcatValueNodeImpl
    )

// TODO: Need custom stub for the field identifier.
class TableGenFieldAccessValueNodeStubElementType(debugName: String) :
    TableGenAbstractValueNodeStubElementType<TableGenFieldAccessValueNode>(
        debugName, ::TableGenFieldAccessValueNodeImpl
    )

class TableGenBitAccessValueNodeStubElementType(debugName: String) :
    TableGenAbstractValueNodeStubElementType<TableGenBitAccessValueNode>(
        debugName, ::TableGenBitAccessValueNodeImpl
    )

class TableGenSliceAccessValueNodeStubElementType(debugName: String) :
    TableGenAbstractValueNodeStubElementType<TableGenSliceAccessValueNode>(
        debugName, ::TableGenSliceAccessValueNodeImpl
    )

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
        // Store the decoded value so that stub-based evaluation matches AST-based evaluation.
        return TableGenStringValueNodeStubImpl(psi.evaluateAtomic().value, parentStub, this)
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

// 'block_string_value' is a sub type of 'string_value_node' and therefore shares its [TableGenStringValueNodeStub].
class TableGenBlockStringValueStubElementType(
    debugName: String
) : TableGenStubElementType<TableGenStringValueNodeStub, TableGenBlockStringValue>(
    debugName, ::TableGenBlockStringValueImpl,
) {
    override fun createStub(
        psi: TableGenBlockStringValue, parentStub: StubElement<out PsiElement?>?
    ): TableGenStringValueNodeStub {
        // Store the decoded value so that stub-based evaluation matches AST-based evaluation.
        return TableGenStringValueNodeStubImpl(psi.evaluateAtomic().value, parentStub, this)
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

/**
 * Stub interface for [TableGenBoolValueNode].
 */
interface TableGenBoolValueNodeStub : TableGenValueNodeStub {
    val value: Boolean
}

class TableGenBoolValueNodeStubElementType(
    debugName: String
) : TableGenStubElementType<TableGenBoolValueNodeStub, TableGenBoolValueNode>(
    debugName, ::TableGenBoolValueNodeImpl,
) {
    override fun createStub(
        psi: TableGenBoolValueNode, parentStub: StubElement<out PsiElement?>?
    ): TableGenBoolValueNodeStub {
        return TableGenBoolValueNodeStubImpl(psi.evaluateAtomic().value == 1L, parentStub, this)
    }

    override fun serialize(
        stub: TableGenBoolValueNodeStub, dataStream: StubOutputStream
    ) {
        dataStream.writeBoolean(stub.value)
    }

    override fun deserialize(
        dataStream: StubInputStream, parentStub: StubElement<*>?
    ): TableGenBoolValueNodeStub {
        return TableGenBoolValueNodeStubImpl(dataStream.readBoolean(), parentStub, this)
    }

    override fun isAlwaysLeaf(root: StubBase<*>) = true
}

private class TableGenBoolValueNodeStubImpl(
    override val value: Boolean,
    parent: StubElement<out PsiElement>?,
    elementType: IStubElementType<*, *>,
) : StubBase<TableGenValueNode>(
    parent, elementType
), TableGenBoolValueNodeStub

class TableGenUndefValueNodeStubElementType(debugName: String) :
    TableGenAbstractValueNodeStubElementType<TableGenUndefValueNode>(
        debugName, ::TableGenUndefValueNodeImpl
    ) {
    override fun isAlwaysLeaf(root: StubBase<*>) = true
}

class TableGenBitsInitValueNodeStubElementType(debugName: String) :
    TableGenAbstractValueNodeStubElementType<TableGenBitsInitValueNode>(
        debugName, ::TableGenBitsInitValueNodeImpl
    )

class TableGenListInitValueNodeStubElementType(debugName: String) :
    TableGenAbstractValueNodeStubElementType<TableGenListInitValueNode>(
        debugName, ::TableGenListInitValueNodeImpl
    )

class TableGenDagInitValueNodeStubElementType(debugName: String) :
    TableGenAbstractValueNodeStubElementType<TableGenDagInitValueNode>(
        debugName, ::TableGenDagInitValueNodeImpl
    )

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

// TODO: Needs proper stub.
class TableGenClassInstantiationValueNodeStubElementType(debugName: String) :
    TableGenAbstractValueNodeStubElementType<TableGenClassInstantiationValueNode>(
        debugName, ::TableGenClassInstantiationValueNodeImpl
    )

class TableGenForeachOperatorValueNodeStubElementType(debugName: String) :
    TableGenAbstractValueNodeStubElementType<TableGenForeachOperatorValueNode>(
        debugName, ::TableGenForeachOperatorValueNodeImpl
    )

class TableGenFoldlOperatorValueNodeStubElementType(debugName: String) :
    TableGenAbstractValueNodeStubElementType<TableGenFoldlOperatorValueNode>(
        debugName, ::TableGenFoldlOperatorValueNodeImpl
    )

/**
 * Stub interface for [TableGenBangOperatorValueNode].
 */
interface TableGenBangOperatorValueNodeStub : TableGenValueNodeStub {
    /**
     * The bang operator token text, e.g. '!div'.
     */
    val operator: String
}

class TableGenBangOperatorValueNodeStubElementType(
    debugName: String
) : TableGenStubElementType<TableGenBangOperatorValueNodeStub, TableGenBangOperatorValueNode>(
    debugName, ::TableGenBangOperatorValueNodeImpl,
) {
    override fun createStub(
        psi: TableGenBangOperatorValueNode, parentStub: StubElement<out PsiElement?>?
    ): TableGenBangOperatorValueNodeStub {
        return TableGenBangOperatorValueNodeStubImpl(psi.bangOperator.text, parentStub, this)
    }

    override fun serialize(
        stub: TableGenBangOperatorValueNodeStub, dataStream: StubOutputStream
    ) {
        dataStream.writeName(stub.operator)
    }

    override fun deserialize(
        dataStream: StubInputStream, parentStub: StubElement<*>?
    ): TableGenBangOperatorValueNodeStub {
        return TableGenBangOperatorValueNodeStubImpl(dataStream.readNameString()!!, parentStub, this)
    }
}

private class TableGenBangOperatorValueNodeStubImpl(
    override val operator: String,
    parent: StubElement<out PsiElement>?,
    elementType: IStubElementType<*, *>,
) : StubBase<TableGenValueNode>(
    parent, elementType
), TableGenBangOperatorValueNodeStub

class TableGenCondOperatorValueNodeStubElementType(debugName: String) :
    TableGenAbstractValueNodeStubElementType<TableGenCondOperatorValueNode>(
        debugName, ::TableGenCondOperatorValueNodeImpl
    )

class TableGenCastOperatorValueNodeStubElementType(debugName: String) :
    TableGenAbstractValueNodeStubElementType<TableGenCastOperatorValueNode>(
        debugName, ::TableGenCastOperatorValueNodeImpl
    )

class TableGenSortOperatorValueNodeStubElementType(debugName: String) :
    TableGenAbstractValueNodeStubElementType<TableGenSortOperatorValueNode>(
        debugName, ::TableGenSortOperatorValueNodeImpl
    )

class TableGenSwitchOperatorValueNodeStubElementType(debugName: String) :
    TableGenAbstractValueNodeStubElementType<TableGenSwitchOperatorValueNode>(
        debugName, ::TableGenSwitchOperatorValueNodeImpl
    )
