package com.github.zero9178.mlirods.language.psi

import com.github.zero9178.mlirods.index.DEFINE_INDEX
import com.github.zero9178.mlirods.index.getElements
import com.github.zero9178.mlirods.language.generated.psi.TableGenIfdefIfndefDirective
import com.github.zero9178.mlirods.model.TableGenIncludedSearchScope
import com.github.zero9178.mlirods.model.getProjectContextDependentCache
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.IndexNotReadyException
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.PsiReferenceBase
import com.intellij.psi.ResolveResult
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.concurrency.annotations.RequiresReadLock

/**
 * Implements the lookup procedure resolving the macro name tested by a '#ifdef'/'#ifndef' directive to the
 * corresponding '#define' directive(s).
 */
class TableGenMacroReference(element: TableGenIfdefIfndefDirective) :
    PsiReferenceBase.Poly<TableGenIfdefIfndefDirective>(element) {

    override fun hashCode(): Int {
        return element.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return element === (other as? TableGenMacroReference)?.element
    }

    @RequiresReadLock
    override fun multiResolve(incompleteCode: Boolean): Array<out ResolveResult> =
        getProjectContextDependentCache(element) { element ->
            val name = element.macroName ?: return@getProjectContextDependentCache emptyArray()

            val project = element.project
            if (DumbService.isDumb(project)) throw IndexNotReadyException.create()

            // A macro may be defined by this file or by any of the files it includes.
            DEFINE_INDEX.getElements(
                name,
                project,
                GlobalSearchScope.union(
                    arrayOf(
                        TableGenIncludedSearchScope(element, project),
                        GlobalSearchScope.fileScope(element.containingFile)
                    )
                )
            ).map { res -> PsiElementResolveResult(res) }.toTypedArray()
        }
}
