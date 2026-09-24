package com.github.zero9178.mlirods.language.types

import com.github.zero9178.mlirods.language.generated.psi.*
import com.github.zero9178.mlirods.language.psi.TableGenBangOperator.*
import com.github.zero9178.mlirods.language.psi.impl.TableGenAtomicValue
import com.github.zero9178.mlirods.language.psi.impl.TableGenEvaluationContext
import com.github.zero9178.mlirods.language.psi.impl.TableGenValueNodeEx
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import com.github.zero9178.mlirods.language.values.TableGenIntegerValue
import com.github.zero9178.mlirods.language.values.TableGenRecordValue
import com.github.zero9178.mlirods.model.TableGenCompilationContext
import kotlin.math.abs

private suspend fun typeOf(node: TableGenValueNode?, context: TableGenCompilationContext): TableGenType =
    node?.type(context) ?: TableGenUnknownType

/**
 * Requests the types of all [nodes] at once: none depends on another, so they are computed in parallel where the
 * dispatcher has the threads for it.
 */
private suspend fun typesOf(
    nodes: List<TableGenValueNode?>, context: TableGenCompilationContext,
): List<TableGenType> = coroutineScope {
    nodes.map { async { it?.type(context) ?: TableGenUnknownType } }.awaitAll()
}

private suspend fun elementTypeOf(iterable: TableGenValueNode?, context: TableGenCompilationContext): TableGenType =
    (typeOf(iterable, context) as? TableGenListType)?.elementType ?: TableGenUnknownType

/**
 * Returns the type of values whose type does not depend on any other.
 */
internal fun typeOfAtomic(element: TableGenAtomicValue): TableGenType = when (element) {
    is TableGenIntegerValueNode -> TableGenIntType
    is TableGenStringValueNode -> TableGenStringType
    // 'true'/'false' are syntactic sugar for the integer values 1 and 0.
    is TableGenBoolValueNode -> TableGenIntType
    is TableGenUndefValueNode -> TableGenUndefType
    else -> TableGenUnknownType
}

/**
 * Implements the type computation logic, resolving whatever a value refers to within [context]. Callers should use
 * [TableGenValueNodeEx.type] which adds caching on top.
 */
internal suspend fun computeTypeOf(
    element: TableGenValueNodeEx, context: TableGenCompilationContext,
): TableGenType = when (element) {
    is TableGenAtomicValue -> typeOfAtomic(element)

    // A binary literal denotes one bit per digit written rather than an integer.
    is TableGenBinaryIntegerValueNode -> TableGenBitsType(element.numberOfBits.toLong())

    // Without an explicit element type, the element type is the one all elements can be used as.
    // Like '?', an empty list adopts the element type expected by its context, making undef the neutral start.
    is TableGenListInitValueNode -> TableGenListType(
        element.typeNode?.toType()
            ?: typesOf(element.valueNodeList, context)
                .fold<_, TableGenType>(TableGenUndefType) { t1, t2 -> commonType(t1, t2, context) }
    )

    is TableGenDagInitValueNode -> TableGenDagType

    is TableGenIdentifierValueNode -> typeOfIdentifier(element, context)

    is TableGenFieldAccessValueNode -> typeOfFieldAccess(element, context)

    is TableGenClassInstantiationValueNode -> TableGenRecordType.create(element)

    is TableGenSliceAccessValueNode -> when (val listType = typeOf(element.valueNode, context)) {
        is TableGenListType -> when (element.sliceElementList.singleOrNull()) {
            is TableGenSingleSliceElement -> listType.elementType
            else -> listType
        }

        else -> TableGenUnknownType
    }

    // Note: This ignores a lot of error cases for the sake of trying to guess user intent regarding the actual
    // intent.
    is TableGenConcatValueNode -> when (val lhsType = typeOf(element.leftOperand, context)) {
        is TableGenListType -> lhsType
        else -> TableGenStringType
    }

    is TableGenForeachOperatorValueNode -> {
        val (iterableType, bodyType) = typesOf(listOf(element.iterable, element.body), context)
        when (iterableType) {
            // Iterating a dag rebuilds the dag rather than collecting the body values into a list, making the body type
            // irrelevant for the result type.
            is TableGenDagType -> TableGenDagType
            else -> TableGenListType(bodyType)
        }
    }

    is TableGenFoldlOperatorValueNode -> typeOf(element.start, context)

    is TableGenSortOperatorValueNode -> typeOf(element.iterable, context)

    is TableGenFilterOperatorValueNode -> typeOf(element.iterable, context)

    is TableGenBitsInitValueNode -> typeOfBitsInit(element, context)

    is TableGenBitAccessValueNode -> typeOfBitAccess(element, context)

    is TableGenCondOperatorValueNode -> typesOf(element.condClauseList.map { it.thenValue }, context).commonType(context)

    is TableGenSwitchOperatorValueNode -> typesOf(element.switchClauseList.map {
        // The trailing clause without a ':' is the default value rather than a 'case: value' pair.
        if (it.hasColon) it.caseValue else it.caseKey
    }, context).commonType(context)

    is TableGenBangOperatorValueNode -> typeOfBangOperator(element, context)

    else -> TableGenUnknownType
}

private suspend fun typeOfIdentifier(
    element: TableGenIdentifierValueNode, context: TableGenCompilationContext,
): TableGenType =
    when (val resolve = element.referencedDeclaration(context)) {
        // The value may again be an identifier referring to another 'defvar', and so on, to any length no matter how
        // deep the AST is. Launched, its type is computed from the bottom of the stack of some thread instead of on top
        // of ours.
        is TableGenDefvarStatement -> coroutineScope { async { typeOf(resolve.valueNode, context) }.await() }
        is TableGenFieldBodyItem -> resolve.typeNode.toType()
        is TableGenTemplateArgDecl -> resolve.typeNode.toType()
        is TableGenBangOperatorDefinition -> {
            when (val parent = resolve.parent) {
                is TableGenForeachOperatorValueNode -> when (val iterableType = typeOf(parent.iterable, context)) {
                    is TableGenDagType -> TableGenDagType
                    is TableGenListType -> iterableType.elementType
                    else -> TableGenUnknownType
                }

                is TableGenFoldlOperatorValueNode ->
                    when (resolve) {
                        parent.accmulator -> typeOf(parent, context)
                        parent.iterator -> elementTypeOf(parent.iterable, context)
                        else -> TableGenUnknownType
                    }

                is TableGenSortOperatorValueNode -> elementTypeOf(parent.iterable, context)

                is TableGenFilterOperatorValueNode -> elementTypeOf(parent.iterable, context)

                else -> TableGenUnknownType
            }
        }

        is TableGenDefStatement -> TableGenRecordType.create(resolve)
        else -> TableGenUnknownType
    }

private suspend fun typeOfFieldAccess(
    element: TableGenFieldAccessValueNode, context: TableGenCompilationContext,
): TableGenType {
    val identifier = element.fieldName ?: return TableGenUnknownType
    val record = (typeOf(element.valueNode, context) as? TableGenRecordType)?.record(context)
        ?: return TableGenUnknownType
    val declaredType = record.fields(context)[identifier]?.typeNode?.toType() ?: return TableGenUnknownType
    // Only the fields of a 'def' have a value to go by. Any other record (e.g. a template argument of class type) is
    // only known to be of the declared type.
    if (record !is TableGenDefStatement) return declaredType

    // TableGen folds the field access of a 'def' into the field's value, making the type of the expression that of the
    // value rather than the declared type of the field. Since values are converted to the declared type on assignment,
    // this only makes a difference for records (and lists thereof), which keep their more derived type.
    // The value may again be a field access of another 'def', and so on, to any length no matter how deep the AST is.
    // Launched, its type is computed from the bottom of the stack of some thread instead of on top of ours.
    // Fields may also be defined in terms of each other, e.g. 'def A { Foo f = B.g; }' and 'def B { Foo g = A.f; }'.
    // TableGen rejects such cycles. The types on them are unknown, leaving the declared type as the best guess.
    val valueType = coroutineScope { async { TableGenRecordValue(record, context).fields[identifier].type }.await() }
    return declaredType.refinedBy(valueType, context)
}

private suspend fun typeOfBitsInit(element: TableGenBitsInitValueNode, context: TableGenCompilationContext): TableGenType {
    var numberOfBits = 0L
    for (type in typesOf(element.valueNodeList, context)) {
        numberOfBits += when (type) {
            // A 'bits<n>' operand contributes all of its bits at once.
            is TableGenBitsType -> type.numberOfBits ?: return TableGenBitsType(null)
            // An operand of unknown type may yet be a 'bits<n>' contributing more than one bit, leaving the
            // total width unknown as well.
            is TableGenUnknownType -> return TableGenBitsType(null)
            else -> 1L
        }
    }
    return TableGenBitsType(numberOfBits)
}

private suspend fun typeOfBitAccess(element: TableGenBitAccessValueNode, context: TableGenCompilationContext): TableGenType {
    // Selecting bits always yields a 'bits<n>' with one bit per selected bit, regardless of the operand type.
    val widths = element.rangePieceList.map { piece ->
        when (piece) {
            is TableGenSingleBit -> 1L
            is TableGenBitRange -> {
                val start = piece.start.constantInteger(context)
                val end = piece.end?.constantInteger(context)
                // Both bounds are inclusive and may be given in either order.
                abs((end ?: return@map null) - (start ?: return@map null)) + 1
            }

            else -> null
        }
    }
    // A single piece of unknown width leaves the total width unknown as well.
    return TableGenBitsType(widths.fold<Long?, Long?>(0L) { sum, width -> if (sum == null || width == null) null else sum + width })
}

private suspend fun typeOfBangOperator(
    element: TableGenBangOperatorValueNode, context: TableGenCompilationContext,
): TableGenType {
    val operands = element.valueNodeList
    // The optional type argument of e.g. '!getdagarg<int>'.
    val typeArgument = element.typeNode?.toType()
    return when (element.operator ?: return TableGenUnknownType) {
        // Comparisons yield a single bit rather than an 'int'.
        EQ, NE, LE, LT, GE, GT, MATCH -> TableGenBitType

        ADD, SUB, MUL, DIV, NOT, LOGTWO, AND, OR, XOR, SHL, SRA, SRL, SIZE, EMPTY, FIND, ISA, EXISTS, INITIALIZED ->
            TableGenIntType

        REPR, TOLOWER, TOUPPER, STRCONCAT, INTERLEAVE, SUBSTR, GETDAGOPNAME, GETDAGNAME -> TableGenStringType

        DAG, CON, SETDAGOP, SETDAGOPNAME, SETDAGARG, SETDAGNAME -> TableGenDagType

        RANGE -> TableGenListType(TableGenIntType)

        INSTANCES -> TableGenListType(typeArgument ?: TableGenUnknownType)

        // '!cast' and '!getdagarg' yield exactly their type argument, while '!getdagop' without one yields a record
        // of any class, which is not modelled as a type.
        CAST, GETDAGOP, GETDAGARG -> typeArgument ?: TableGenUnknownType

        IF -> typesOf(listOf(operands.getOrNull(1), operands.getOrNull(2)), context).commonType(context)

        // '!subst' yields whatever its third operand is.
        SUBST -> typeOf(operands.getOrNull(2), context)

        // '!head' yields the element type of its operand, while '!tail' yields the list itself.
        HEAD -> elementTypeOf(operands.firstOrNull(), context)
        TAIL -> typeOf(operands.firstOrNull(), context) as? TableGenListType ?: TableGenUnknownType

        LISTSPLAT -> TableGenListType(typeOf(operands.firstOrNull(), context))

        // Both yield a list that all operand lists are compatible with.
        LISTCONCAT, LISTREMOVE -> typesOf(operands, context).commonType(context) as? TableGenListType ?: TableGenUnknownType

        // These are parsed into their own Psi node and have their type computed by their own branch.
        COND, SWITCH, FOREACH, FOLDL, FILTER, SORT -> TableGenUnknownType

        LISTFLATTEN -> {
            val elementType = (typeOf(operands.firstOrNull(), context) as? TableGenListType)?.elementType
                ?: return TableGenUnknownType
            // 'list<list<x>>' is flattened to 'list<x>', while a list of non-lists is left as-is.
            elementType as? TableGenListType ?: TableGenListType(elementType)
        }
    }
}

/**
 * Returns the value of this node if it folds to an integer or null otherwise.
 *
 * A bit range bound does not have to be an integer literal, it only has to fold to an integer. TableGen folds it
 * without a current record, so a global 'defvar' folds while a template argument or field never does, even if it is an
 * integer. Evaluating in the null context yields exactly that behaviour.
 */
private suspend fun TableGenValueNode.constantInteger(context: TableGenCompilationContext): Long? =
    (evaluate(TableGenEvaluationContext(context)) as? TableGenIntegerValue)?.value
