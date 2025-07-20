package com.github.zero9178.mlirods.language.psi

import com.github.zero9178.mlirods.index.CLASS_INDEX
import com.github.zero9178.mlirods.index.getElements
import com.github.zero9178.mlirods.language.TableGenFile
import com.github.zero9178.mlirods.language.completion.createLookupElement
import com.github.zero9178.mlirods.language.generated.psi.TableGenAbstractClassRef
import com.github.zero9178.mlirods.language.generated.psi.TableGenClassStatement
import com.github.zero9178.mlirods.language.generated.psi.TableGenScopeItem
import com.github.zero9178.mlirods.language.stubs.disallowTreeLoading
import com.github.zero9178.mlirods.model.TableGenIncludedSearchScope
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.IndexNotReadyException
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.PsiReferenceBase
import com.intellij.psi.ResolveResult
import com.intellij.psi.util.CachedValuesManager
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
         * Returns all completion variants at the given [positionToken].
         * [positionToken] should be an identifier token.
         */
        fun getVariants(positionToken: PsiElement) = localSearchOrder(positionToken).map {
            createLookupElement(it, positionToken)
        }
    }

    override fun getVariants() = getVariants(element.classIdentifier).toList().toTypedArray()

    @RequiresReadLock
    override fun multiResolve(incompleteCode: Boolean): Array<out ResolveResult> =
        CachedValuesManager.getProjectPsiDependentCache(element) {
            disallowTreeLoading {
                val name = element.className
                val klass = localSearchOrder(element).find {
                    it.name == name
                }

                // Lookup in the same file succeeded.
                if (klass != null) return@disallowTreeLoading arrayOf(PsiElementResolveResult(klass))

                val project = element.project
                if (DumbService.isDumb(project)) throw IndexNotReadyException.create()

                // Otherwise, use the index to search in TableGen files included by this file.
                CLASS_INDEX.getElements(
                    name,
                    project,
                    TableGenIncludedSearchScope(element, project)
                ).map { PsiElementResolveResult(it) }.toTypedArray()
            }
        }
}
