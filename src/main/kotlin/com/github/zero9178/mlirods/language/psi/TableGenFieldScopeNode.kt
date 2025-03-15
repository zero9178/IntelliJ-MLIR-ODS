package com.github.zero9178.mlirods.language.psi

import com.github.zero9178.mlirods.language.generated.psi.TableGenClassRef
import com.github.zero9178.mlirods.language.generated.psi.TableGenFieldBodyItem
import com.github.zero9178.mlirods.language.stubs.TableGenStubElementTypes
import com.github.zero9178.mlirods.language.stubs.disallowTreeLoading
import com.github.zero9178.mlirods.language.stubs.stubbedChildren
import com.intellij.extapi.psi.StubBasedPsiElementBase
import com.intellij.psi.PsiElement
import com.intellij.psi.util.endOffset
import com.intellij.psi.util.startOffset

/**
 * Map used to lookup fields within a [TableGenFieldScopeNode].
 * Will perform the lookup not only in [myRoot], but also any of its base classes.
 */
class FieldMap(private val myRoot: TableGenFieldScopeNode) {

    /**
     *
     */
    operator fun get(fieldName: PsiElement): TableGenFieldBodyItem? {
        val differentFile = myRoot.containingFile != fieldName.containingFile

        // Compute lazily as this performs a traversal up to the parent file.
        val startOffset = lazy(LazyThreadSafetyMode.PUBLICATION) {
            fieldName.startOffset
        }

        // Only consider fields defined before 'fieldName'.
        myRoot.directFields[fieldName.text]?.let {
            if (differentFile || it.endOffset < startOffset.value) return it
        }

        // Only consider base classes referenced before 'fieldName'.
        return myRoot.baseClassRefs.takeWhile {
            differentFile || it.endOffset < startOffset.value
        }.mapNotNull {
            it.referencedClass
        }.firstNotNullOfOrNull {
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
     * Returns a sequence of all references to base classes that should be used for field lookup.
     */
    val baseClassRefs: Sequence<TableGenClassRef>

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
