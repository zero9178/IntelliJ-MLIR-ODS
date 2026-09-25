package com.github.zero9178.mlirods.language.psi

import com.github.zero9178.mlirods.language.generated.psi.TableGenClassRef
import com.github.zero9178.mlirods.language.generated.psi.TableGenClassStatement
import com.github.zero9178.mlirods.language.generated.psi.TableGenFieldBodyItem
import com.github.zero9178.mlirods.language.generated.psi.TableGenTemplateArgDecl
import com.github.zero9178.mlirods.language.generated.psi.TableGenValueNode
import com.github.zero9178.mlirods.language.stubs.disallowTreeLoading
import com.github.zero9178.mlirods.model.TableGenCompilationContext
import com.github.zero9178.mlirods.model.getProjectContextDependentCache
import com.github.zero9178.mlirods.model.projectContextDependentSuspendingCachedValue
import com.intellij.psi.PsiElement
import com.intellij.util.concurrency.annotations.RequiresReadLock

/**
 * Map used to lookup fields within a [TableGenFieldScopeNode] within [myContext].
 * Will perform the lookup not only in [myRoot], but also any of its base classes.
 */
class FieldMap(private val myRoot: TableGenFieldScopeNode, private val myContext: TableGenCompilationContext) {

    /**
     * Returns the field named [fieldName] within [myRoot] or null if no such field exists.
     * If there are multiple [TableGenFieldBodyItem] that define [fieldName], then the defining one is returned.
     * If [beforeElement] is not null, null is returned if the field has not yet been defined at the location of
     * [beforeElement].
     */
    operator fun get(fieldName: String, beforeElement: PsiElement? = null): TableGenFieldBodyItem? =
        disallowTreeLoading {
            // Only consider base classes referenced before 'beforeElement'.
            myRoot.baseClassRefs.takeWhile {
                beforeElement?.let { other -> it.isBefore(other) } ?: true
            }.mapNotNull {
                it.referencedDefinitionBlocking(myContext)
            }.firstNotNullOfOrNull {
                FieldMap(it, myContext)[fieldName, beforeElement]
            }?.let { return@disallowTreeLoading it }

            // Only consider fields defined before 'beforeElement'.
            val field = myRoot.directFields[fieldName] ?: return@disallowTreeLoading null
            when {
                beforeElement == null -> field
                field.isBefore(beforeElement) != false -> field
                else -> null
            }
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
     * Returns a sequence of all fields of this class, including inherited fields.
     * The field body items returned by this sequence are guaranteed to be the defining field body items.
     */
    @RequiresReadLock
    fun allFields(context: TableGenCompilationContext): Sequence<TableGenFieldBodyItem> = sequence {
        val seen = mutableSetOf<String?>()
        baseClassRefs.mapNotNull {
            it.referencedDefinitionBlocking(context)
        }.flatMap {
            it.allFields(context)
        }.forEach {
            if (seen.add(it.fieldName)) yield(it)
        }

        yieldAll(directFields.values.asSequence().filter {
            !seen.contains(it.fieldName)
        })
    }

    /**
     * Returns a map of all field assignments in order of application (earliest to latest), including from all
     * transitive base classes.
     * The first element in a list is therefore always a field body item if valid TableGen.
     */
    @RequiresReadLock
    fun allFieldAssignments(context: TableGenCompilationContext): Map<String, List<TableGenFieldAssignmentNode>> =
        getProjectContextDependentCache(this, context) {
            val result = directFieldAssignments.toMutableMap()
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
