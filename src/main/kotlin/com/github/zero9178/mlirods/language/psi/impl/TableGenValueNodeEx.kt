package com.github.zero9178.mlirods.language.psi.impl

import com.github.zero9178.mlirods.language.generated.psi.TableGenDefStatement
import com.github.zero9178.mlirods.language.generated.psi.TableGenFieldBodyItem
import com.github.zero9178.mlirods.language.generated.psi.TableGenLetBodyItem
import com.github.zero9178.mlirods.language.generated.psi.TableGenLetItem
import com.github.zero9178.mlirods.language.generated.psi.TableGenTemplateArgDecl
import com.github.zero9178.mlirods.language.generated.psi.TableGenValueNode
import com.github.zero9178.mlirods.language.generated.psi.TableGenVisitor
import com.github.zero9178.mlirods.language.psi.TableGenFieldScopeNode
import com.github.zero9178.mlirods.language.stubs.impl.TableGenBoolValueNodeStub
import com.github.zero9178.mlirods.language.stubs.impl.TableGenIntegerValueNodeStub
import com.github.zero9178.mlirods.language.stubs.impl.TableGenStringValueNodeStub
import com.github.zero9178.mlirods.language.types.TableGenType
import com.github.zero9178.mlirods.language.types.TableGenTypeOfValueVisitor
import com.github.zero9178.mlirods.language.values.TableGenIntegerValue
import com.github.zero9178.mlirods.language.values.TableGenStringValue
import com.github.zero9178.mlirods.language.values.TableGenUnknownValue
import com.github.zero9178.mlirods.language.values.TableGenValue
import com.intellij.psi.PsiElement
import com.intellij.psi.util.CachedValuesManager

/**
 * Class passed around as context during a constant evaluation.
 * This is used to keep track of things such as template parameter to argument mapping or field mappings.
 */
class TableGenEvaluationContext private constructor(
    val evaluateTemplateArgDeclInContext: TableGenEvaluationContext.(TableGenTemplateArgDecl) -> TableGenValue,
    val evaluateFieldInContext: TableGenEvaluationContext.(String) -> TableGenValue,
) {

    constructor() : this(
        {
            TableGenUnknownValue
        },
        {
            TableGenUnknownValue
        },
    )

    constructor(defStatement: TableGenDefStatement) : this({
        defStatement.allArgToTemplateArgMapping[it]?.evaluate(this) ?: TableGenUnknownValue
    }, { fieldName ->
        defStatement.allFieldAssignments[fieldName]?.lastOrNull()?.let {
            when (it) {
                is TableGenFieldBodyItem -> it.valueNode
                // TODO: Implement append and prepend semantics.
                is TableGenLetBodyItem -> it.valueNode
                is TableGenLetItem -> it.valueNode
                else -> null
            }
        }?.evaluate(this) ?: TableGenUnknownValue
    })
}

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

    val stub: TableGenIntegerValueNodeStub?
}

interface TableGenIdentifierValueNodeEx : TableGenValueNodeEx {
    val identifierText: String
}

interface TableGenBangOperatorValueNodeEx : TableGenValueNodeEx {
    /**
     * The bang operator token text, e.g. '!div'.
     */
    val operatorName: String
}

interface TableGenBoolValueNodeEx : TableGenAtomicValue {
    override fun evaluateAtomic(): TableGenIntegerValue

    val stub: TableGenBoolValueNodeStub?
}

interface TableGenStringValueNodeEx : TableGenAtomicValue {
    override fun evaluateAtomic(): TableGenStringValue

    val stub: TableGenStringValueNodeStub?
}