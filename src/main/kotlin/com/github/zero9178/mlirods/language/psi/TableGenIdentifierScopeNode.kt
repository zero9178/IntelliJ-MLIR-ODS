package com.github.zero9178.mlirods.language.psi

import com.intellij.psi.PsiElement
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.childrenOfType
import com.intellij.psi.util.parents

/**
 * Interface implemented by any [PsiElement] which creates a scope of elements found by 'identifier' lookup.
 */
interface TableGenIdentifierScopeNode : PsiElement {

    /**
     * Entry within an id map. An entry consists of two elements: The one to be found by the lookup, and its occurrence
     * within the source file. These are usually the same except for elements that may be found through inheritance
     * (e.g. fields), where the reference that imports them is the occurrence.
     */
    data class IdMapEntry(val element: TableGenIdentifierElement, val occurrence: PsiElement) {

        constructor(element: TableGenIdentifierElement) : this(element, element)

        /**
         * Compares the lexicographical position of [occurrence] with [element].
         */
        operator fun compareTo(element: PsiElement): Int = requireNotNull(occurrence.compareTo(element)) {
            "occurrences should have been in the same file"
        }

        /**
         * Compares the lexicographical position of the [occurrence]s.
         */
        operator fun compareTo(element: IdMapEntry) = compareTo(element.occurrence)
    }

    /**
     * Returns a map containing all elements that can be found by def lookup directly nested within this scope.
     * Elements with the same name are within a list ordered by lexical appearance of the occurrence element.
     * Every occurrence must therefore be in the same file as 'this' and must start after 'this'.
     */
    val directIdMap: Map<String, List<IdMapEntry>>
        get() = emptyMap<String, List<IdMapEntry>>()

    /**
     * Same as [directIdMap], but contains elements from every parent scope as well.
     * Lexicographical sorting of elements in lists is preserved.
     */
    val idMap: Map<String, List<IdMapEntry>>
        get() = CachedValuesManager.getProjectPsiDependentCache(this) {
            val parentMap = parentScope?.idMap
                ?: return@getProjectPsiDependentCache directIdMap

            val result = directIdMap.toMutableMap()
            parentMap.entries.forEach { (k, v) ->
                // Drop all elements from the parent that occur before 'this'.
                result.merge(k, v.takeWhile {
                    it < this
                }) { directList, parentList ->
                    parentList + directList
                }
            }
            result
        }

    /**
     * Returns true if [element], which must be a direct child of 'this', is within the scope created by 'this'.
     * If not, then 'this' is not the parent scope of [element] (despite being the parent element).
     *
     * This is used for Psi elements where the scope does not span all children, e.g. the iterable argument of
     * '!foreach'.
     */
    fun isWithinNewScope(element: PsiElement): Boolean = true

    /**
     * Returns the scope that 'this' is directly contained in or null if it has no parent scope.
     */
    val parentScope: TableGenIdentifierScopeNode?
        get() = getParentScope(this)

    companion object {
        /**
         * Returns the scope that [element] is directly contained in or null if it has no parent scope.
         */
        fun getParentScope(element: PsiElement): TableGenIdentifierScopeNode? =
            element.parents(withSelf = true).windowed(2, partialWindows = false).mapNotNull {
                val prev = it[0]
                val curr = it[1]
                if (curr !is TableGenIdentifierScopeNode)
                    null
                else if (curr.isWithinNewScope(prev))
                    curr
                else
                    null
            }.firstOrNull()
    }
}
