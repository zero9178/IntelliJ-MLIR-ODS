package com.github.zero9178.mlirods.language.psi

import com.github.zero9178.mlirods.language.generated.psi.TableGenFieldBodyItem
import com.github.zero9178.mlirods.language.stubs.TableGenStubElementTypes
import com.github.zero9178.mlirods.language.stubs.disallowTreeLoading
import com.github.zero9178.mlirods.language.stubs.stubbedChildren
import com.intellij.extapi.psi.StubBasedPsiElementBase
import com.intellij.util.AstLoadingFilter

/**
 * Map used to lookup fields within a [TableGenFieldScopeNode].
 * Will perform the lookup not only in [myRoot], but also any of its base classes.
 */
class FieldMap(private val myRoot: TableGenFieldScopeNode) {
    operator fun get(fieldName: String): TableGenFieldBodyItem? {
        myRoot.directFields[fieldName]?.let { return it }

        return myRoot.baseClasses.firstNotNullOfOrNull {
            FieldMap(it)[fieldName]
        }
    }
}

/**
 * Interface used by any [TableGenIdentifierScopeNode] which may also contain fields.
 */
interface TableGenFieldScopeNode : TableGenIdentifierScopeNode {
    /**
     * Returns a map of all fields that are defined directly within this.
     */
    val directFields: Map<String, TableGenFieldBodyItem>

    /**
     * Returns a sequence returning all base classes of this that should be used for field lookup.
     */
    val baseClasses: Sequence<TableGenFieldScopeNode>

    /**
     * Returns a map for field lookup.
     */
    val fields: FieldMap
        get() = FieldMap(this)
}

/**
 * Computes the direct fields of a [TableGenFieldScopeNode] that is a parent stub of fields.
 */
fun <T> T.computeDirectFields(): Map<String, TableGenFieldBodyItem> where T : TableGenFieldScopeNode, T : StubBasedPsiElementBase<*> =
    disallowTreeLoading {
        stubbedChildren(TableGenStubElementTypes.FIELD_BODY_ITEM).associateBy {
            it.fieldIdentifier!!.text
        }
    }
