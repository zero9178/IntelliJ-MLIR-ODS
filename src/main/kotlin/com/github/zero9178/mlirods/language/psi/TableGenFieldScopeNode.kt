package com.github.zero9178.mlirods.language.psi

import com.github.zero9178.mlirods.language.generated.psi.TableGenClassRef
import com.github.zero9178.mlirods.language.generated.psi.TableGenClassStatement
import com.github.zero9178.mlirods.language.generated.psi.TableGenFieldBodyItem
import com.github.zero9178.mlirods.language.generated.psi.TableGenLetStatement
import com.github.zero9178.mlirods.language.generated.psi.TableGenTemplateArgDecl
import com.github.zero9178.mlirods.language.generated.psi.TableGenValueNode
import com.github.zero9178.mlirods.language.psi.impl.TableGenAbstractLetItem
import com.github.zero9178.mlirods.language.stubs.disallowTreeLoading
import com.github.zero9178.mlirods.model.TableGenCompilationContext
import com.github.zero9178.mlirods.model.getProjectContextDependentCache
import com.github.zero9178.mlirods.model.projectContextDependentSuspendingCachedValue
import com.intellij.psi.PsiElement
import com.intellij.psi.util.parentsOfType
import com.intellij.util.concurrency.annotations.RequiresReadLock
import java.util.AbstractMap.SimpleImmutableEntry
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

/**
 * Map used to lookup fields within a [TableGenFieldScopeNode] within [myContext].
 * Will perform the lookup not only in [myRoot], but also any of its base classes.
 */
class FieldMap(private val myRoot: TableGenFieldScopeNode, private val myContext: TableGenCompilationContext) {

    /**
     * Returns the field named [fieldName] within [myRoot] or null if no such field exists yet at the location of
     * [beforeElement]. If there are multiple [TableGenFieldBodyItem] that define [fieldName], then the defining one is
     * returned. [TableGenFieldScopeNode.allFieldsByName] looks up the fields regardless of any location.
     */
    @RequiresReadLock
    operator fun get(fieldName: String, beforeElement: PsiElement): TableGenFieldBodyItem? =
        disallowTreeLoading {
            // Only consider base classes referenced before 'beforeElement'.
            myRoot.baseClassRefs.takeWhile {
                it.isBefore(beforeElement) != false
            }.mapNotNull {
                it.referencedDefinitionBlocking(myContext)
            }.firstNotNullOfOrNull {
                FieldMap(it, myContext)[fieldName, beforeElement]
            }?.let { return@disallowTreeLoading it }

            // Only consider fields defined before 'beforeElement'.
            myRoot.directFields[fieldName]?.takeIf { it.isBefore(beforeElement) != false }
        }

    /**
     * Returns the field named [fieldName] within [myRoot] or null if no such field exists yet at the location of
     * [fieldName].
     */
    operator fun get(fieldName: PsiElement) = get(fieldName.text, fieldName)
}

/**
 * Interface used by any [TableGenIdentifierScopeNode] which may also contain fields.
 *
 * Everything reaching into base classes depends on which class a base class reference resolves to and therefore takes
 * the [TableGenCompilationContext] it is asked in; see there for why.
 */
interface TableGenFieldScopeNode : TableGenIdentifierScopeNode {
    /**
     * Returns a map of all fields that are defined directly within this.
     * If there is one or more field with the same name, the map contains the first occurrence.
     */
    val directFields: Map<String, TableGenFieldBodyItem>

    /**
     * Returns a map of all field assignments in order of application (earliest to latest) that directly occur within
     * 'this'.
     */
    val directFieldAssignments: Map<String, List<TableGenFieldAssignmentNode>>

    /**
     * Returns the fields of this within [context] by name, including the inherited ones: those of the base classes in
     * order, followed by the fields of this itself. Every name maps to the field defining it, i.e. the first one found.
     * A class on a cycle of base classes has no fields. The map is cached per [context].
     */
    @RequiresReadLock
    suspend fun allFieldsByName(context: TableGenCompilationContext): Map<String, TableGenFieldBodyItem> =
        projectContextDependentSuspendingCachedValue(
            this, context, "fields by name", onCycle = { emptyMap() }
        ) { scope ->
            val result = linkedMapOf<String, TableGenFieldBodyItem>()
            // Following a reference to a base class is launched, so that the depth of the stack does not depend on
            // the depth of the class hierarchy.
            coroutineScope {
                scope.baseClassRefs.toList().map { ref ->
                    async { ref.referencedDefinition(context)?.allFieldsByName(context) }
                }.awaitAll()
            }.forEach { it?.forEach(result::putIfAbsent) }
            scope.directFields.forEach(result::putIfAbsent)
            result
        }.await()

    /**
     * Returns a map of all field assignments in order of application (earliest to latest), including from all
     * transitive base classes and the top-level 'let' statements enclosing this.
     * The first element in a list is therefore always a field body item if valid TableGen.
     */
    @RequiresReadLock
    fun allFieldAssignments(context: TableGenCompilationContext): Map<String, List<TableGenFieldAssignmentNode>> =
        getProjectContextDependentCache(this, context) {
            // Like in TableGen, the enclosing 'let' statements override what is inherited from the base classes, from
            // the outermost to the innermost one, and are overridden by the body in turn.
            val result = parentsOfType<TableGenLetStatement>(withSelf = false).toList().asReversed().flatMap {
                it.letItemList
            }.mapNotNull { item ->
                item.fieldName?.let { it to item }
            }.groupBy<_, _, TableGenFieldAssignmentNode>({ it.first }) { it.second }.toMutableMap()
            directFieldAssignments.forEach { (k, v) ->
                result.merge(k, v) { lets, direct ->
                    lets + direct
                }
            }
            baseClassRefs.toList().asReversed().mapNotNull { it.referencedDefinitionBlocking(context) }.map {
                it.allFieldAssignments(context)
            }.forEach {
                it.forEach { (k, v) ->
                    result.merge(k, v) { existing, parent ->
                        parent + existing
                    }
                }
            }
            result
        }

    /**
     * Returns a mapping of each each field to the assignments making up its value, in order of application.
     * Assuming valid TableGen, the first item is always an initial value setter, i.e. a field body item or a 'let'
     * that sets the entire field followed by 'let's that perform partial assignments
     * ('prepend', 'append' mode, single bits assignments, etc.).
     *
     * Invalid TableGen might have the first item be a partial assignment.
     */
    @RequiresReadLock
    fun effectiveFieldAssignments(context: TableGenCompilationContext): Map<String, List<TableGenFieldAssignmentNode>> =
        EffectiveFieldAssignmentsView(allFieldAssignments(context))

    /**
     * Returns a sequence of all references to base classes that should be used for field lookup.
     */
    val baseClassRefs: Sequence<TableGenClassRef>

    /**
     * Returns a set of all base classes (direct and transitive) of this node excluding 'this'.
     * The set contains a null value if there is at least one base-class that could not be resolved.
     */
    @RequiresReadLock
    fun allBaseClasses(context: TableGenCompilationContext): Set<TableGenClassStatement?> =
        getProjectContextDependentCache(this, context) {
            baseClassRefs.map {
                it.referencedDefinitionBlocking(context)
            }.flatMap {
                sequenceOf(it) + it?.allBaseClasses(context)?.asSequence().orEmpty()
            }.toSet()
        }

    /**
     * Returns whether this record is [target] or transitively derives from it. Returns null if the class hierarchy
     * could not be fully resolved without finding [target]; in that case a derivation through the unresolved class
     * cannot be ruled out.
     *
     * Note that a forward declaration and its definition denote the same class while being distinct statements, see
     * [TableGenClassStatement.isSameClassAs].
     */
    @RequiresReadLock
    fun derivesFrom(target: TableGenFieldScopeNode, context: TableGenCompilationContext): Boolean? {
        // Trivial self case.
        if (target === this) return true

        // Cannot derive from a non-class.
        if (target !is TableGenClassStatement) return false

        // A class may be declared before it is defined, in which case [target], this record and any of its base
        // classes may each be whichever statement of their class was visible from where they got referenced.
        if (this is TableGenClassStatement && isSameClassAs(target)) return true
        val baseClasses = allBaseClasses(context)
        if (baseClasses.any { it != null && it.isSameClassAs(target) }) return true

        // If null is contained in the set then not all base classes are known.
        // Depending on the caller we should handle this explicitly.
        // Return a null sentinel in this case.
        if (baseClasses.contains(null)) return null
        return false
    }

    /**
     * Returns what every template argument of the classes directly derived from is bound to within [context]: the
     * argument given in the class reference or else the default value of the template argument. Like in TableGen, both
     * are values of the record deriving from this: a default referring to another template argument of its class sees
     * what the same class reference binds it to.
     */
    @RequiresReadLock
    suspend fun directArgToTemplateArgMapping(
        context: TableGenCompilationContext,
    ): Map<TableGenTemplateArgDecl, TableGenValueNode> = projectContextDependentSuspendingCachedValue(
        this, context, "direct template argument mapping", onCycle = { emptyMap() }
    ) {
        baseClassRefs.toList().flatMap { ref ->
            val defaults = ref.referencedDefinition(context)?.templateArgDeclList.orEmpty().mapNotNull { decl ->
                decl.valueNode?.let { decl to it }
            }
            val arguments = ref.argValueItemList.flatMap {
                val referencedTemplateArgDecl =
                    it.referencedDefinition(context) ?: return@flatMap emptyList()
                val valueNode = it.valueNode ?: return@flatMap emptyList()
                listOf(referencedTemplateArgDecl to valueNode)
            }
            // Arguments take precedence over defaults.
            defaults + arguments
        }.toMap()
    }.await()

    @RequiresReadLock
    suspend fun allArgToTemplateArgMapping(
        context: TableGenCompilationContext,
    ): Map<TableGenTemplateArgDecl, TableGenValueNode> = projectContextDependentSuspendingCachedValue(
        this, context, "template argument mapping", onCycle = { emptyMap() }
    ) {
        val result = directArgToTemplateArgMapping(context).toMutableMap()
        baseClassRefs.toList().mapNotNull {
            it.referencedDefinition(context)?.allArgToTemplateArgMapping(context)
        }.forEach {
            it.forEach { (decl, node) ->
                result[decl] = node
            }
        }
        result
    }.await()

    /**
     * Returns a map for field lookup within [context].
     */
    fun fields(context: TableGenCompilationContext): FieldMap = FieldMap(this, context)
}

/**
 * The view returned by [TableGenFieldScopeNode.effectiveFieldAssignments] over the assignments [all] returned by
 * [TableGenFieldScopeNode.allFieldAssignments].
 */
private class EffectiveFieldAssignmentsView(
    private val all: Map<String, List<TableGenFieldAssignmentNode>>,
) : AbstractMap<String, List<TableGenFieldAssignmentNode>>() {
    override val size: Int
        get() = all.size

    override val keys: Set<String>
        get() = all.keys

    override fun containsKey(key: String): Boolean = all.containsKey(key)

    override fun get(key: String): List<TableGenFieldAssignmentNode>? = all[key]?.let(::effectiveAssignments)

    override val entries: Set<Map.Entry<String, List<TableGenFieldAssignmentNode>>> =
        object : AbstractSet<Map.Entry<String, List<TableGenFieldAssignmentNode>>>() {
            override val size: Int
                get() = all.size

            override fun iterator(): Iterator<Map.Entry<String, List<TableGenFieldAssignmentNode>>> =
                all.entries.asSequence().map { (field, assignments) ->
                    SimpleImmutableEntry(field, effectiveAssignments(assignments))
                }.iterator()
        }
}

/**
 * Returns the effective part of [assignments], given in order of application, see
 * [TableGenFieldScopeNode.effectiveFieldAssignments].
 */
private fun effectiveAssignments(assignments: List<TableGenFieldAssignmentNode>): List<TableGenFieldAssignmentNode> =
    assignments.drop(assignments.indexOfLast { (it as? TableGenAbstractLetItem)?.letMode == null }.coerceAtLeast(0))
