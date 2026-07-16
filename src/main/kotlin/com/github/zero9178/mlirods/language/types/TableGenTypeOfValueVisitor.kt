package com.github.zero9178.mlirods.language.types

import com.github.zero9178.mlirods.language.generated.psi.TableGenBangOperatorDefinition
import com.github.zero9178.mlirods.language.generated.psi.TableGenBlockStringValue
import com.github.zero9178.mlirods.language.generated.psi.TableGenBoolValueNode
import com.github.zero9178.mlirods.language.generated.psi.TableGenCastOperatorValueNode
import com.github.zero9178.mlirods.language.generated.psi.TableGenClassInstantiationValueNode
import com.github.zero9178.mlirods.language.generated.psi.TableGenConcatValueNode
import com.github.zero9178.mlirods.language.generated.psi.TableGenDagInitValueNode
import com.github.zero9178.mlirods.language.generated.psi.TableGenDefStatement
import com.github.zero9178.mlirods.language.generated.psi.TableGenDefvarStatement
import com.github.zero9178.mlirods.language.generated.psi.TableGenFieldAccessValueNode
import com.github.zero9178.mlirods.language.generated.psi.TableGenFieldBodyItem
import com.github.zero9178.mlirods.language.generated.psi.TableGenFilterOperatorValueNode
import com.github.zero9178.mlirods.language.generated.psi.TableGenFoldlOperatorValueNode
import com.github.zero9178.mlirods.language.generated.psi.TableGenForeachOperatorValueNode
import com.github.zero9178.mlirods.language.generated.psi.TableGenSortOperatorValueNode
import com.github.zero9178.mlirods.language.generated.psi.TableGenIdentifierValueNode
import com.github.zero9178.mlirods.language.generated.psi.TableGenIntegerValueNode
import com.github.zero9178.mlirods.language.generated.psi.TableGenListInitValueNode
import com.github.zero9178.mlirods.language.generated.psi.TableGenSingleSliceElement
import com.github.zero9178.mlirods.language.generated.psi.TableGenSliceAccessValueNode
import com.github.zero9178.mlirods.language.generated.psi.TableGenStringValueNode
import com.github.zero9178.mlirods.language.generated.psi.TableGenTemplateArgDecl
import com.github.zero9178.mlirods.language.generated.psi.TableGenBangOperatorValueNode
import com.github.zero9178.mlirods.language.generated.psi.TableGenBitAccessValueNode
import com.github.zero9178.mlirods.language.generated.psi.TableGenBitRange
import com.github.zero9178.mlirods.language.generated.psi.TableGenBitsInitValueNode
import com.github.zero9178.mlirods.language.generated.psi.TableGenCondOperatorValueNode
import com.github.zero9178.mlirods.language.generated.psi.TableGenSingleBit
import com.github.zero9178.mlirods.language.generated.psi.TableGenSwitchOperatorValueNode
import com.github.zero9178.mlirods.language.generated.psi.TableGenUndefValueNode
import com.github.zero9178.mlirods.language.generated.psi.TableGenValueNode
import com.github.zero9178.mlirods.language.generated.psi.TableGenVisitor
import com.github.zero9178.mlirods.language.psi.TableGenBangOperator.*
import com.github.zero9178.mlirods.language.psi.impl.TableGenEvaluationContext
import com.github.zero9178.mlirods.language.values.TableGenIntegerValue
import com.intellij.psi.PsiElement
import kotlin.math.abs

/**
 * Visitor implementing the type computation logic.
 */
object TableGenTypeOfValueVisitor : TableGenVisitor<TableGenType>() {

    override fun visitPsiElement(o: PsiElement): TableGenType {
        return TableGenUnknownType
    }

    override fun visitIntegerValueNode(element: TableGenIntegerValueNode) = TableGenIntType

    override fun visitStringValueNode(element: TableGenStringValueNode) = TableGenStringType

    override fun visitBlockStringValue(element: TableGenBlockStringValue) = TableGenStringType

    // 'true'/'false' are syntactic sugar for the integer values 1 and 0.
    override fun visitBoolValueNode(element: TableGenBoolValueNode) = TableGenIntType

    override fun visitUndefValueNode(element: TableGenUndefValueNode) = TableGenUndefType

    override fun visitListInitValueNode(element: TableGenListInitValueNode) = TableGenListType(
        element.typeNode?.toType() ?: element.valueNodeList.firstOrNull()?.type ?: TableGenUnknownType
    )

    override fun visitDagInitValueNode(element: TableGenDagInitValueNode) = TableGenDagType

    override fun visitIdentifierValueNode(element: TableGenIdentifierValueNode): TableGenType {
        return when (val resolve = element.reference?.resolve()) {
            is TableGenDefvarStatement -> resolve.valueNode?.type ?: TableGenUnknownType
            is TableGenFieldBodyItem -> resolve.typeNode.toType()
            is TableGenTemplateArgDecl -> resolve.typeNode.toType()
            is TableGenBangOperatorDefinition -> {
                when (val parent = resolve.parent) {
                    is TableGenForeachOperatorValueNode ->
                        (parent.iterable?.type as? TableGenListType)?.elementType
                            ?: TableGenUnknownType

                    is TableGenFoldlOperatorValueNode ->
                        when (resolve) {
                            parent.accmulator -> parent.type
                            parent.iterator -> (parent.iterable?.type as? TableGenListType)?.elementType
                                ?: TableGenUnknownType

                            else -> TableGenUnknownType
                        }

                    is TableGenSortOperatorValueNode ->
                        (parent.iterable?.type as? TableGenListType)?.elementType
                            ?: TableGenUnknownType

                    is TableGenFilterOperatorValueNode ->
                        (parent.iterable?.type as? TableGenListType)?.elementType
                            ?: TableGenUnknownType

                    else -> TableGenUnknownType
                }
            }

            is TableGenDefStatement -> TableGenRecordType.create(resolve)
            else -> TableGenUnknownType
        }
    }

    override fun visitFieldAccessValueNode(element: TableGenFieldAccessValueNode): TableGenType {
        val identifier = element.fieldName ?: return TableGenUnknownType
        val type = element.valueNode.type
        return when (type) {
            is TableGenRecordType -> {
                val field = type.record?.fields?.get(identifier) ?: return TableGenUnknownType
                field.typeNode.toType()
            }

            else -> TableGenUnknownType
        }
    }

    override fun visitClassInstantiationValueNode(element: TableGenClassInstantiationValueNode) =
        TableGenRecordType.create(element)

    override fun visitSliceAccessValueNode(element: TableGenSliceAccessValueNode): TableGenType {
        val listType = element.valueNode.type as? TableGenListType ?: return TableGenUnknownType
        val sliceElements = element.sliceElementList
        return when (sliceElements.singleOrNull()) {
            is TableGenSingleSliceElement -> listType.elementType
            else -> listType
        }
    }

    override fun visitConcatValueNode(element: TableGenConcatValueNode): TableGenType {
        val lhsType = element.leftOperand.type
        // Note: This ignores a lot of error cases for the sake of trying to guess user intent regarding the actual
        // intent.
        return when (lhsType) {
            is TableGenListType -> lhsType
            else -> TableGenStringType
        }
    }

    override fun visitForeachOperatorValueNode(element: TableGenForeachOperatorValueNode) =
        TableGenListType(element.body?.type ?: TableGenUnknownType)

    override fun visitFoldlOperatorValueNode(element: TableGenFoldlOperatorValueNode): TableGenType =
        element.start?.type ?: TableGenUnknownType

    override fun visitSortOperatorValueNode(element: TableGenSortOperatorValueNode) =
        element.iterable?.type ?: TableGenUnknownType

    override fun visitFilterOperatorValueNode(element: TableGenFilterOperatorValueNode) =
        element.iterable?.type ?: TableGenUnknownType

    override fun visitCastOperatorValueNode(o: TableGenCastOperatorValueNode) =
        o.typeNode?.toType() ?: TableGenUnknownType

    override fun visitBitsInitValueNode(element: TableGenBitsInitValueNode): TableGenType {
        var numberOfBits = 0L
        for (value in element.valueNodeList) {
            numberOfBits += when (val type = value.type) {
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

    override fun visitBitAccessValueNode(element: TableGenBitAccessValueNode): TableGenType {
        // Selecting bits always yields a 'bits<n>' with one bit per selected bit, regardless of the operand type.
        var numberOfBits = 0L
        for (piece in element.rangePieceList) {
            numberOfBits += when (piece) {
                is TableGenSingleBit -> 1L
                is TableGenBitRange -> {
                    val start = piece.start.constantInteger ?: return TableGenBitsType(null)
                    val end = piece.end?.constantInteger ?: return TableGenBitsType(null)
                    // Both bounds are inclusive and may be given in either order.
                    abs(end - start) + 1
                }

                else -> return TableGenBitsType(null)
            }
        }
        return TableGenBitsType(numberOfBits)
    }

    override fun visitCondOperatorValueNode(element: TableGenCondOperatorValueNode) =
        element.condClauseList.map { it.thenValue?.type ?: TableGenUnknownType }.commonType()

    override fun visitSwitchOperatorValueNode(element: TableGenSwitchOperatorValueNode) =
        element.switchClauseList.map {
            // The trailing clause without a ':' is the default value rather than a 'case: value' pair.
            if (it.hasColon) it.caseValue?.type ?: TableGenUnknownType else it.caseKey.type
        }.commonType()

    override fun visitBangOperatorValueNode(element: TableGenBangOperatorValueNode): TableGenType {
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

            // '!getdagop' without a type argument yields a record of any class, which is not modelled as a type.
            GETDAGOP, GETDAGARG -> typeArgument ?: TableGenUnknownType

            IF -> commonType(
                operands.getOrNull(1)?.type ?: TableGenUnknownType,
                operands.getOrNull(2)?.type ?: TableGenUnknownType,
            )

            // '!subst' yields whatever its third operand is.
            SUBST -> operands.getOrNull(2)?.type ?: TableGenUnknownType

            // '!head' yields the element type of its operand, while '!tail' yields the list itself.
            HEAD -> (operands.firstOrNull()?.type as? TableGenListType)?.elementType ?: TableGenUnknownType
            TAIL -> operands.firstOrNull()?.type as? TableGenListType ?: TableGenUnknownType

            LISTSPLAT -> TableGenListType(operands.firstOrNull()?.type ?: TableGenUnknownType)

            // Both yield a list that all operand lists are compatible with.
            LISTCONCAT, LISTREMOVE -> operands.map { it.type }.commonType() as? TableGenListType
                ?: TableGenUnknownType

            LISTFLATTEN -> {
                val elementType = (operands.firstOrNull()?.type as? TableGenListType)?.elementType
                    ?: return TableGenUnknownType
                // 'list<list<x>>' is flattened to 'list<x>', while a list of non-lists is left as-is.
                elementType as? TableGenListType ?: TableGenListType(elementType)
            }
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
private val TableGenValueNode.constantInteger: Long?
    get() = (evaluate(TableGenEvaluationContext()) as? TableGenIntegerValue)?.value
