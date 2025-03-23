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
     * Returns the field named [fieldName] within [myRoot] or null if no such field exists.
     * If [element] is not null, null is returned if the field has not yet been defined at the location of [element].
     */
    operator fun get(fieldName: String, element: PsiElement? = null): TableGenFieldBodyItem? = disallowTreeLoading {
        val differentFile = myRoot.containingFile != element?.containingFile

        // Compute lazily as this performs a traversal up to the parent file.
        val startOffset = lazy(LazyThreadSafetyMode.PUBLICATION) {
            element?.startOffset ?: Int.MAX_VALUE
        }

        // Only consider fields defined before 'fieldName'.
        myRoot.directFields[fieldName]?.let {
            if (differentFile || it.endOffset < startOffset.value) return@disallowTreeLoading it
        }

        // Only consider base classes referenced before 'fieldName'.
        myRoot.baseClassRefs.takeWhile {
            differentFile || it.endOffset < startOffset.value
        }.mapNotNull {
            it.referencedClass
        }.firstNotNullOfOrNull {
            FieldMap(it)[fieldName, element]
        }
    }

    /**
     * Returns the field named [fieldName] within [myRoot] or null if no such field exists.
     */
    operator fun get(fieldName: PsiElement) = get(fieldName.text, fieldName)
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
