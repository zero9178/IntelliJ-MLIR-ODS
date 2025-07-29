package com.github.zero9178.mlirods.language.psi.impl

import com.github.zero9178.mlirods.MyIcons
import com.github.zero9178.mlirods.language.generated.psi.*
import com.github.zero9178.mlirods.language.psi.*
import com.github.zero9178.mlirods.language.psi.impl.TableGenPsiImplUtil.Companion.toString
import com.github.zero9178.mlirods.language.stubs.impl.TableGenDefNameIdentifierStub
import com.github.zero9178.mlirods.language.types.*
import com.intellij.extapi.psi.ASTDelegatePsiElement
import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.extapi.psi.StubBasedPsiElementBase
import com.intellij.navigation.ItemPresentation
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.util.NlsSafe
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vfs.toNioPathOrNull
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.PsiReference
import com.intellij.psi.StubBasedPsiElement
import com.intellij.psi.util.endOffset
import com.intellij.psi.util.startOffset
import javax.swing.Icon
import kotlin.io.path.relativeToOrNull

class TableGenPsiImplUtil {
    companion object {

        @JvmStatic
        fun getPlaceHolderText(element: TableGenAbstractClassStatement, textRange: TextRange): String =
            if (element.lAngle?.startOffset?.let { textRange.contains(it) } == true) {
                "<~>"
            } else {
                "{...}"
            }

        @JvmStatic
        fun getFoldingTextRanges(element: TableGenAbstractClassStatement): Collection<TextRange> {
            val list = mutableListOf<TextRange>()
            list.apply {
                add(
                    TextRange(
                        element.lAngle?.startOffset ?: return@apply, element.rAngle?.endOffset ?: return@apply
                    )
                )
            }
            list.apply {
                add(
                    TextRange(
                        element.lBrace?.startOffset ?: return@apply, element.rBrace?.endOffset ?: return@apply
                    )
                )
            }
            return list
        }

        @JvmStatic
        fun getPlaceHolderText(element: TableGenBlockStringValue, textRange: TextRange): String = "[{...}]"

        @JvmStatic
        fun getReference(element: TableGenIdentifierValueNode): PsiReference? {
            return TableGenDefReference(element)
        }

        @JvmStatic
        fun getReference(element: TableGenAbstractClassRef): PsiReference? {
            return TableGenClassReference(element)
        }

        @JvmStatic
        fun getClassName(element: TableGenAbstractClassRef): String {
            return element.classIdentifier.text
        }

        @JvmStatic
        fun getClassName(element: TableGenClassRef): String {
            return element.stub?.name ?: getClassName(element as TableGenAbstractClassRef)
        }

        @JvmStatic
        fun getClassName(element: TableGenClassTypeNode): String {
            return element.stub?.className
                ?: getClassName(element as TableGenAbstractClassRef)
        }

        @JvmStatic
        fun getReference(element: TableGenLetBodyItem): PsiReference? {
            return TableGenLetReference(element)
        }

        @JvmStatic
        fun getReference(element: TableGenFieldAccessValueNode): PsiReference? {
            return TableGenFieldAccessReference(element)
        }

        /**
         * Returns the string value that the TableGen string element corresponds to.
         */
        @JvmStatic
        fun getStringValue(stringElement: PsiElement): String {
            val text = stringElement.text
            if (text.startsWith("\"")) {
                // TODO: Decode escape sequences etc.
                return text.substring(1, text.length - 1)
            }
            // TODO: Check whether something needs to be done for code blocks.
            return text.substring(2, text.length - 2)
        }

        @JvmStatic
        fun getIntegerValue(integerElement: PsiElement?) = integerElement?.text?.toIntOrNull()

        /**
         * Workaround for [ASTDelegatePsiElement] to implement the same [toString] method
         * as [ASTWrapperPsiElement]
         */
        @JvmStatic
        fun toString(element: PsiElement): String {
            var name = element.javaClass.simpleName + "("
            name += if (element is StubBasedPsiElement<*>) element.iElementType
            else element.node.elementType
            name += ")"
            return name
        }

        /**
         * Returns the name of the given [TableGenDefNameIdentifierOwner] or null if it has no name.
         */
        @JvmStatic
        fun getName(element: TableGenDefNameIdentifierOwner): String? {
            ((element as? StubBasedPsiElementBase<*>)?.stub as? TableGenDefNameIdentifierStub)?.let {
                return it.name
            }
            return element.nameIdentifier?.text
        }

        /**
         * Replaces the name within [element] using [name].
         */
        @JvmStatic
        fun setName(element: TableGenDefNameIdentifierOwner, name: @NlsSafe String): PsiElement? {
            element.nameIdentifier?.replace(createIdentifier(element.project, name))
            return element
        }

        /**
         * Returns the absolute offset used to navigate to [element].
         * Always returns the offset of the identifier.
         */
        @JvmStatic
        fun getTextOffset(element: TableGenDefNameIdentifierOwner): Int {
            return element.nameIdentifier?.textOffset ?: element.startOffset
        }

        private class TableGenItemPresentation(
            private val element: PsiNamedElement,
            private val icon: Icon = MyIcons.TableGenIcon
        ) : ItemPresentation {
            override fun getPresentableText() = element.name

            override fun getIcon(unused: Boolean) = icon

            override fun getLocationString(): String? {
                val projectDir = element.project.guessProjectDir()?.toNioPathOrNull() ?: return null
                val file = element.containingFile?.virtualFile?.toNioPathOrNull() ?: return null

                // Only make the path relative to the project directory if it is a subdirectory.
                if (!file.startsWith(projectDir)) return file.toString()

                return file.relativeToOrNull(projectDir)?.toString()
            }
        }

        /**
         * Custom representation for any [PsiNamedElement] as it appears in 'Find Usages' and the
         * declaration list.
         */
        @JvmStatic
        fun getPresentation(element: PsiNamedElement): ItemPresentation? = TableGenItemPresentation(element)

        @JvmStatic
        fun getPresentation(element: TableGenDefNameIdentifierOwner): ItemPresentation? =
            TableGenItemPresentation(element, MyIcons.TableGenDef)

        @JvmStatic
        fun getPresentation(element: TableGenClassStatement): ItemPresentation? =
            TableGenItemPresentation(element, MyIcons.TableGenClass)

        @JvmStatic
        fun toType(element: TableGenBitTypeNode) = TableGenBitType

        @JvmStatic
        fun toType(element: TableGenIntTypeNode) = TableGenIntType

        @JvmStatic
        fun toType(element: TableGenStringTypeNode) = TableGenStringType

        @JvmStatic
        fun toType(element: TableGenDagTypeNode) = TableGenDagType

        @JvmStatic
        fun toType(element: TableGenCodeTypeNode) = TableGenStringType

        @JvmStatic
        fun toType(element: TableGenBitsTypeNode) = element.integer?.let {
            TableGenBitsType(getIntegerValue(it))
        } ?: TableGenUnknownType

        @JvmStatic
        fun toType(element: TableGenListTypeNode) = element.typeNode?.let {
            TableGenListType(it.toType())
        } ?: TableGenUnknownType

        @JvmStatic
        fun toType(element: TableGenClassTypeNode) = TableGenRecordType.create(element)

        @JvmStatic
        fun getType(element: TableGenIntegerValueNode) = TableGenIntType

        @JvmStatic
        fun getType(element: TableGenStringValueNode) = TableGenStringType

        @JvmStatic
        fun getType(element: TableGenBlockStringValue) = TableGenStringType

        @JvmStatic
        fun getType(element: TableGenBoolValueNode) = TableGenBitType

        @JvmStatic
        fun getType(element: TableGenListInitValueNode) = TableGenListType(
            element.typeNode?.toType() ?: element.valueNodeList.firstOrNull()?.type ?: TableGenUnknownType
        )

        @JvmStatic
        fun getType(element: TableGenDagInitValueNode) = TableGenDagType

        @JvmStatic
        fun getType(element: TableGenIdentifierValueNode): TableGenType {
            val resolve = element.reference?.resolve()
            return when (resolve) {
                is TableGenDefvarStatement -> resolve.valueNode?.type ?: TableGenUnknownType
                is TableGenFieldBodyItem -> resolve.typeNode.toType()
                is TableGenTemplateArgDecl -> resolve.typeNode.toType()
                is TableGenForeachOperatorValueNode -> (resolve.iterable?.type as? TableGenListType)?.elementType
                    ?: TableGenUnknownType

                is TableGenFoldlOperatorValueNode -> (resolve.iterable?.type as? TableGenListType)?.elementType
                    ?: TableGenUnknownType

                is TableGenFoldlAccumulator -> (resolve.parent as? TableGenFoldlOperatorValueNode)?.type
                    ?: TableGenUnknownType

                is TableGenDefStatement -> TableGenRecordType.create(resolve)
                else -> TableGenUnknownType
            }
        }

        @JvmStatic
        fun getType(element: TableGenFieldAccessValueNode): TableGenType {
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

        @JvmStatic
        fun getType(element: TableGenClassInstantiationValueNode) = TableGenRecordType.create(element)

        @JvmStatic
        fun getType(element: TableGenSliceAccessValueNode): TableGenType {
            val listType = element.valueNode.type as? TableGenListType ?: return TableGenUnknownType
            val sliceElements = element.sliceElementList
            return when (sliceElements.singleOrNull()) {
                is TableGenSingleSliceElement -> listType.elementType
                else -> listType
            }
        }

        @JvmStatic
        fun getType(element: TableGenConcatValueNode): TableGenType {
            val lhsType = element.leftOperand.type
            // Note: This ignores a lot of error cases for the sake of trying to guess user intent regarding the actual
            // intent.
            return when (lhsType) {
                is TableGenListType -> lhsType
                else -> TableGenStringType
            }
        }

        @JvmStatic
        fun getType(element: TableGenForeachOperatorValueNode) =
            TableGenListType(element.body?.type ?: TableGenUnknownType)

        @JvmStatic
        fun getType(element: TableGenFoldlOperatorValueNode): TableGenType = element.start?.type ?: TableGenUnknownType
    }
}