package com.github.zero9178.mlirods.language.psi

import com.github.zero9178.mlirods.language.psi.TableGenIdentifierScopeNode.IdMapEntry
import com.github.zero9178.mlirods.model.TableGenCompilationContext
import com.intellij.psi.PsiElement

/**
 * Appends the entries of [key] visible within [scope] within [context] to [result], restricted to the ones occurring
 * before [before] if given.
 *
 * Every enclosing scope appends to the one list rather than returning a list of its own: a lookup then compares and
 * copies every entry once, no matter how deeply [scope] is nested.
 */
private suspend fun appendEntries(
    scope: TableGenIdentifierScopeNode,
    key: String,
    context: TableGenCompilationContext,
    before: PsiElement?,
    result: MutableList<IdMapEntry>,
) {
    // Everything inherited occurs before 'scope' and therefore before everything declared within it.
    scope.parentScope?.let { appendEntries(it, key, context, before = scope, result) }
    val direct = scope.directIdMap(context)[key] ?: return
    if (before == null) {
        result.addAll(direct)
        return
    }
    for (entry in direct) {
        if (entry >= before) break
        result.add(entry)
    }
}

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
     *
     * [context] only matters to scopes with entries that stem from resolution, such as the fields a record inherits
     * from its base classes; every other scope ignores it.
     */
    suspend fun directIdMap(context: TableGenCompilationContext): Map<String, List<IdMapEntry>> = emptyMap()

    /**
     * Returns the entries of [key] within [context] that are visible within this scope, i.e. those of [directIdMap]
     * and those of every parent scope occurring before this, or null if there are none. Lexicographical sorting of the
     * entries is preserved.
     */
    suspend fun idMapEntries(key: String, context: TableGenCompilationContext): List<IdMapEntry>? {
        val result = mutableListOf<IdMapEntry>()
        appendEntries(this, key, context, before = null, result)
        return result.ifEmpty { null }
    }

    /**
     * Same as [directIdMap], but contains the entries of every parent scope as well, see [idMapEntries]. Unlike
     * [idMapEntries], this looks up every name visible within this scope.
     */
    suspend fun idMap(context: TableGenCompilationContext): Map<String, List<IdMapEntry>> {
        val keys = linkedSetOf<String>()
        var scope: TableGenIdentifierScopeNode? = this
        while (scope != null) {
            keys += scope.directIdMap(context).keys
            scope = scope.parentScope
        }
        return buildMap {
            for (key in keys) idMapEntries(key, context)?.let { put(key, it) }
        }
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
        fun getParentScope(element: PsiElement): TableGenIdentifierScopeNode? {
            var prev = element
            var curr = element.parent
            while (curr != null) {
                if (curr is TableGenIdentifierScopeNode && curr.isWithinNewScope(prev)) return curr
                prev = curr
                curr = curr.parent
            }
            return null
        }
    }
}
