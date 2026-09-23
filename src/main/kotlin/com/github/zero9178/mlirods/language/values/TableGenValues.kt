package com.github.zero9178.mlirods.language.values

import com.github.zero9178.mlirods.language.generated.psi.TableGenClassInstantiationValueNode
import com.github.zero9178.mlirods.language.generated.psi.TableGenDefStatement
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
 * A list of [elements], all of which can be used as [elementType].
 */
data class TableGenListValue(val elements: List<TableGenValue>, val elementType: TableGenType) : TableGenValue {
    override val type: TableGenListType
        get() = TableGenListType(elementType)
}

/**
 * A record, exposing its fields for further evaluation. Either a 'def' or the anonymous record created by a class
 * instantiation.
 */
class TableGenRecordValue private constructor(
    private val myContext: TableGenEvaluationContext,
    override val type: TableGenRecordType,
) : TableGenValue {

    constructor(defStatement: TableGenDefStatement) : this(
        TableGenEvaluationContext(defStatement), TableGenRecordType.create(defStatement)
    )

    /**
     * The anonymous record created by [instantiation] when evaluated in [context].
     */
    constructor(instantiation: TableGenClassInstantiationValueNode, context: TableGenEvaluationContext) : this(
        TableGenEvaluationContext(instantiation, context), TableGenRecordType.create(instantiation)
    )

    /**
     * Lazily evaluates the fields of the referenced record within its own context.
     */
    inner class FieldValueMap {
        suspend operator fun get(name: String): TableGenValue = myContext.evaluateFieldInContext(myContext, name)
    }

    val fields: FieldValueMap
        get() = FieldValueMap()
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
