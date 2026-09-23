package com.github.zero9178.mlirods.language.psi

import com.github.zero9178.mlirods.index.MULTICLASS_INDEX
import com.github.zero9178.mlirods.index.getVisibleElements
import com.github.zero9178.mlirods.language.generated.psi.TableGenDefmStatement
import com.github.zero9178.mlirods.language.generated.psi.TableGenMultiClassRef
import com.github.zero9178.mlirods.language.generated.psi.TableGenMulticlassStatement
import com.github.zero9178.mlirods.language.stubs.disallowTreeLoading
import com.github.zero9178.mlirods.model.TableGenCompilationContext
import com.github.zero9178.mlirods.model.getProjectContextDependentCache
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.IndexNotReadyException
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.PsiReferenceBase
import com.intellij.psi.ResolveResult
import com.intellij.util.concurrency.annotations.RequiresReadLock

/**
 * Returns the multiclass called [name] that is visible from [element] within [context], i.e. precedes it once every
 * 'include' directive is pasted in. [element] itself is never the result. Results are cached per [element], [name] and
 * [context].
 *
 * A multiclass cannot be defined more than once: TableGen rejects every definition following the first one. The last
 * definition, i.e. the one closest to [element], is returned nonetheless, as it is the one more likely to be meant.
 *
 * Does not require the syntax tree of any file.
 */
@RequiresReadLock
fun findVisibleMulticlass(
    name: String, element: PsiElement, context: TableGenCompilationContext
): TableGenMulticlassStatement? = getProjectContextDependentCache(element, name to context) {
    if (DumbService.isDumb(it.project)) throw IndexNotReadyException.create()

    val seenFrom = context.at(it)
    MULTICLASS_INDEX.getVisibleElements(name, seenFrom).maxWithOrNull(seenFrom.textOrder)
}

/**
 * Returns true if [ref] refers to a class rather than a multiclass within [context].
 *
 * A 'defm' may derive from classes in addition to multiclasses as long as the classes come last. TableGen tells them
 * apart by checking every name following the first one for whether a class of that name is visible, making the first
 * name that is and every name following it refer to classes. The first name of a 'defm' always refers to a multiclass,
 * as does every name of a 'multiclass' statement.
 */
@RequiresReadLock
fun refersToClass(ref: TableGenMultiClassRef, context: TableGenCompilationContext): Boolean {
    val defm = ref.parent as? TableGenDefmStatement ?: return false

    val names = defm.multiClassRefList
    val index = names.indexOf(ref)
    assert(index >= 0) { "name should have been one of the names of its 'defm'" }
    // The names up to and including 'ref', except for the first one.
    return names.subList(1, index + 1).any {
        TableGenClassReference.findVisibleClasses(it.className, it, context).isNotEmpty()
    }
}

/**
 * Implements the lookup procedure for the names in the parent list of 'defm' and 'multiclass' statements, which refer
 * to multiclasses or, in a 'defm', to classes, see [refersToClass].
 */
class TableGenMultiClassReference(element: TableGenMultiClassRef) :
    PsiReferenceBase.Poly<TableGenMultiClassRef>(element) {

    override fun hashCode(): Int {
        return element.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return element === (other as? TableGenMultiClassReference)?.element
    }

    companion object {
        /**
         * Returns what [element] refers to within [context]: the classes of its name if it [refersToClass], the
         * multiclass of its name otherwise. This is what resolving the reference and every lookup on the PSI itself are
         * built on.
         *
         * Does not require the syntax tree of any file.
         */
        @RequiresReadLock
        fun findTargets(element: TableGenMultiClassRef, context: TableGenCompilationContext): List<PsiElement> =
            disallowTreeLoading {
                if (refersToClass(element, context))
                    TableGenClassReference.findVisibleClasses(element.className, element, context)
                else listOfNotNull(findVisibleMulticlass(element.className, element, context))
            }
    }

    /**
     * Resolves the reference in the context the platform sees the file in, i.e. the one it derives from the include
     * graph. Anything that already has a context in hand should use [findTargets] instead.
     */
    @RequiresReadLock
    override fun multiResolve(incompleteCode: Boolean): Array<out ResolveResult> {
        val context = TableGenCompilationContext.activeFor(element)
        return PsiElementResolveResult.createResults(findTargets(element, context))
    }
}
