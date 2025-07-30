package com.github.zero9178.mlirods.language.psi.impl

import com.github.zero9178.mlirods.language.types.TableGenType
import com.github.zero9178.mlirods.language.types.TableGenUnknownType
import com.github.zero9178.mlirods.language.values.TableGenIntegerValue
import com.github.zero9178.mlirods.language.values.TableGenUnknownValue
import com.github.zero9178.mlirods.language.values.TableGenValue
import com.intellij.psi.PsiElement

/**
 * Class passed around as context during a constant evaluation.
 * This is used to keep track of things such as template parameter to argument mapping or field mappings.
 */
class TableGenEvaluationContext

interface TableGenValueNodeEx : PsiElement {
    /**
     * Returns the type of this TableGen expression.
     */
    val type: TableGenType
        get() = TableGenUnknownType

    /**
     * Performs constant evaluation of this value within the given context.
     */
    fun evaluate(context: TableGenEvaluationContext): TableGenValue = TableGenUnknownValue
}

/**
 * Specialization interface for [TableGenValueNodeEx.evaluate] implementations which are atomic.
 * The result of these are never dependent on a context, but rather purely on their Psi subtree.
 */
interface TableGenAtomicValue : TableGenValueNodeEx {
    fun evaluateAtomic(): TableGenValue?
}

interface TableGenIntegerValueNodexEx : TableGenAtomicValue {
    override fun evaluateAtomic(): TableGenIntegerValue?
}
