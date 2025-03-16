package com.github.zero9178.mlirods.language.psi.impl

import com.github.zero9178.mlirods.MyIcons
import com.github.zero9178.mlirods.language.generated.psi.TableGenAbstractClassRef
import com.github.zero9178.mlirods.language.generated.psi.TableGenAbstractClassStatement
import com.github.zero9178.mlirods.language.generated.psi.TableGenBlockStringValue
import com.github.zero9178.mlirods.language.generated.psi.TableGenClassRef
import com.github.zero9178.mlirods.language.generated.psi.TableGenClassStatement
import com.github.zero9178.mlirods.language.generated.psi.TableGenIdentifierValue
import com.github.zero9178.mlirods.language.generated.psi.TableGenLetBodyItem
import com.github.zero9178.mlirods.language.generated.psi.impl.TableGenClassRefImpl
import com.github.zero9178.mlirods.language.generated.psi.impl.TableGenIncludeDirectiveImpl
import com.github.zero9178.mlirods.language.psi.TableGenClassReference
import com.github.zero9178.mlirods.language.psi.TableGenDefNameIdentifierOwner
import com.github.zero9178.mlirods.language.psi.TableGenDefReference
import com.github.zero9178.mlirods.language.psi.TableGenLetReference
import com.github.zero9178.mlirods.language.psi.createIdentifier
import com.github.zero9178.mlirods.language.psi.impl.TableGenPsiImplUtil.Companion.toString
import com.github.zero9178.mlirods.language.stubs.impl.TableGenDefNameIdentifierStub
import com.intellij.extapi.psi.ASTDelegatePsiElement
import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.extapi.psi.StubBasedPsiElementBase
import com.intellij.navigation.ItemPresentation
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.util.NlsSafe
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vfs.toNioPathOrNull
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.StubBasedPsiElement
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.util.endOffset
import com.intellij.psi.util.startOffset
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
                        element.lAngle?.startOffset ?: return@apply,
                        element.rAngle?.endOffset ?: return@apply
                    )
                )
            }
            list.apply {
                add(
                    TextRange(
                        element.lBrace?.startOffset ?: return@apply,
                        element.rBrace?.endOffset ?: return@apply
                    )
                )
            }
            return list
        }

        @JvmStatic
        fun getPlaceHolderText(element: TableGenBlockStringValue, textRange: TextRange): String = "[{...}]"

        @JvmStatic
        fun getReference(element: TableGenIdentifierValue): PsiReference? {
            return TableGenDefReference(element)
        }

        @JvmStatic
        fun getReference(element: TableGenAbstractClassRef): PsiReference? {
            return TableGenClassReference(element)
        }

        @JvmStatic
        fun getReferencedClass(element: TableGenAbstractClassRef): TableGenClassStatement? {
            return element.reference?.resolve() as? TableGenClassStatement
        }

        @JvmStatic
        fun getClassName(element: TableGenAbstractClassRef): String {
            return element.classIdentifier.text
        }

        @JvmStatic
        fun getClassName(element: TableGenClassRefImpl): String {
            return element.greenStub?.name ?: getClassName(element as TableGenAbstractClassRef)
        }

        @JvmStatic
        fun getReference(element: TableGenLetBodyItem): PsiReference? {
            return TableGenLetReference(element)
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

        /**
         * Workaround for [ASTDelegatePsiElement] to implement the same [toString] method
         * as [ASTWrapperPsiElement]
         */
        @JvmStatic
        fun toString(element: ASTDelegatePsiElement): String {
            var name = element.javaClass.simpleName + "("
            name += if (element is StubBasedPsiElement<*>)
                element.elementType
            else
                element.node.elementType
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

        /**
         * Custom representation for any [TableGenDefNameIdentifierOwner] as it appears in 'Find Usages' and the
         * declaration list.
         */
        @JvmStatic
        fun getPresentation(element: TableGenDefNameIdentifierOwner): ItemPresentation? = object : ItemPresentation {
            override fun getPresentableText() = element.name

            override fun getIcon(unused: Boolean) = MyIcons.TableGenIcon

            override fun getLocationString(): String? {
                val projectDir = element.project.guessProjectDir()?.toNioPathOrNull() ?: return null
                val file = element.containingFile?.virtualFile?.toNioPathOrNull() ?: return null

                // Only make the path relative to the project directory if it is a subdirectory.
                if (!file.startsWith(projectDir)) return file.toString()

                return file.relativeToOrNull(projectDir)?.toString()
            }
        }
    }
}