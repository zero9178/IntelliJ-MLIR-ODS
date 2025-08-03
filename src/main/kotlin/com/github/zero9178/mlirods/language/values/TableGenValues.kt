package com.github.zero9178.mlirods.language.values

import com.github.zero9178.mlirods.language.generated.psi.TableGenDefStatement
import com.github.zero9178.mlirods.language.types.TableGenStringType
import com.github.zero9178.mlirods.language.psi.impl.TableGenEvaluationContext
import com.github.zero9178.mlirods.language.types.*

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
 *
 */
class TableGenRecordValue(private val myStatement: TableGenDefStatement) : TableGenValue {

    /**
     *
     */
    inner class FieldValueMap() {
        operator fun get(name: String): TableGenValue? {
            val context = TableGenEvaluationContext(myStatement)
            return context.evaluateFieldInContext(name)
        }
    }

    /**
     *
     */
    val fields: FieldValueMap
        get() = FieldValueMap()

    override val type = TableGenRecordType.create(myStatement)
}

/**
 *
 */
object TableGenUndefValue : TableGenValue {
    override val type: TableGenUnknownType
        get() = TableGenUnknownType
}

/**
 * Class representing an unknown value due to e.g. erroneous code.
 */
object TableGenUnknownValue : TableGenValue {
    override val type: TableGenUnknownType
        get() = TableGenUnknownType
}
