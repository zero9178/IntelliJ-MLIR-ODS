package com.github.zero9178.mlirods.language.types

import com.github.zero9178.mlirods.language.generated.psi.*
import com.github.zero9178.mlirods.language.psi.TableGenBangOperator.*
import com.github.zero9178.mlirods.language.psi.impl.TableGenAtomicValue
import com.github.zero9178.mlirods.language.psi.impl.TableGenEvaluationContext
import com.github.zero9178.mlirods.language.psi.impl.TableGenValueNodeEx
import com.github.zero9178.mlirods.language.values.TableGenIntegerValue
import kotlin.math.abs

private fun typeOf(node: TableGenValueNode?): TableGenType = node?.type ?: TableGenUnknownType

private fun typesOf(nodes: List<TableGenValueNode?>): List<TableGenType> = nodes.map(::typeOf)

private fun elementTypeOf(iterable: TableGenValueNode?): TableGenType =
    (typeOf(iterable) as? TableGenListType)?.elementType ?: TableGenUnknownType

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
 * Implements the type computation logic. Callers should use [TableGenValueNodeEx.type] which adds caching on top.
 */
internal fun computeTypeOf(element: TableGenValueNodeEx): TableGenType = when (element) {
    is TableGenAtomicValue -> typeOfAtomic(element)

    // A binary literal denotes one bit per digit written rather than an integer.
    is TableGenBinaryIntegerValueNode -> TableGenBitsType(element.numberOfBits.toLong())

    is TableGenListInitValueNode -> TableGenListType(
        element.typeNode?.toType() ?: typeOf(element.valueNodeList.firstOrNull())
    )

    is TableGenDagInitValueNode -> TableGenDagType

    is TableGenIdentifierValueNode -> typeOfIdentifier(element)

    is TableGenFieldAccessValueNode -> typeOfFieldAccess(element)

    is TableGenClassInstantiationValueNode -> TableGenRecordType.create(element)

    is TableGenSliceAccessValueNode -> when (val listType = typeOf(element.valueNode)) {
        is TableGenListType -> when (element.sliceElementList.singleOrNull()) {
            is TableGenSingleSliceElement -> listType.elementType
            else -> listType
        }

        else -> TableGenUnknownType
    }

    // Note: This ignores a lot of error cases for the sake of trying to guess user intent regarding the actual
    // intent.
    is TableGenConcatValueNode -> when (val lhsType = typeOf(element.leftOperand)) {
        is TableGenListType -> lhsType
        else -> TableGenStringType
    }

    is TableGenForeachOperatorValueNode -> {
        val (iterableType, bodyType) = typesOf(listOf(element.iterable, element.body))
        when (iterableType) {
            // Iterating a dag rebuilds the dag rather than collecting the body values into a list, making the body type
            // irrelevant for the result type.
            is TableGenDagType -> TableGenDagType
            else -> TableGenListType(bodyType)
        }
    }

    is TableGenFoldlOperatorValueNode -> typeOf(element.start)

    is TableGenSortOperatorValueNode -> typeOf(element.iterable)

    is TableGenFilterOperatorValueNode -> typeOf(element.iterable)

    is TableGenBitsInitValueNode -> typeOfBitsInit(element)

    is TableGenBitAccessValueNode -> typeOfBitAccess(element)

    is TableGenCondOperatorValueNode -> typesOf(element.condClauseList.map { it.thenValue }).commonType()

    is TableGenSwitchOperatorValueNode -> typesOf(element.switchClauseList.map {
        // The trailing clause without a ':' is the default value rather than a 'case: value' pair.
        if (it.hasColon) it.caseValue else it.caseKey
    }).commonType()

    is TableGenBangOperatorValueNode -> typeOfBangOperator(element)

    else -> TableGenUnknownType
}

private fun typeOfIdentifier(element: TableGenIdentifierValueNode): TableGenType =
    when (val resolve = element.reference?.resolve()) {
        is TableGenDefvarStatement -> typeOf(resolve.valueNode)
        is TableGenFieldBodyItem -> resolve.typeNode.toType()
        is TableGenTemplateArgDecl -> resolve.typeNode.toType()
        is TableGenBangOperatorDefinition -> {
            when (val parent = resolve.parent) {
                is TableGenForeachOperatorValueNode -> when (val iterableType = typeOf(parent.iterable)) {
                    is TableGenDagType -> TableGenDagType
                    is TableGenListType -> iterableType.elementType
                    else -> TableGenUnknownType
                }

                is TableGenFoldlOperatorValueNode ->
                    when (resolve) {
                        parent.accmulator -> typeOf(parent)
                        parent.iterator -> elementTypeOf(parent.iterable)
                        else -> TableGenUnknownType
                    }

                is TableGenSortOperatorValueNode -> elementTypeOf(parent.iterable)

                is TableGenFilterOperatorValueNode -> elementTypeOf(parent.iterable)

                else -> TableGenUnknownType
            }
        }

        is TableGenDefStatement -> TableGenRecordType.create(resolve)
        else -> TableGenUnknownType
    }

private fun typeOfFieldAccess(element: TableGenFieldAccessValueNode): TableGenType {
    val identifier = element.fieldName ?: return TableGenUnknownType
    return when (val type = typeOf(element.valueNode)) {
        is TableGenRecordType -> {
            val field = type.record?.fields?.get(identifier) ?: return TableGenUnknownType
            field.typeNode.toType()
        }

        else -> TableGenUnknownType
    }
}

private fun typeOfBitsInit(element: TableGenBitsInitValueNode): TableGenType {
    var numberOfBits = 0L
    for (type in typesOf(element.valueNodeList)) {
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

private fun typeOfBitAccess(element: TableGenBitAccessValueNode): TableGenType {
    // Selecting bits always yields a 'bits<n>' with one bit per selected bit, regardless of the operand type.
    val widths = element.rangePieceList.map { piece ->
        when (piece) {
            is TableGenSingleBit -> 1L
            is TableGenBitRange -> {
                val start = piece.start.constantInteger()
                val end = piece.end?.constantInteger()
                // Both bounds are inclusive and may be given in either order.
                abs((end ?: return@map null) - (start ?: return@map null)) + 1
            }

            else -> null
        }
    }
    // A single piece of unknown width leaves the total width unknown as well.
    return TableGenBitsType(widths.fold<Long?, Long?>(0L) { sum, width -> if (sum == null || width == null) null else sum + width })
}

private fun typeOfBangOperator(element: TableGenBangOperatorValueNode): TableGenType {
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

        IF -> typesOf(listOf(operands.getOrNull(1), operands.getOrNull(2))).commonType()

        // '!subst' yields whatever its third operand is.
        SUBST -> typeOf(operands.getOrNull(2))

        // '!head' yields the element type of its operand, while '!tail' yields the list itself.
        HEAD -> elementTypeOf(operands.firstOrNull())
        TAIL -> typeOf(operands.firstOrNull()) as? TableGenListType ?: TableGenUnknownType

        LISTSPLAT -> TableGenListType(typeOf(operands.firstOrNull()))

        // Both yield a list that all operand lists are compatible with.
        LISTCONCAT, LISTREMOVE -> typesOf(operands).commonType() as? TableGenListType ?: TableGenUnknownType

        // These are parsed into their own Psi node and have their type computed by their own branch.
        COND, SWITCH, FOREACH, FOLDL, FILTER, SORT -> TableGenUnknownType

        LISTFLATTEN -> {
            val elementType = (typeOf(operands.firstOrNull()) as? TableGenListType)?.elementType
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
private fun TableGenValueNode.constantInteger(): Long? =
    (evaluateBlocking(TableGenEvaluationContext()) as? TableGenIntegerValue)?.value
