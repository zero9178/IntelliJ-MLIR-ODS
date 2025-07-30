package com.github.zero9178.mlirods.language.values

import com.github.zero9178.mlirods.language.types.TableGenIntType
import com.github.zero9178.mlirods.language.types.TableGenType
import com.github.zero9178.mlirods.language.types.TableGenUnknownType

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
 * Class representing an unknown value due to e.g. erroneous code.
 */
object TableGenUnknownValue : TableGenValue {
    override val type: TableGenUnknownType
        get() = TableGenUnknownType
}
