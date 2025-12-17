package com.github.zero9178.mlirods.language.psi.impl

import com.github.zero9178.mlirods.language.generated.psi.TableGenVisitor
import com.github.zero9178.mlirods.language.types.TableGenType
import com.github.zero9178.mlirods.language.types.TableGenTypeOfValueVisitor
import com.github.zero9178.mlirods.language.values.TableGenIntegerValue
import com.github.zero9178.mlirods.language.values.TableGenStringValue
import com.github.zero9178.mlirods.language.values.TableGenUnknownValue
import com.github.zero9178.mlirods.language.values.TableGenValue
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement

/**
 * Class passed around as context during a constant evaluation.
 * This is used to keep track of things such as template parameter to argument mapping or field mappings.
 */
class TableGenEvaluationContext

interface TableGenValueNodeEx : PsiElement {
    /**
     * Generic accept method allowing 'TableGenVisitor's with return values.
     */
    fun <R> accept(visitor: TableGenVisitor<R>): R

    /**
     * Returns the type of this TableGen expression.
     */
    val type: TableGenType
        get() = accept(TableGenTypeOfValueVisitor)

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

interface TableGenIntegerValueNodeEx : TableGenAtomicValue {
    override fun evaluateAtomic(): TableGenIntegerValue?
}

interface TableGenStringValueNodeEx : TableGenAtomicValue {
    override fun evaluateAtomic(): TableGenStringValue?
}

interface TableGenBlockStringValueNodeEx : TableGenStringValueNodeEx {
    /**
     *
     */
    val relevantTextRange: TextRange
        get() {
            return TextRange(2, textLength - 2)
        }
}