package com.github.zero9178.mlirods.language.types

import com.github.zero9178.mlirods.language.generated.psi.TableGenAbstractClassRef
import com.github.zero9178.mlirods.language.psi.TableGenFieldScopeNode
import com.intellij.psi.PsiNamedElement

/**
 * Super class of values representing the TableGen type system.
 */
sealed class TableGenType

/**
 * 'int' type, which is a 64-bit integer.
 */
object TableGenIntType : TableGenType()

/**
 * Single bit type, also used for booleans.
 */
object TableGenBitType : TableGenType()

/**
 * String type used for string and 'code'.
 */
object TableGenStringType : TableGenType()

/**
 * Singletonn type representing all DAG instances.
 */
object TableGenDagType : TableGenType()

/**
 * Bits type with the given number of bits.
 * [numberOfBits] may be zero if it is unknown, due to e.g. errors in the code.
 */
class TableGenBitsType(val numberOfBits: Int?) : TableGenType()

/**
 * List type containing [elementType] elements.
 */
class TableGenListType(val elementType: TableGenType) : TableGenType()

/**
 * Type representing either a 'def' or 'class' containing fields.
 */
class TableGenRecordType private constructor(
    /**
     * The name of the record being referenced.
     */
    val recordName: String,
    private val myRecord: () -> TableGenFieldScopeNode?
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
            }) {
                record
            }

        /**
         * Constructs a record type from a class reference.
         */
        fun create(reference: TableGenAbstractClassRef) = TableGenRecordType(reference.className) {
            reference.referencedClass
        }
    }

    /**
     * Returns the record being referenced or null if it could not be found.
     */
    val record: TableGenFieldScopeNode?
        get() = myRecord.invoke()
}

/**
 * Type representing an unknown type due to a previous error, but not an error worth reporting itself.
 */
object TableGenUnknownType : TableGenType()
