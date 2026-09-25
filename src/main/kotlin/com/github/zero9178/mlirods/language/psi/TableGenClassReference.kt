package com.github.zero9178.mlirods.language.psi

import com.github.zero9178.mlirods.index.CLASS_INDEX
import com.github.zero9178.mlirods.index.getVisibleElements
import com.github.zero9178.mlirods.language.completion.createLookupElement
import com.github.zero9178.mlirods.language.generated.psi.TableGenAbstractClassRef
import com.github.zero9178.mlirods.language.generated.psi.TableGenClassStatement
import com.github.zero9178.mlirods.language.generated.psi.TableGenScopeItem
import com.github.zero9178.mlirods.language.psi.impl.TableGenAbstractClassRefEx
import com.github.zero9178.mlirods.language.stubs.disallowTreeLoading
import com.github.zero9178.mlirods.model.TableGenCompilationContext
import com.github.zero9178.mlirods.model.getProjectContextDependentCache
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.IndexNotReadyException
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.PsiReferenceBase
import com.intellij.psi.ResolveResult
import com.intellij.psi.util.parentsOfType
import com.intellij.util.concurrency.annotations.RequiresReadLock

/**
 * Implements the lookup procedure for classes.
 */
class TableGenClassReference(element: TableGenAbstractClassRef) :
    PsiReferenceBase.Poly<TableGenAbstractClassRef>(element) {

    override fun hashCode(): Int {
        return element.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return element === (other as? TableGenClassReference)?.element
    }

    companion object {
        private fun localSearchOrder(element: PsiElement) = sequence {
            var last: TableGenScopeItem? = null
            for (iter in element.parentsOfType<TableGenScopeItem>(withSelf = true)) {
                if (iter is TableGenClassStatement)
                    yield(iter)

                last = iter
            }
            if (last == null) return@sequence

            val file = last.containingFile as? TableGenFile ?: return@sequence
            val spine = file.stubbedSpine
            yieldAll((0 until spine.stubCount).asSequence().mapNotNull {
                spine.getStubPsi(it)
            }.takeWhile { it != last }.filterIsInstance<TableGenClassStatement>())
        }

        /**
         * Returns all statements of the class called [name] that are visible from [element] within [context], i.e.
         * precede it once every 'include' directive is pasted in, be they declarations or definitions. The statements
         * are in the order they have within that text, making the last one the closest to [element]. [element] itself
         * is never part of the result. Results are cached per [element], [name] and [context].
         *
         * Does not require the syntax tree of any file.
         */
        @RequiresReadLock
        fun findVisibleClasses(
            name: String, element: PsiElement, context: TableGenCompilationContext
        ): List<TableGenClassStatement> = getProjectContextDependentCache(element, name to context) { element ->
            if (DumbService.isDumb(element.project)) throw IndexNotReadyException.create()

            val seenFrom = context.at(element)
            CLASS_INDEX.getVisibleElements(name, seenFrom).sortedWith(seenFrom.textOrder)
        }

        /**
         * Returns whether [element] names the class whose parent list it is part of. The class is visible from there,
         * as TableGen defines a class before parsing its parent list, but deriving from it makes TableGen recurse
         * until it crashes.
         */
        fun isDerivingFromOwnClass(element: TableGenAbstractClassRefEx): Boolean =
            (element.parent as? TableGenClassStatement)?.name == element.className

        /**
         * Returns all statements of the class [element] refers to that are visible from it within [context], see the
         * overload above. This is what resolving the reference and every lookup on the PSI itself are built on.
         *
         * A class deriving from itself, see [isDerivingFromOwnClass], derives from nothing instead, as every lookup
         * through base classes would otherwise follow the cycle forever.
         */
        @RequiresReadLock
        fun findVisibleClasses(element: TableGenAbstractClassRefEx, context: TableGenCompilationContext) =
            disallowTreeLoading {
                if (isDerivingFromOwnClass(element)) emptyList()
                else findVisibleClasses(element.className, element, context)
            }

        /**
         * Returns all completion variants at the given [positionToken].
         * [positionToken] should be an identifier token.
         *
         * Within the parent list of a class, the class itself is never a variant, see [isOwnClassVariant].
         */
        fun getVariants(positionToken: PsiElement) = localSearchOrder(positionToken).filterNot {
            isOwnClassVariant(it, positionToken)
        }.map {
            createLookupElement(it, positionToken)
        }

        /**
         * Returns whether completing [variant] at [positionToken] would make a class derive from itself, see
         * [isDerivingFromOwnClass]. This includes the forward declarations of the class, wherever they are.
         */
        fun isOwnClassVariant(variant: TableGenClassStatement, positionToken: PsiElement): Boolean {
            val derivingClass = (positionToken.parent as? TableGenAbstractClassRef)?.parent as? TableGenClassStatement
            return derivingClass != null && variant.name == derivingClass.name
        }
    }

    override fun getVariants() = getVariants(element.classIdentifier).toList().toTypedArray()

    /**
     * Resolves the reference in the context the platform sees the file in, i.e. the one it derives from the include
     * graph. Anything that already has a context in hand should use [findVisibleClasses] instead.
     */
    @RequiresReadLock
    override fun multiResolve(incompleteCode: Boolean): Array<out ResolveResult> {
        val context = TableGenCompilationContext.activeFor(element)
        return PsiElementResolveResult.createResults(findVisibleClasses(element, context))
    }
}
