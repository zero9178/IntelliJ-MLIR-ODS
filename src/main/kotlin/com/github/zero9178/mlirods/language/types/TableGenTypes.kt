package com.github.zero9178.mlirods.language.types

import com.github.zero9178.mlirods.language.generated.psi.TableGenAbstractClassRef
import com.github.zero9178.mlirods.language.psi.TableGenFieldScopeNode
import com.github.zero9178.mlirods.model.TableGenCompilationContext
import com.intellij.psi.PsiNamedElement
import com.intellij.util.concurrency.annotations.RequiresReadLock

/**
 * Super class of values representing the TableGen type system.
 */
sealed class TableGenType {
    /**
     * Returns whether a value of this type is assignable to a location of the [target] type. Record types resolve
     * their records within [context] to answer this.
     *
     * Returns null whenever the outcome cannot be determined, due to e.g. an erroneous AST or failed reference
     * resolution.
     */
    @RequiresReadLock
    abstract fun isConvertibleTo(target: TableGenType, context: TableGenCompilationContext): Boolean?

    /**
     * Returns a human-readable representation of this type.
     */
    abstract override fun toString(): String
}

/**
 * 'int' type, which is a 64-bit integer.
 */
object TableGenIntType : TableGenType() {
    override fun isConvertibleTo(target: TableGenType, context: TableGenCompilationContext): Boolean? = when (target) {
        is TableGenBitType, is TableGenBitsType, is TableGenIntType -> true
        is TableGenUnknownType -> null
        else -> false
    }

    override fun toString() = "int"
}

/**
 * Single bit type, also used for booleans.
 */
object TableGenBitType : TableGenType() {
    override fun isConvertibleTo(target: TableGenType, context: TableGenCompilationContext): Boolean? = when (target) {
        is TableGenBitType, is TableGenIntType -> true
        // A bit is convertible to 'bits<1>'.
        is TableGenBitsType -> target.numberOfBits?.let { it == 1L }
        is TableGenUnknownType -> null
        else -> false
    }

    override fun toString() = "bit"
}

/**
 * String type used for string and 'code'.
 */
object TableGenStringType : TableGenType() {
    override fun isConvertibleTo(target: TableGenType, context: TableGenCompilationContext): Boolean? = when (target) {
        is TableGenStringType -> true
        is TableGenUnknownType -> null
        else -> false
    }

    override fun toString() = "string"
}

/**
 * Singletonn type representing all DAG instances.
 */
object TableGenDagType : TableGenType() {
    override fun isConvertibleTo(target: TableGenType, context: TableGenCompilationContext): Boolean? = when (target) {
        is TableGenDagType -> true
        is TableGenUnknownType -> null
        else -> false
    }

    override fun toString() = "dag"
}

/**
 * Bits type with the given number of bits.
 * [numberOfBits] may be zero if it is unknown, due to e.g. errors in the code.
 */
data class TableGenBitsType(val numberOfBits: Long?) : TableGenType() {
    override fun isConvertibleTo(target: TableGenType, context: TableGenCompilationContext): Boolean? = when (target) {
        is TableGenBitsType ->
            if (numberOfBits == null || target.numberOfBits == null) null
            else numberOfBits == target.numberOfBits
        // 'bits<1>' is convertible to a single bit.
        is TableGenBitType -> numberOfBits?.let { it == 1L }
        is TableGenIntType -> true
        is TableGenUnknownType -> null
        else -> false
    }

    override fun toString() = "bits<${numberOfBits ?: "?"}>"
}

/**
 * List type containing [elementType] elements.
 */
data class TableGenListType(val elementType: TableGenType) : TableGenType() {
    override fun isConvertibleTo(target: TableGenType, context: TableGenCompilationContext): Boolean? = when (target) {
        is TableGenListType -> elementType.isConvertibleTo(target.elementType, context)
        is TableGenUnknownType -> null
        else -> false
    }

    override fun toString() = "list<$elementType>"
}

/**
 * Type representing either a 'def' or 'class' containing fields.
 *
 * A type is syntactic: created from a class reference, it names the class without resolving it, as which statement
 * the reference resolves to depends on the compilation context it is asked in. Resolution happens in [record].
 */
class TableGenRecordType private constructor(
    /**
     * The name of the record being referenced.
     */
    val recordName: String,
    private val myRecord: TableGenFieldScopeNode?,
    private val myReference: TableGenAbstractClassRef?,
) :
    TableGenType() {

    companion object {
        /**
         * Constructs a record type from an already resolved record.
         * The given record must have name.
         */
        fun <T> create(record: T) where T : TableGenFieldScopeNode, T : PsiNamedElement =
            TableGenRecordType(checkNotNull(record.name) {
                "Record must be named"
            }, record, null)

        /**
         * Constructs a record type from a class reference.
         */
        fun create(reference: TableGenAbstractClassRef) = TableGenRecordType(reference.className, null, reference)
    }

    /**
     * Returns the record being referenced within [context] or null if it could not be found. A type created from
     * an already resolved record yields that record whatever the context.
     */
    @RequiresReadLock
    fun record(context: TableGenCompilationContext): TableGenFieldScopeNode? =
        myRecord ?: myReference?.referencedClass(context)

    override fun isConvertibleTo(target: TableGenType, context: TableGenCompilationContext): Boolean? {
        if (target !is TableGenRecordType) return if (target is TableGenUnknownType) null else false

        // Identical class references are trivially convertible, even if resolution fails.
        if (this === target) return true

        val source = record(context) ?: return null
        val targetRecord = target.record(context) ?: return null
        return source.derivesFrom(targetRecord, context)
    }

    override fun toString() = recordName
}

/**
 * Type of the `?` ("undef") value, which is assignable to any field regardless of its type.
 */
object TableGenUndefType : TableGenType() {
    override fun isConvertibleTo(target: TableGenType, context: TableGenCompilationContext): Boolean = true

    override fun toString() = "?"
}

/**
 * Type representing an unknown type due to a previous error, but not an error worth reporting itself.
 */
object TableGenUnknownType : TableGenType() {
    override fun isConvertibleTo(target: TableGenType, context: TableGenCompilationContext): Boolean? = null

    override fun toString() = "?"
}

/**
 * Returns the more precise type of a location of this declared type, given that it holds a value of [valueType].
 *
 * Values are converted to the declared type on assignment, which is a no-op for records of a derived class. These keep
 * their more derived type, and so do the elements of a list. Any other declared type is returned as is, as are types
 * that the value is not known to be convertible to.
 */
@RequiresReadLock
fun TableGenType.refinedBy(valueType: TableGenType, context: TableGenCompilationContext): TableGenType = when (this) {
    is TableGenRecordType if valueType is TableGenRecordType && valueType.isConvertibleTo(this, context) == true ->
        valueType

    is TableGenListType if valueType is TableGenListType ->
        TableGenListType(elementType.refinedBy(valueType.elementType, context))

    else -> this
}

/**
 * Returns the type that values of both [t1] and [t2] can be used as, as is required by operators yielding one of
 * multiple values (e.g. '!if' or '!cond').
 *
 * [TableGenUndefType] adopts the type of the other side, as a '?' operand is compatible with any type.
 * [TableGenUnknownType] is returned if the two types are inconsistent, mirroring that TableGen rejects such programs;
 * reporting them is up to the caller.
 */
@RequiresReadLock
fun commonType(t1: TableGenType, t2: TableGenType, context: TableGenCompilationContext): TableGenType {
    // Both '?' and an unknown type carry no information, making the other side the best guess available.
    if (t1 is TableGenUndefType || t1 is TableGenUnknownType) return t2
    if (t2 is TableGenUndefType || t2 is TableGenUnknownType) return t1

    if (t1 == t2) return t1
    if (t1.isConvertibleTo(t2, context) == true) return t2
    if (t2.isConvertibleTo(t1, context) == true) return t1

    // Two lists have a common list type if their element types do.
    if (t1 is TableGenListType && t2 is TableGenListType) {
        return TableGenListType(commonType(t1.elementType, t2.elementType, context))
    }

    // TODO: Two record types where neither derives from the other still have the set of classes that both derive from
    //  as their common type. Representing this requires a record type constrained by a set of classes rather than one
    //  naming a single record, so an unknown type is returned for now.
    return TableGenUnknownType
}

/**
 * Returns the type that values of all types in this collection can be used as. See [commonType].
 */
@RequiresReadLock
fun Iterable<TableGenType>.commonType(context: TableGenCompilationContext): TableGenType =
    reduceOrNull { t1, t2 -> commonType(t1, t2, context) } ?: TableGenUnknownType
