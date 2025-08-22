package com.github.zero9178.mlirods.language.psi.impl

import com.github.zero9178.mlirods.MyIcons
import com.github.zero9178.mlirods.language.generated.psi.*
import com.github.zero9178.mlirods.language.psi.*
import com.github.zero9178.mlirods.language.psi.impl.TableGenPsiImplUtil.Companion.toString
import com.github.zero9178.mlirods.language.stubs.impl.TableGenIdentifierElementStub
import com.github.zero9178.mlirods.language.types.*
import com.github.zero9178.mlirods.language.values.TableGenIntegerValue
import com.github.zero9178.mlirods.language.values.TableGenStringValue
import com.github.zero9178.mlirods.language.values.TableGenUnknownValue
import com.github.zero9178.mlirods.language.values.TableGenValue
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
import com.intellij.psi.util.childLeafs
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
            return TableGenIdentifierReference(element)
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
            var text = stringElement.text
            if (!text.startsWith("\"")) {
                text = text.drop(2)
                if (text.endsWith("}]"))
                    text = text.dropLast(2)

                return text
            }

            text = text.drop(1)
            if (text.endsWith('"'))
                text = text.dropLast(1)

            val result = StringBuilder()
            val iter = text.iterator()
            while (iter.hasNext()) {
                when (val c = iter.nextChar()) {
                    '\\' -> {
                        if (!iter.hasNext()) {
                            result.append(c)
                            continue
                        }
                        when (val c2 = iter.nextChar()) {
                            'n' -> result.append('\n')
                            't' -> result.append('\t')
                            '\\' -> result.append('\\')
                            '\'' -> result.append('\'')
                            '\"' -> result.append('\"')
                            else -> {
                                result.append(c)
                                result.append(c2)
                            }
                        }
                    }

                    else -> result.append(c)
                }
            }
            return result.toString()
        }

        /**
         * Returns a 64-bit integer parsed according to the lexer rules of TableGen.
         * [integerElement] should be a psi element whose text range corresponds to an INTEGER token optionally lead by
         * a minus sign.
         *
         * Returns null if it could not be parsed due to the value being too large to fit in 64 bit.
         */
        @JvmStatic
        fun getIntegerValue(integerElement: PsiElement): Long? {
            val text = integerElement.text

            data class Radii(val prefix: String, val radix: Int)

            // Non-decimal are always parsed as unsigned integers and denoted by their prefix.
            for (iter in arrayOf(Radii("0b", 2), Radii("0x", 16)))
                if (text.startsWith(iter.prefix))
                    return text.drop(iter.prefix.length).toULongOrNull(iter.radix)?.toLong()

            // Parse signed if lead by a minus.
            if (text.startsWith('-'))
                return text.toLongOrNull()

            return text.toULongOrNull()?.toLong()
        }

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
         * Returns the name of the given [TableGenIdentifierElement] or null if it has no name.
         */
        @JvmStatic
        fun getName(element: TableGenIdentifierElement): String? {
            ((element as? StubBasedPsiElementBase<*>)?.stub as? TableGenIdentifierElementStub)?.let {
                return it.name
            }
            return element.nameIdentifier?.text
        }

        /**
         * Replaces the name within [element] using [name].
         */
        @JvmStatic
        fun setName(element: TableGenIdentifierElement, name: @NlsSafe String): PsiElement? {
            element.nameIdentifier?.replace(createIdentifier(element.project, name))
            return element
        }

        /**
         * Returns the absolute offset used to navigate to [element].
         * Always returns the offset of the identifier.
         */
        @JvmStatic
        fun getTextOffset(element: TableGenIdentifierElement): Int {
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
        fun getPresentation(element: TableGenIdentifierElement): ItemPresentation? =
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
        fun toType(element: TableGenBitsTypeNode) =
            TableGenBitsType(element.integer?.let { getIntegerValue(it) })

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

        @JvmStatic
        fun evaluate(element: TableGenAtomicValue, context: TableGenEvaluationContext): TableGenValue =
            element.evaluateAtomic() ?: TableGenUnknownValue

        @JvmStatic
        fun evaluateAtomic(element: TableGenIntegerValueNode): TableGenIntegerValue? {
            val value = getIntegerValue(element) ?: return null
            return TableGenIntegerValue(value)
        }

        @JvmStatic
        fun evaluateAtomic(element: TableGenStringValueNode): TableGenStringValue {
            val res = element.childLeafs().fold("") { acc, c ->
                acc + getStringValue(c)
            }
            return TableGenStringValue(res)
        }

        @JvmStatic
        fun getDirectIdMap(element: TableGenForeachOperatorValueNode): Map<String, List<TableGenIdentifierScopeNode.IdMapEntry>> =
            buildMap {
                element.iterator?.let {
                    it.name?.let { name ->
                        put(name, listOf(TableGenIdentifierScopeNode.IdMapEntry(it)))
                    }
                }
            }

        @JvmStatic
        fun isWithinNewScope(self: TableGenForeachOperatorValueNode, element: PsiElement) = element == self.body

        @JvmStatic
        fun getDirectIdMap(element: TableGenFoldlOperatorValueNode): Map<String, List<TableGenIdentifierScopeNode.IdMapEntry>> =
            buildMap {
                listOfNotNull(element.iterator, element.accmulator).forEach {
                    it.name?.let { name ->
                        put(name, listOf(TableGenIdentifierScopeNode.IdMapEntry(it)))
                    }
                }
            }

        @JvmStatic
        fun isWithinNewScope(self: TableGenFoldlOperatorValueNode, element: PsiElement) = element == self.body

        @JvmStatic
        fun isDeclaration(self: TableGenClassStatement): Boolean {
            return self.templateArgDeclList.isEmpty() && self.classRefList.isEmpty() && !self.hasBody
        }

        @JvmStatic
        fun getHasBody(self: TableGenClassStatement): Boolean {
            self.stub?.let { return it.hasBody }

            return self.lBrace != null || self.rBrace != null
        }
    }
}