package com.github.zero9178.mlirods.language.patterns

import com.github.zero9178.mlirods.language.generated.psi.TableGenLetBodyItem
import com.github.zero9178.mlirods.language.generated.psi.TableGenStringValueNode
import com.intellij.patterns.PatternCondition
import com.intellij.patterns.PsiElementPattern
import com.intellij.patterns.StandardPatterns
import com.intellij.util.ProcessingContext

/**
 * Pattern matching a [TableGenLetBodyItem].
 */
class TableGenLetBodyItemCapture : PsiElementPattern.Capture<TableGenLetBodyItem>(TableGenLetBodyItem::class.java) {
    /**
     * Creates a new refined pattern that additionally requires the [TableGenLetBodyItem] to assign to a field of a
     * certain name.
     */
    fun withFieldName(name: String) = with(object : PatternCondition<TableGenLetBodyItem>("withFieldName") {
        override fun accepts(
            item: TableGenLetBodyItem, context: ProcessingContext?
        ) = item.fieldName == name
    })
}

class TableGenPatterns : StandardPatterns() {
    companion object {
        /**
         * Pattern matching any TableGen string.
         */
        @JvmStatic
        fun tableGenStringValue() =
            object : PsiElementPattern.Capture<TableGenStringValueNode>(TableGenStringValueNode::class.java) {}

        /**
         * Pattern matching a [TableGenLetBodyItem].
         */
        @JvmStatic
        fun tableGenLetBodyItem() = TableGenLetBodyItemCapture()
    }
}
