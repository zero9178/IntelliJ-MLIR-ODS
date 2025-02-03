package com.github.zero9178.mlirods.language.psi.impl

import com.github.zero9178.mlirods.language.generated.psi.*
import com.intellij.openapi.util.TextRange
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
    }
}