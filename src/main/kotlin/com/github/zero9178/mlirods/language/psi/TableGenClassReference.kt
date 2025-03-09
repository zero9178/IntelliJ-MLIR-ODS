package com.github.zero9178.mlirods.language.psi

import com.github.zero9178.mlirods.index.CLASS_INDEX
import com.github.zero9178.mlirods.index.getElements
import com.github.zero9178.mlirods.language.generated.psi.*
import com.github.zero9178.mlirods.model.TableGenIncludedSearchScope
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.IndexNotReadyException
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.PsiReferenceBase
import com.intellij.psi.ResolveResult
import com.intellij.psi.stubs.StubIndex
import com.intellij.psi.util.parentsOfType
import com.intellij.util.concurrency.annotations.RequiresReadLock

/**
 * Implements the lookup procedure for classes.
 */
class TableGenClassReference(element: TableGenAbstractClassRef) :
    PsiReferenceBase.Poly<TableGenAbstractClassRef>(element) {

    @RequiresReadLock
    override fun multiResolve(incompleteCode: Boolean): Array<out ResolveResult> {
        val name = element.className.text
        val topLevelElement =
            element.parentsOfType<TableGenScopeItem>(withSelf = false).lastOrNull() ?: return emptyArray()
        val klass = topLevelElement.itemsBefore(withSelf = false).filterIsInstance<TableGenClassStatement>().find {
            it.name == name
        }

        // Lookup in the same file succeeded.
        if (klass != null) return arrayOf(PsiElementResolveResult(klass))

        val project = element.project
        if (DumbService.isDumb(project)) throw IndexNotReadyException.create()

        // Otherwise, use the index to search in TableGen files included by this file.
        return CLASS_INDEX.getElements(
            name,
            project,
            TableGenIncludedSearchScope(element, project)
        ).map { PsiElementResolveResult(it) }.toTypedArray()
    }
}
