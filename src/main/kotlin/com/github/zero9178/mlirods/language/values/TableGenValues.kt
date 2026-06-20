package com.github.zero9178.mlirods.language.values

import com.github.zero9178.mlirods.language.generated.psi.TableGenDefStatement
import com.github.zero9178.mlirods.language.psi.impl.TableGenEvaluationContext
import com.github.zero9178.mlirods.language.types.TableGenIntType
import com.github.zero9178.mlirods.language.types.TableGenRecordType
import com.github.zero9178.mlirods.language.types.TableGenStringType
import com.github.zero9178.mlirods.language.types.TableGenType
import com.github.zero9178.mlirods.language.types.TableGenUndefType
import com.github.zero9178.mlirods.language.types.TableGenUnknownType
import com.intellij.psi.util.CachedValuesManager

/**
 * Base class for all possible TableGen values.
 */
sealed interface TableGenValue {
    /**
     * Returns the type of the value.
     */
    val type: TableGenType
}

/**
 * A 64-bit integer value as can be obtained by an integer token.
 */
data class TableGenIntegerValue(val value: Long) : TableGenValue {
    override val type: TableGenIntType
        get() = TableGenIntType
}

/**
 * A string value.
 */
data class TableGenStringValue(val value: String) : TableGenValue {
    override val type: TableGenStringType
        get() = TableGenStringType
}

/**
 * A reference to a record ('def') instance, exposing its fields for further evaluation.
 */
class TableGenRecordValue(private val myStatement: TableGenDefStatement) : TableGenValue {

    /**
     * Lazily evaluates the fields of the referenced record within its own context.
     */
    inner class FieldValueMap {
        operator fun get(name: String): TableGenValue {
            val context = TableGenEvaluationContext(myStatement)
            return context.evaluateFieldInContext(context, name)
        }
    }

    val fields: FieldValueMap
        get() = FieldValueMap()

    override val type = TableGenRecordType.create(myStatement)
}

/**
 * The `?` value, representing an uninitialized ("undef") value.
 */
object TableGenUndefValue : TableGenValue {
    override val type: TableGenUndefType
        get() = TableGenUndefType
}

/**
 * Class representing an unknown value due to e.g. erroneous code.
 */
object TableGenUnknownValue : TableGenValue {
    override val type: TableGenUnknownType
        get() = TableGenUnknownType
}
