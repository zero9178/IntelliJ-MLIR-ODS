package com.github.zero9178.mlirods.language.psi.impl

import com.github.zero9178.mlirods.language.generated.TableGenTypes
import com.github.zero9178.mlirods.language.generated.psi.TableGenLetBodyItem
import com.github.zero9178.mlirods.language.psi.TableGenLetReference
import com.github.zero9178.mlirods.language.psi.TableGenRecord
import com.github.zero9178.mlirods.language.stubs.impl.TableGenLetBodyItemStub
import com.intellij.extapi.psi.StubBasedPsiElementBase
import com.intellij.lang.ASTNode
import com.intellij.navigation.ItemPresentation
import com.intellij.openapi.util.NlsSafe
import com.intellij.psi.stubs.IStubElementType
import com.intellij.psi.util.elementType
import com.intellij.psi.util.parentOfType
import javax.swing.Icon

abstract class TableGenLetBodyItemMixin : StubBasedPsiElementBase<TableGenLetBodyItemStub>,
    TableGenLetBodyItem {

    constructor(node: ASTNode) : super(node)

    constructor(stub: TableGenLetBodyItemStub, stubType: IStubElementType<*, *>) : super(stub, stubType)

    override fun getName(): String? {
        greenStub?.let { return it.name }

        return fieldIdentifier?.text
    }

    override val fieldName: String?
        get() = name

    override fun getTextOffset(): Int {
        return fieldIdentifier?.textOffset ?: super.getTextOffset()
    }

    override fun getReference() = TableGenLetReference(this)

    override fun getPresentation(): ItemPresentation? {
        return object : ItemPresentation {
            private fun shouldPrintValue(): Boolean {
                val value = valueNode ?: return false

                // Arbitrary heuristic:
                if (value.textLength > 25) return false

                return when (value.elementType) {
                    // Print literal atoms only.
                    TableGenTypes.STRING_VALUE_NODE,
                    TableGenTypes.INTEGER_VALUE_NODE,
                    TableGenTypes.BOOL_VALUE_NODE,
                    TableGenTypes.UNDEF_VALUE_NODE -> true

                    else -> false
                }
            }

            override fun getLocationString(): @NlsSafe String? {
                return parentOfType<TableGenRecord>()?.name ?: super.getLocationString()
            }

            override fun getPresentableText(): @NlsSafe String {
                return "let $fieldName = ${if (shouldPrintValue()) valueNode?.text else "..."};"
            }

            override fun getIcon(unused: Boolean): Icon? {
                return null
            }
        }
    }
}