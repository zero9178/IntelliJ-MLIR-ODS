package com.github.zero9178.mlirods.language.psi

import com.github.zero9178.mlirods.language.generated.psi.TableGenClassRef
import com.github.zero9178.mlirods.language.generated.psi.TableGenFieldBodyItem
import com.github.zero9178.mlirods.language.generated.psi.TableGenTemplateArgDecl
import com.github.zero9178.mlirods.language.generated.psi.TableGenValueNode
import com.github.zero9178.mlirods.language.stubs.disallowTreeLoading
import com.intellij.psi.PsiElement
import com.intellij.psi.util.CachedValuesManager

/**
 * Map used to lookup fields within a [TableGenFieldScopeNode].
 * Will perform the lookup not only in [myRoot], but also any of its base classes.
 */
class FieldMap(private val myRoot: TableGenFieldScopeNode) {

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
                it.referencedClass
            }.firstNotNullOfOrNull {
                FieldMap(it)[fieldName, beforeElement]
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
    val directFieldAssignments: Map<String, List<TableGenFieldIdentifierNode>>

    /**
     * Returns a sequence of all fields of this class, including inherited fields.
     * The field body items returned by this sequence are guaranteed to be the defining field body items.
     */
    val allFields: Sequence<TableGenFieldBodyItem>
        get() = sequence {
            val seen = mutableSetOf<String?>()
            baseClassRefs.mapNotNull {
                it.referencedClass
            }.flatMap {
                it.allFields
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
    val allFieldAssignments: Map<String, List<TableGenFieldIdentifierNode>>
        get() = CachedValuesManager.getProjectPsiDependentCache(this) {
            val result = directFieldAssignments.toMutableMap()
            baseClassRefs.toList().asReversed().mapNotNull { it.referencedClass }.map {
                it.allFieldAssignments
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

    val directArgToTemplateArgMapping: Map<TableGenTemplateArgDecl, TableGenValueNode>
        get() = CachedValuesManager.getProjectPsiDependentCache(this) {
            baseClassRefs.flatMap { ref ->
                ref.argValueItemList.flatMap {
                    val referencedTemplateArgDecl = it.referencedTemplateArgDecl ?: return@flatMap emptyList()
                    val valueNode = it.valueNode ?: return@flatMap emptyList()
                    listOf(referencedTemplateArgDecl to valueNode)
                }
            }.toMap()
        }

    val allArgToTemplateArgMapping: Map<TableGenTemplateArgDecl, TableGenValueNode>
        get() = CachedValuesManager.getProjectPsiDependentCache(this) {
            val result = directArgToTemplateArgMapping.toMutableMap()
            baseClassRefs.mapNotNull { it.referencedClass?.allArgToTemplateArgMapping }.forEach {
                it.forEach { (decl, node) ->
                    result[decl] = node
                }
            }
            result
        }

    /**
     * Returns a map for field lookup.
     */
    val fields: FieldMap
        get() = FieldMap(this)
}
