package com.github.zero9178.mlirods.language.psi.impl

import com.github.zero9178.mlirods.language.generated.psi.TableGenAbstractClassStatement
import com.github.zero9178.mlirods.language.generated.psi.TableGenBlockStringValue
import com.github.zero9178.mlirods.language.generated.psi.TableGenIncludeDirective
import com.github.zero9178.mlirods.language.generated.psi.impl.TableGenIncludeDirectiveImpl
import com.github.zero9178.mlirods.language.psi.TableGenIncludeReferenceSet
import com.github.zero9178.mlirods.language.psi.impl.TableGenPsiImplUtil.Companion.toString
import com.intellij.extapi.psi.ASTDelegatePsiElement
import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.resolve.reference.impl.providers.FileReference
import com.intellij.psi.util.endOffset
import com.intellij.psi.util.startOffset

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
        fun getReferences(element: TableGenIncludeDirective): Array<FileReference>? {
            return TableGenIncludeReferenceSet(element).allReferences
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
        fun getIncludeSuffix(element: TableGenIncludeDirectiveImpl): String {
            return element.stub?.includeSuffix ?: element.string?.let { getStringValue(it) } ?: ""
        }

        /**
         * Workaround for [ASTDelegatePsiElement] to implement the same [toString] method
         * as [ASTWrapperPsiElement]
         */
        @JvmStatic
        fun toString(element: ASTDelegatePsiElement): String {
            return element.javaClass.simpleName + "(" + element.node.elementType + ")"
        }
    }
}