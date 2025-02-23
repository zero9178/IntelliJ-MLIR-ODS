package com.github.zero9178.mlirods.language.psi

import com.github.zero9178.mlirods.index.DEF_INDEX
import com.github.zero9178.mlirods.language.generated.psi.TableGenIdentifierValue
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.IndexNotReadyException
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.PsiReferenceBase
import com.intellij.psi.ResolveResult
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.stubs.StubIndex
import com.intellij.psi.util.parents
import com.intellij.util.concurrency.annotations.RequiresReadLock

/**
 * Implements the lookup procedure for 'def's.
 */
class TableGenDefReference(element: TableGenIdentifierValue) : PsiReferenceBase.Poly<TableGenIdentifierValue>(element) {

    @RequiresReadLock
    override fun multiResolve(incompleteCode: Boolean): Array<out ResolveResult> {
        val name = element.identifier.text
        // TODO: This does not take into account the position of [element] in the AST.
        val def = element.parents(false).filterIsInstance<TableGenIdentifierScopeNode>().firstNotNullOfOrNull {
            it.defs.find { def ->
                def.name == name
            }
        }

        // Lookup in the same file succeeded.
        if (def != null) return arrayOf(PsiElementResolveResult(def))

        val project = element.project
        if (DumbService.isDumb(project)) throw IndexNotReadyException.create()

        // Otherwise, use the index to search in TableGen files included by this file.
        return StubIndex.getElements(
            DEF_INDEX,
            name,
            project,
            // TODO: We should only be searching in files included by the contained file.
            GlobalSearchScope.allScope(project),
            TableGenDefNameIdentifierOwner::class.java
        ).map { PsiElementResolveResult(it) }.toTypedArray()
    }
}
