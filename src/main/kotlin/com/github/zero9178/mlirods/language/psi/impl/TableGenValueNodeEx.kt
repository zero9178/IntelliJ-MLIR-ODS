package com.github.zero9178.mlirods.language.psi.impl

import com.github.zero9178.mlirods.language.generated.psi.*
import com.github.zero9178.mlirods.language.psi.TableGenBangOperator
import com.github.zero9178.mlirods.language.stubs.impl.TableGenBoolValueNodeStub
import com.github.zero9178.mlirods.language.stubs.impl.TableGenIntegerValueNodeStub
import com.github.zero9178.mlirods.language.stubs.impl.TableGenStringValueNodeStub
import com.github.zero9178.mlirods.language.types.TableGenType
import com.github.zero9178.mlirods.language.types.TableGenTypeOfValueVisitor
import com.github.zero9178.mlirods.language.values.TableGenIntegerValue
import com.github.zero9178.mlirods.language.values.TableGenStringValue
import com.github.zero9178.mlirods.language.values.TableGenUnknownValue
import com.github.zero9178.mlirods.language.values.TableGenValue
import com.github.zero9178.mlirods.model.getProjectContextDependentCache
import com.intellij.psi.PsiElement
import com.intellij.util.containers.ConcurrentFactoryMap

/**
 * Class passed around as context during a constant evaluation.
 * This is used to keep track of things such as template parameter to argument mapping or field mappings.
 *
 * Two contexts are considered equal if they originate from the same [source]. This allows evaluation results
 * to be cached per context (see [TableGenValueNodeEx.evaluate]).
 */
class TableGenEvaluationContext private constructor(
    private val source: Any?,
    val evaluateTemplateArgDeclInContext: TableGenEvaluationContext.(TableGenTemplateArgDecl) -> TableGenValue,
    val evaluateFieldInContext: TableGenEvaluationContext.(String) -> TableGenValue,
) {

    /**
     * Null-context. Template arguments and implicit field definitions yield unknown values.
     * This is the context that should be used for top-level evaluation and class-statements.
     */
    constructor() : this(
        null,
        {
            TableGenUnknownValue
        },
        {
            TableGenUnknownValue
        },
    )

    constructor(defStatement: TableGenDefStatement) : this(defStatement, {
        defStatement.allArgToTemplateArgMapping[it]?.evaluate(this) ?: TableGenUnknownValue
    }, { fieldName ->
        // TODO: Implement append and prepend semantics.
        defStatement.allFieldAssignments[fieldName]?.lastOrNull()?.assignedValueNode
            ?.evaluate(this) ?: TableGenUnknownValue
    })

    override fun equals(other: Any?): Boolean =
        this === other || (other is TableGenEvaluationContext && source == other.source)

    override fun hashCode(): Int = source.hashCode()
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
        get() = getProjectContextDependentCache(this) {
            it.accept(TableGenTypeOfValueVisitor)
        }

    /**
     * Performs constant evaluation of this value within the given context.
     *
     * Results are cached per [context] and invalidated on any PSI or include-context change. Implementations should not
     * override this method but [evaluateInner] instead.
     */
    fun evaluate(context: TableGenEvaluationContext): TableGenValue =
        getProjectContextDependentCache(this) { element ->
            ConcurrentFactoryMap.createMap<TableGenEvaluationContext, TableGenValue> {
                element.evaluateInner(it)
            }
        }[context] ?: TableGenUnknownValue

    /**
     * Performs the actual constant evaluation of this value within the given context. Implemented per value node kind;
     * callers should use [evaluate] which adds caching on top.
     */
    fun evaluateInner(context: TableGenEvaluationContext): TableGenValue = TableGenUnknownValue
}

/**
 * Specialization interface for [TableGenValueNodeEx.evaluate] implementations which are atomic.
 * The result of these are never dependent on a context, but rather purely on their Psi subtree.
 */
interface TableGenAtomicValue : TableGenValueNodeEx {
    fun evaluateAtomic(): TableGenValue?

    override val type: TableGenType
        // No need to cache for atomics.
        get() = accept(TableGenTypeOfValueVisitor)

    /**
     * Atomic values do not depend on the [context] and are cheap to compute from their PSI subtree, so [evaluate]
     * bypasses caching and resolves directly to [evaluateAtomic].
     */
    override fun evaluate(context: TableGenEvaluationContext): TableGenValue =
        evaluateAtomic() ?: TableGenUnknownValue

    override fun evaluateInner(context: TableGenEvaluationContext) = evaluate(context)
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

    /**
     * The known bang operator this node uses, or null if [operatorName] is not a recognized bang operator.
     */
    val operator: TableGenBangOperator?
        get() = TableGenBangOperator.fromOperatorName(operatorName)
}

interface TableGenBoolValueNodeEx : TableGenAtomicValue {
    override fun evaluateAtomic(): TableGenIntegerValue

    val stub: TableGenBoolValueNodeStub?
}

interface TableGenStringValueNodeEx : TableGenAtomicValue {
    override fun evaluateAtomic(): TableGenStringValue

    val stub: TableGenStringValueNodeStub?
}