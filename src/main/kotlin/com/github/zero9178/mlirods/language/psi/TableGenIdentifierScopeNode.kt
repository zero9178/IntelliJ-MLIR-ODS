package com.github.zero9178.mlirods.language.psi

import com.github.zero9178.mlirods.language.psi.TableGenIdentifierScopeNode.IdMapEntry
import com.intellij.psi.PsiElement

/**
 * Lazy view of the [TableGenIdentifierScopeNode.idMap] of [myScope], composed out of the id map of its parent scope and
 * its own [TableGenIdentifierScopeNode.directIdMap].
 */
private class ScopeIdMap(private val myScope: TableGenIdentifierScopeNode) :
    AbstractMap<String, List<IdMapEntry>>() {

    /**
     * The constituents are fetched once per view rather than once per lookup. As a view is created anew on every
     * [TableGenIdentifierScopeNode.idMap] access, what they capture never outlives the read action it was fetched in.
     */
    private val myParent: Map<String, List<IdMapEntry>> by lazy(LazyThreadSafetyMode.PUBLICATION) {
        myScope.parentScope?.idMap ?: emptyMap()
    }
    private val myDirect: Map<String, List<IdMapEntry>> by lazy(LazyThreadSafetyMode.PUBLICATION) {
        myScope.directIdMap
    }

    /**
     * Returns true if the parent scope contributes entries for [key], i.e. has any occurring before [myScope].
     */
    private fun inheritsEntries(key: String): Boolean {
        // The lists are ordered and never empty.
        val first = myParent[key]?.firstOrNull() ?: return false
        return first < myScope
    }

    /**
     * Returns the keys of this map: those of [myDirect] first, followed by the ones only the parent scope contributes
     * entries for.
     */
    private fun keySequence(): Sequence<String> = myDirect.keys.asSequence() + myParent.keys.asSequence().filter {
        it !in myDirect && inheritsEntries(it)
    }

    /**
     * Appends the entries of [key] visible within [scope] to [result], restricted to the ones occurring before
     * [before] if given.
     *
     * Every enclosing scope appends to the one list rather than returning a list of its own: a lookup then compares
     * and copies every entry once, no matter how deeply [scope] is nested.
     */
    private fun appendEntries(
        scope: TableGenIdentifierScopeNode, key: String, before: PsiElement?, result: MutableList<IdMapEntry>
    ) {
        // Everything inherited occurs before 'scope' and therefore before everything declared within it.
        scope.parentScope?.let { appendEntries(it, key, before = scope, result) }
        val direct = scope.directIdMap[key] ?: return
        if (before == null) {
            result.addAll(direct)
            return
        }
        for (entry in direct) {
            if (entry >= before) break
            result.add(entry)
        }
    }

    override fun get(key: String): List<IdMapEntry>? {
        val result = mutableListOf<IdMapEntry>()
        appendEntries(myScope, key, before = null, result)
        return result.ifEmpty { null }
    }

    override fun containsKey(key: String) = get(key) != null

    override val size: Int
        get() = keySequence().count()

    override val keys: Set<String>
        get() = object : AbstractSet<String>() {
            override val size get() = this@ScopeIdMap.size

            override fun contains(element: String) = containsKey(element)

            override fun iterator() = keySequence().iterator()
        }

    override val entries: Set<Map.Entry<String, List<IdMapEntry>>>
        get() = object : AbstractSet<Map.Entry<String, List<IdMapEntry>>>() {
            override val size get() = this@ScopeIdMap.size

            override fun iterator(): Iterator<Map.Entry<String, List<IdMapEntry>>> = keySequence().map {
                java.util.AbstractMap.SimpleImmutableEntry(it, getValue(it))
            }.iterator()
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
     */
    val directIdMap: Map<String, List<IdMapEntry>>
        get() = emptyMap<String, List<IdMapEntry>>()

    /**
     * Same as [directIdMap], but contains elements from every parent scope as well.
     * Lexicographical sorting of elements in lists is preserved.
     *
     * The parent scopes are composed into the result lazily rather than merged into it, so unlike [directIdMap] the
     * returned map assembles the list of a key on lookup and iterating it costs one lookup per visible name. The
     * returned view derives everything from the composed maps on use and is created anew on every access, so it must
     * not be retained beyond the enclosing read action.
     */
    val idMap: Map<String, List<IdMapEntry>>
        get() = ScopeIdMap(this)

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
