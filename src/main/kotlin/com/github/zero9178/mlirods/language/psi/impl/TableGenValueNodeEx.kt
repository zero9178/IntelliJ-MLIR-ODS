package com.github.zero9178.mlirods.language.psi.impl

import com.github.zero9178.mlirods.cache.SuspendingCachedValue
import com.github.zero9178.mlirods.language.generated.psi.TableGenClassInstantiationValueNode
import com.github.zero9178.mlirods.language.generated.psi.TableGenDefStatement
import com.github.zero9178.mlirods.language.generated.psi.TableGenTemplateArgDecl
import com.github.zero9178.mlirods.language.generated.psi.TableGenVisitor
import com.github.zero9178.mlirods.language.psi.TableGenBangOperator
import com.github.zero9178.mlirods.language.psi.TableGenIdentifierElement
import com.github.zero9178.mlirods.language.psi.TableGenIdentifierReference
import com.github.zero9178.mlirods.language.psi.TableGenRecord
import com.github.zero9178.mlirods.language.stubs.impl.TableGenBinaryIntegerValueNodeStub
import com.github.zero9178.mlirods.language.stubs.impl.TableGenBoolValueNodeStub
import com.github.zero9178.mlirods.language.stubs.impl.TableGenClassInstantiationValueNodeStub
import com.github.zero9178.mlirods.language.stubs.impl.TableGenIntegerValueNodeStub
import com.github.zero9178.mlirods.language.stubs.impl.TableGenStringValueNodeStub
import com.github.zero9178.mlirods.language.types.TableGenType
import com.github.zero9178.mlirods.language.types.TableGenUnknownType
import com.github.zero9178.mlirods.language.types.computeTypeOf
import com.github.zero9178.mlirods.language.types.typeOfAtomic
import com.github.zero9178.mlirods.language.values.TableGenIntegerValue
import com.github.zero9178.mlirods.language.values.TableGenStringValue
import com.github.zero9178.mlirods.language.values.TableGenUnknownValue
import com.github.zero9178.mlirods.language.values.TableGenValue
import com.github.zero9178.mlirods.model.TableGenCompilationContext
import com.github.zero9178.mlirods.model.projectContextDependentSuspendingCachedValue
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.util.concurrency.annotations.RequiresBlockingContext
import com.intellij.util.concurrency.annotations.RequiresReadLock

/**
 * Class passed around as context during a constant evaluation.
 * This is used to keep track of things such as template parameter to argument mapping or field mappings, and of the
 * [compilationContext] every reference met along the way is resolved in.
 *
 * Two contexts are considered equal if they originate from the same [source] and [compilationContext]. This allows
 * evaluation results to be cached per context (see [TableGenValueNodeEx.evaluate]).
 */
class TableGenEvaluationContext private constructor(
    val compilationContext: TableGenCompilationContext,
    private val source: Any?,
    val evaluateTemplateArgDeclInContext: suspend TableGenEvaluationContext.(TableGenTemplateArgDecl) -> TableGenValue,
    val evaluateFieldInContext: suspend TableGenEvaluationContext.(String) -> TableGenValue,
) {

    /**
     * Null-context. Template arguments and implicit field definitions yield unknown values.
     * This is the context that should be used for top-level evaluation and class-statements.
     */
    constructor(compilationContext: TableGenCompilationContext) : this(
        compilationContext,
        null,
        {
            TableGenUnknownValue
        },
        {
            TableGenUnknownValue
        },
    )

    constructor(
        defStatement: TableGenDefStatement,
        compilationContext: TableGenCompilationContext,
    ) : this(compilationContext, defStatement, {
        defStatement.allArgToTemplateArgMapping(compilationContext)[it]?.evaluate(this) ?: TableGenUnknownValue
    }, { fieldName ->
        evaluateFieldAssignments(defStatement, fieldName)
    })

    /**
     * Context of the anonymous record created by [instantiation] where it is written, i.e. in [outer]. The arguments of
     * the instantiation are evaluated in [outer], everything within the instantiated class in this context. This
     * includes the defaults of its template arguments not given an argument, which may refer to the arguments given.
     * The instantiation resolves in the compilation context of [outer].
     */
    constructor(instantiation: TableGenClassInstantiationValueNode, outer: TableGenEvaluationContext) : this(
        outer.compilationContext,
        InstantiationSource(instantiation, outer),
        { decl ->
            val argument = instantiation.argValueItemList.firstOrNull {
                it.referencedTemplateArgDecl(compilationContext) == decl
            }
            argument?.valueNode?.evaluate(outer)
                // 'allArgToTemplateArgMapping' only binds the template arguments of the base classes of the
                // instantiated class. Its own are bound by this instantiation alone and default to values of the
                // instantiated class.
                ?: (instantiation.referencedClass(compilationContext)?.allArgToTemplateArgMapping(compilationContext)
                    ?.get(decl) ?: decl.valueNode)?.evaluate(this)
                ?: TableGenUnknownValue
        },
        { fieldName ->
            evaluateFieldAssignments(instantiation.referencedClass(compilationContext), fieldName)
        })

    /**
     * An instantiation yields a different record in every context its arguments may be evaluated in.
     */
    private data class InstantiationSource(
        val instantiation: TableGenClassInstantiationValueNode,
        val outer: TableGenEvaluationContext,
    )

    override fun equals(other: Any?): Boolean = this === other || (other is TableGenEvaluationContext
            && source == other.source && compilationContext == other.compilationContext)

    override fun hashCode(): Int = 31 * source.hashCode() + compilationContext.hashCode()

    override fun toString() = when (source) {
        null -> "null context in $compilationContext"
        is TableGenDefStatement -> "context of 'def ${source.name}' in $compilationContext"
        // The identity tells apart instantiations of the same class.
        is InstantiationSource -> with(source.instantiation) {
            "context of '$className<...>'@${System.identityHashCode(this)} in ${source.outer}"
        }

        else -> "context of $source in $compilationContext"
    }

    private suspend fun evaluateFieldAssignments(record: TableGenRecord?, fieldName: String) =
        // TODO: Implement append and prepend semantics.
        record?.allFieldAssignments(compilationContext)[fieldName]?.lastOrNull()?.assignedValueNode?.evaluate(this)
            ?: TableGenUnknownValue
}

/**
 * The cached type of [element] in [context], see [TableGenValueNodeEx.type].
 */
private fun cachedTypeOf(
    element: TableGenValueNodeEx,
    context: TableGenCompilationContext,
): SuspendingCachedValue<TableGenType> =
    projectContextDependentSuspendingCachedValue(element, context, "type", onCycle = { TableGenUnknownType }) {
        computeTypeOf(it, context)
    }

/**
 * The cached value of [element] in [context], see [TableGenValueNodeEx.evaluate]. It is dropped along with the values of
 * every other context when anything changes, and with it [context], which would otherwise keep the PSI it originates
 * from alive.
 */
private fun cachedValueOf(
    element: TableGenValueNodeEx,
    context: TableGenEvaluationContext,
): SuspendingCachedValue<TableGenValue> =
    projectContextDependentSuspendingCachedValue(element, context, "value", onCycle = { TableGenUnknownValue }) {
        it.evaluateInner(context)
    }

interface TableGenValueNodeEx : PsiElement {
    /**
     * Generic accept method allowing 'TableGenVisitor's with return values.
     */
    fun <R> accept(visitor: TableGenVisitor<R>): R

    /**
     * Returns the type of this TableGen expression, resolving whatever it refers to within [context]. A type that
     * depends on itself is unknown.
     *
     * Like [evaluate], this is for code that is a coroutine, with [typeBlocking] for everything else.
     */
    suspend fun type(context: TableGenCompilationContext): TableGenType =
        cachedTypeOf(this, context).await()

    /**
     * [type] for callers that cannot suspend, see [evaluateBlocking].
     */
    @RequiresReadLock
    @RequiresBlockingContext
    fun typeBlocking(context: TableGenCompilationContext): TableGenType =
        cachedTypeOf(this, context).getBlocking()

    /**
     * Performs constant evaluation of this value within the given context. A value that depends on itself is unknown.
     *
     * Results are cached per [context] and invalidated on any PSI or include-context change. They are
     * [SuspendingCachedValue]s computed from each other. This is how code that is a coroutine asks for one; everything
     * else has [evaluateBlocking]. Implementations should not override this method but [evaluateInner] instead.
     */
    suspend fun evaluate(context: TableGenEvaluationContext): TableGenValue =
        cachedValueOf(this, context).await()

    /**
     * [evaluate] for callers that cannot suspend, i.e. the platform. See [SuspendingCachedValue.getBlocking] for what
     * is required of the read action this is called in.
     */
    @RequiresReadLock
    @RequiresBlockingContext
    fun evaluateBlocking(context: TableGenEvaluationContext): TableGenValue =
        cachedValueOf(this, context).getBlocking()

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

    // No need to cache for atomics.
    override suspend fun type(context: TableGenCompilationContext): TableGenType = typeOfAtomic(this)

    override fun typeBlocking(context: TableGenCompilationContext): TableGenType = typeOfAtomic(this)

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

interface TableGenClassInstantiationValueNodeEx : TableGenValueNodeEx {
    val stub: TableGenClassInstantiationValueNodeStub?
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

    /**
     * Returns the one declaration the identifier refers to within [context], or `null` if it refers to none or to
     * several, mirroring [TableGenIdentifierReference.resolve]. See
     * [TableGenIdentifierReference.findVisibleDeclarations].
     */
    @RequiresReadLock
    fun referencedDeclaration(context: TableGenCompilationContext): TableGenIdentifierElement?
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
