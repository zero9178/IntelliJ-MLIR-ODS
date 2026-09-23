package com.github.zero9178.mlirods.language.psi

import com.github.zero9178.mlirods.index.DEFINE_INDEX
import com.github.zero9178.mlirods.index.getElements
import com.github.zero9178.mlirods.language.generated.psi.TableGenDefineDirective
import com.github.zero9178.mlirods.language.generated.psi.TableGenIfdefIfndefDirective
import com.github.zero9178.mlirods.model.TableGenCompilationContext
import com.github.zero9178.mlirods.model.getProjectContextDependentCache
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.IndexNotReadyException
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.PsiReferenceBase
import com.intellij.psi.ResolveResult
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

    companion object {
        /**
         * Returns all '#define' directives of the macro tested by [element] that are visible from it within
         * [context]. This is what resolving the reference and every lookup on the PSI itself are built on; results
         * are cached per [element] and [context].
         */
        @RequiresReadLock
        fun findVisibleDefines(
            element: TableGenIfdefIfndefDirective, context: TableGenCompilationContext
        ): List<TableGenDefineDirective> = getProjectContextDependentCache(element, context) { element ->
            val name = element.macroName ?: return@getProjectContextDependentCache emptyList()

            val project = element.project
            if (DumbService.isDumb(project)) throw IndexNotReadyException.create()

            // A macro may be defined by this file or by any of the files pasted in before the directive.
            // TODO: Within those files this does not consider the position of the '#define' yet.
            DEFINE_INDEX.getElements(name, project, context.at(element).scope).toList()
        }
    }

    /**
     * Resolves the reference in the context the platform sees the file in, i.e. the one it derives from the include
     * graph. Anything that already has a context in hand should use [findVisibleDefines] instead.
     */
    @RequiresReadLock
    override fun multiResolve(incompleteCode: Boolean): Array<out ResolveResult> {
        val context = TableGenCompilationContext.activeFor(element)
        return PsiElementResolveResult.createResults(findVisibleDefines(element, context))
    }
}
