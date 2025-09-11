package com.github.zero9178.mlirods.language.types

import com.github.zero9178.mlirods.language.generated.psi.TableGenBangOperatorDefinition
import com.github.zero9178.mlirods.language.generated.psi.TableGenBlockStringValue
import com.github.zero9178.mlirods.language.generated.psi.TableGenBoolValueNode
import com.github.zero9178.mlirods.language.generated.psi.TableGenClassInstantiationValueNode
import com.github.zero9178.mlirods.language.generated.psi.TableGenConcatValueNode
import com.github.zero9178.mlirods.language.generated.psi.TableGenDagInitValueNode
import com.github.zero9178.mlirods.language.generated.psi.TableGenDefStatement
import com.github.zero9178.mlirods.language.generated.psi.TableGenDefvarStatement
import com.github.zero9178.mlirods.language.generated.psi.TableGenFieldAccessValueNode
import com.github.zero9178.mlirods.language.generated.psi.TableGenFieldBodyItem
import com.github.zero9178.mlirods.language.generated.psi.TableGenFoldlOperatorValueNode
import com.github.zero9178.mlirods.language.generated.psi.TableGenForeachOperatorValueNode
import com.github.zero9178.mlirods.language.generated.psi.TableGenIdentifierValueNode
import com.github.zero9178.mlirods.language.generated.psi.TableGenIntegerValueNode
import com.github.zero9178.mlirods.language.generated.psi.TableGenListInitValueNode
import com.github.zero9178.mlirods.language.generated.psi.TableGenSingleSliceElement
import com.github.zero9178.mlirods.language.generated.psi.TableGenSliceAccessValueNode
import com.github.zero9178.mlirods.language.generated.psi.TableGenStringValueNode
import com.github.zero9178.mlirods.language.generated.psi.TableGenTemplateArgDecl
import com.github.zero9178.mlirods.language.generated.psi.TableGenVisitor
import com.intellij.psi.PsiElement

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

    override fun visitBoolValueNode(element: TableGenBoolValueNode) = TableGenBitType

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
}