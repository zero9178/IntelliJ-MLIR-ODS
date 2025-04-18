package com.github.zero9178.mlirods.language.psi

import com.github.zero9178.mlirods.index.CLASS_INDEX
import com.github.zero9178.mlirods.index.getElements
import com.github.zero9178.mlirods.language.generated.psi.*
import com.github.zero9178.mlirods.language.stubs.disallowTreeLoading
import com.github.zero9178.mlirods.model.TableGenIncludedSearchScope
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.IndexNotReadyException
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.PsiReferenceBase
import com.intellij.psi.ResolveResult
import com.intellij.psi.stubs.StubIndex
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

    @RequiresReadLock
    override fun multiResolve(incompleteCode: Boolean): Array<out ResolveResult> =
        CachedValuesManager.getProjectPsiDependentCache(element) {
            disallowTreeLoading {
                val name = element.className
                val topLevelElement =
                    element.parentsOfType<TableGenScopeItem>(withSelf = false).lastOrNull()
                        ?: return@disallowTreeLoading emptyArray()
                val klass = topLevelElement.classStatementsBefore().find {
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
