package com.github.zero9178.mlirods.language.psi

import com.github.zero9178.mlirods.language.generated.psi.TableGenClassStatement
import com.github.zero9178.mlirods.language.generated.psi.TableGenDefmStatement
import com.github.zero9178.mlirods.language.generated.psi.TableGenFieldBodyItem
import com.github.zero9178.mlirods.language.generated.psi.TableGenIncludeDirective
import com.github.zero9178.mlirods.language.generated.psi.TableGenLetItem
import com.github.zero9178.mlirods.language.generated.psi.TableGenLetStatement
import com.github.zero9178.mlirods.language.generated.psi.TableGenMultiClassRef
import com.github.zero9178.mlirods.language.generated.psi.TableGenMulticlassStatement
import com.github.zero9178.mlirods.cache.SuspendingCachedValue
import com.github.zero9178.mlirods.model.TableGenCompilationContext
import com.github.zero9178.mlirods.model.projectContextDependentSuspendingCachedValue
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiReferenceBase
import com.intellij.psi.ResolveResult
import com.intellij.util.concurrency.annotations.RequiresReadLock
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

/**
 * Returns the fields named [name] of the records the multiclasses and classes [ref] instantiates within [context], one
 * collection per multiclass or class.
 */
private suspend fun instantiatedFieldsOf(
    ref: TableGenMultiClassRef, name: String, context: TableGenCompilationContext,
): List<Collection<TableGenFieldBodyItem>> =
    TableGenMultiClassReference.findTargets(ref, context).mapNotNull {
        when (it) {
            // Like in TableGen, a multiclass deriving from itself derives from nothing, as it has no records yet.
            is TableGenMulticlassStatement ->
                if (it !== ref.parent) assignableFieldsOf(it, name, throughDefm = true, context).await() else null
            // The classes of a 'defm' are base classes of all its records.
            is TableGenClassStatement -> it.allFieldsByName(context)[name]?.let(::listOf)
            else -> null
        }
    }

/**
 * Computes what [assignableFieldsOf] returns, see there.
 */
private suspend fun computeAssignableFields(
    container: TableGenStatementContainer, name: String, throughDefm: Boolean, context: TableGenCompilationContext,
): Collection<TableGenFieldBodyItem> {
    val result = linkedSetOf<TableGenFieldBodyItem>()
    coroutineScope {
        // What the targets refer to is independent of each other and therefore requested all at once, while being
        // added in source order once known. Following references is launched, so that the depth of the stack does not
        // depend on how long a chain of includes, instantiations or base classes is. Nested 'let's are part of the
        // syntax tree and requested on the stack.
        val steps = mutableListOf<suspend () -> Unit>()
        fun addInstantiations(refs: List<TableGenMultiClassRef>) = refs.forEach { ref ->
            val fields = async { instantiatedFieldsOf(ref, name, context) }
            steps += { fields.await().forEach(result::addAll) }
        }

        // The records of a multiclass' base multiclasses are only subject to a 'let' enclosing a 'defm', not one
        // enclosing the multiclass.
        if (throughDefm && container is TableGenMulticlassStatement) addInstantiations(container.multiClassRefList)

        // The statements of nested 'let's are summarized by the 'let' itself, which every 'let' reaching it shares.
        for (child in container.nestedStatements { it !is TableGenLetStatement }) when (child) {
            is TableGenRecord -> {
                val bases = child.baseClassRefs.map { ref ->
                    async { ref.referencedDefinition(context)?.allFieldsByName(context) }
                }.toList()
                steps += {
                    // The field defining the name is the one of the first base class having it. Reached through a
                    // 'defm', the 'let' applies to the fields of the body as well.
                    val inherited = bases.firstNotNullOfOrNull { it.await()?.get(name) }
                    if (inherited != null) result += inherited
                    else if (throughDefm) child.directFields[name]?.let(result::add)
                }
            }

            is TableGenDefmStatement -> addInstantiations(child.multiClassRefList)
            is TableGenIncludeDirective -> {
                val fields = async {
                    child.includedFile?.let {
                        PsiManager.getInstance(child.project).findFile(it) as? TableGenFile
                    }?.let { assignableFieldsOf(it, name, throughDefm, context).await() }
                }
                steps += { fields.await()?.let(result::addAll) }
            }

            is TableGenLetStatement -> steps += {
                result.addAll(assignableFieldsOf(child, name, throughDefm, context).await())
            }
        }
        steps.forEach { it() }
    }
    return result
}

/**
 * What [assignableFieldsOf] is cached by besides the container.
 */
private data class AssignableFieldsKey(
    val context: TableGenCompilationContext, val name: String, val throughDefm: Boolean,
)

/**
 * Returns the fields named [name] that a 'let' enclosing [container] assigns to within [context].
 * [throughDefm] denotes whether the records are reached through a 'defm', i.e. are records the 'defm' produces by
 * instantiating a multiclass. TableGen applies a 'let' enclosing a 'defm' once these records are complete, so it
 * applies to the fields of their bodies as well, and to the records of the multiclass' base multiclasses. Otherwise, a
 * 'let' only applies to inherited fields.
 */
private fun assignableFieldsOf(
    container: TableGenStatementContainer, name: String, throughDefm: Boolean, context: TableGenCompilationContext,
): SuspendingCachedValue<Collection<TableGenFieldBodyItem>> = projectContextDependentSuspendingCachedValue(
    container, AssignableFieldsKey(context, name, throughDefm), "fields assignable by 'let'",
    onCycle = { emptyList() },
) { computeAssignableFields(it, name, throughDefm, context) }

/**
 * Resolution used to find fields referenced by the items of top-level 'let' statements.
 */
class TableGenLetItemReference(element: TableGenLetItem) : PsiReferenceBase.Poly<TableGenLetItem>(element) {

    companion object {
        /**
         * Returns the fields [element] assigns to within [context]: the distinct fields of every record the 'let' it is
         * part of applies to, as the records may get the field from unrelated classes. The fields are cached per 'let',
         * name and [context], making this a lookup for every item of a 'let' assigning the same name.
         */
        @RequiresReadLock
        suspend fun findFields(
            element: TableGenLetItem, context: TableGenCompilationContext,
        ): Collection<TableGenFieldBodyItem> = fieldsOf(element, context)?.await().orEmpty()

        /**
         * [findFields] for platform entry points. Suspending code must use [findFields] instead.
         */
        @RequiresReadLock
        fun findFieldsBlocking(
            element: TableGenLetItem, context: TableGenCompilationContext,
        ): Collection<TableGenFieldBodyItem> = fieldsOf(element, context)?.getBlocking().orEmpty()

        /**
         * The cached value of [findFields], or null if [element] names no field.
         */
        private fun fieldsOf(
            element: TableGenLetItem, context: TableGenCompilationContext,
        ): SuspendingCachedValue<Collection<TableGenFieldBodyItem>>? {
            val name = element.fieldName ?: return null
            return assignableFieldsOf(element.parent as TableGenLetStatement, name, throughDefm = false, context)
        }
    }

    /**
     * Resolves the reference in the context the platform sees the file in, i.e. the one it derives from the include
     * graph. Anything that already has a context in hand should use [findFields] instead.
     */
    @RequiresReadLock
    override fun multiResolve(incompleteCode: Boolean): Array<out ResolveResult> {
        val context = TableGenCompilationContext.activeFor(element)
        return PsiElementResolveResult.createResults(findFieldsBlocking(element, context))
    }
}
