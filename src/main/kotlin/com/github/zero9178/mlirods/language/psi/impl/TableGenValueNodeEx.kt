package com.github.zero9178.mlirods.language.psi.impl

import com.github.zero9178.mlirods.language.generated.psi.*
import com.github.zero9178.mlirods.language.psi.TableGenBangOperator
import com.github.zero9178.mlirods.language.stubs.impl.TableGenBinaryIntegerValueNodeStub
import com.github.zero9178.mlirods.language.stubs.impl.TableGenBoolValueNodeStub
import com.github.zero9178.mlirods.language.stubs.impl.TableGenIntegerValueNodeStub
import com.github.zero9178.mlirods.language.stubs.impl.TableGenStringValueNodeStub
import com.github.zero9178.mlirods.cache.SuspendingCachedValue
import com.github.zero9178.mlirods.language.types.TableGenType
import com.github.zero9178.mlirods.language.types.computeTypeOf
import com.github.zero9178.mlirods.language.types.typeOfAtomic
import com.github.zero9178.mlirods.language.values.TableGenIntegerValue
import com.github.zero9178.mlirods.language.values.TableGenStringValue
import com.github.zero9178.mlirods.language.values.TableGenUnknownValue
import com.github.zero9178.mlirods.language.values.TableGenValue
import com.github.zero9178.mlirods.model.dependsOnProjectContext
import com.github.zero9178.mlirods.model.getProjectContextDependentCache
import com.github.zero9178.mlirods.model.projectContextDependentSuspendingCachedValue
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.util.concurrency.annotations.RequiresBlockingContext
import com.intellij.util.concurrency.annotations.RequiresReadLock
import java.util.concurrent.ConcurrentHashMap

/**
 * Class passed around as context during a constant evaluation.
 * This is used to keep track of things such as template parameter to argument mapping or field mappings.
 *
 * Two contexts are considered equal if they originate from the same [source]. This allows evaluation results
 * to be cached per context (see [TableGenValueNodeEx.evaluate]).
 */
class TableGenEvaluationContext private constructor(
    private val source: Any?,
    val evaluateTemplateArgDeclInContext: suspend TableGenEvaluationContext.(TableGenTemplateArgDecl) -> TableGenValue,
    val evaluateFieldInContext: suspend TableGenEvaluationContext.(String) -> TableGenValue,
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
        // Fields may be defined in terms of each other (e.g. 'int g = f; let f = g;'). TableGen rejects such cycles,
        // and so do the cached values: every value on the cycle is unknown, see 'evaluate'.
        // TODO: Implement append and prepend semantics.
        defStatement.allFieldAssignments[fieldName]?.lastOrNull()?.assignedValueNode
            ?.evaluate(this) ?: TableGenUnknownValue
    })

    override fun equals(other: Any?): Boolean =
        this === other || (other is TableGenEvaluationContext && source == other.source)

    override fun hashCode(): Int = source.hashCode()

    override fun toString() = when (source) {
        null -> "null context"
        is TableGenDefStatement -> "context of 'def ${source.name}'"
        else -> "context of $source"
    }
}

/**
 * The cached values of [element], one per context it was evaluated in so far. The map is what is dropped when anything
 * changes, and with it the contexts, which would otherwise keep the PSI they originate from alive.
 */
private fun cachedValuesOf(
    element: TableGenValueNodeEx,
): SuspendingCachedValue<ConcurrentHashMap<TableGenEvaluationContext, SuspendingCachedValue<TableGenValue>>> =
    projectContextDependentSuspendingCachedValue(element) { ConcurrentHashMap() }

private fun ConcurrentHashMap<TableGenEvaluationContext, SuspendingCachedValue<TableGenValue>>.cachedValueOf(
    element: TableGenValueNodeEx,
    context: TableGenEvaluationContext,
): SuspendingCachedValue<TableGenValue> = computeIfAbsent(context) {
    // Identified by what is at hand without loading the tree of a stubbed element.
    SuspendingCachedValue(
        "value of ${element.javaClass.simpleName}@${System.identityHashCode(element)} in $context",
        onCycle = { TableGenUnknownValue },
    ) {
        dependsOnProjectContext(element.project)
        element.evaluateInner(context)
    }
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
        get() = getProjectContextDependentCache(this) { computeTypeOf(it) }

    /**
     * Performs constant evaluation of this value within the given context. A value that depends on itself is unknown.
     *
     * Results are cached per [context] and invalidated on any PSI or include-context change. They are
     * [SuspendingCachedValue]s computed from each other. This is how code that is a coroutine asks for one; everything
     * else has [evaluateBlocking]. Implementations should not override this method but [evaluateInner] instead.
     */
    suspend fun evaluate(context: TableGenEvaluationContext): TableGenValue =
        cachedValuesOf(this).await().cachedValueOf(this, context).await()

    /**
     * [evaluate] for callers that cannot suspend, i.e. the platform. See [SuspendingCachedValue.getBlocking] for what
     * is required of the read action this is called in.
     */
    @RequiresReadLock
    @RequiresBlockingContext
    fun evaluateBlocking(context: TableGenEvaluationContext): TableGenValue =
        cachedValuesOf(this).getBlocking().cachedValueOf(this, context).getBlocking()

    /**
     * Performs the actual constant evaluation of this value within the given context. Implemented per value node kind;
     * callers should use [evaluate] which adds caching on top.
     */
    suspend fun evaluateInner(context: TableGenEvaluationContext): TableGenValue = TableGenUnknownValue
}

/**
 * Specialization interface for [TableGenValueNodeEx.evaluate] implementations which are atomic.
 * The result of these are never dependent on a context, but rather purely on their Psi subtree.
 */
interface TableGenAtomicValue : TableGenValueNodeEx {
    fun evaluateAtomic(): TableGenValue?

    override val type: TableGenType
        // No need to cache for atomics.
        get() = typeOfAtomic(this)

    /**
     * Atomic values do not depend on the [context] and are cheap to compute from their PSI subtree, so [evaluate]
     * bypasses caching and resolves directly to [evaluateAtomic].
     */
    override suspend fun evaluate(context: TableGenEvaluationContext): TableGenValue =
        evaluateAtomic() ?: TableGenUnknownValue

    override fun evaluateBlocking(context: TableGenEvaluationContext): TableGenValue =
        evaluateAtomic() ?: TableGenUnknownValue

    override suspend fun evaluateInner(context: TableGenEvaluationContext) = evaluate(context)
}

interface TableGenIntegerValueNodeEx : TableGenAtomicValue {
    override fun evaluateAtomic(): TableGenIntegerValue?

    val stub: TableGenIntegerValueNodeStub?
}

/**
 * A binary literal denotes a 'bits<n>' value rather than an integer.
 */
interface TableGenBinaryIntegerValueNodeEx : TableGenValueNodeEx {
    val stub: TableGenBinaryIntegerValueNodeStub?

    /**
     * The number of bits this literal denotes, which is the number of digits written: '0b0001' is four bits wide while
     * '0b1' is one.
     */
    val numberOfBits: Int
        get() = stub?.numberOfBits ?: (textLength - BINARY_PREFIX.length)
}

/**
 * Prefix every binary literal starts with.
 */
internal const val BINARY_PREFIX = "0b"

interface TableGenIdentifierValueNodeEx : TableGenValueNodeEx {
    val identifierText: String
}

interface TableGenBangOperatorValueNodeEx : TableGenValueNodeEx {
    /**
     * The bang operator token text, e.g. '!div'.
     */
    val operatorName: String

    /**
     * The known bang operator this node uses, or null if [operatorName] is not a recognized bang operator. Only
     * operators without a dedicated Psi node can ever be returned.
     */
    val operator: TableGenBangOperator?
        get() = TableGenBangOperator.fromOperatorName(operatorName)

    /**
     * Text range of the '<type>' argument including its angle brackets, or null if the operator has no type argument.
     * The operator's own angle brackets are used rather than the outermost ones of the type, which may bring its own
     * (e.g. 'list<int>').
     */
    val typeArgumentRange: TextRange?
}

interface TableGenBoolValueNodeEx : TableGenAtomicValue {
    override fun evaluateAtomic(): TableGenIntegerValue

    val stub: TableGenBoolValueNodeStub?
}

interface TableGenStringValueNodeEx : TableGenAtomicValue {
    override fun evaluateAtomic(): TableGenStringValue

    val stub: TableGenStringValueNodeStub?
}