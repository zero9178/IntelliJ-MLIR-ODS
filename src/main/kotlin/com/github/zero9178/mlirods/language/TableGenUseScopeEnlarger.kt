package com.github.zero9178.mlirods.language

import com.github.zero9178.mlirods.language.generated.psi.TableGenClassStatement
import com.github.zero9178.mlirods.language.generated.psi.TableGenDefineDirective
import com.github.zero9178.mlirods.language.psi.TableGenIdentifierElement
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.SearchScope
import com.intellij.psi.search.UseScopeEnlarger

/**
 * Widens the use scope of TableGen definitions to all TableGen files.
 *
 * A definition is referenced by every file transitively including it, which is unrelated to the module a file belongs
 * to. The default use scope is derived from that module, making it too narrow: the includer may live in a different
 * module than the definition, e.g. the synthetic module contributed by
 * [com.github.zero9178.mlirods.model.TableGenWorkspaceModelService] for TableGen files outside the project's content.
 * Searches such as the identifier highlighting under the caret intersect with the use scope and would otherwise find no
 * references, even though resolution succeeds.
 *
 * Restricting to TableGen files keeps the scope as small as still sound, as only they can reference a definition.
 */
internal class TableGenUseScopeEnlarger : UseScopeEnlarger() {
    override fun getAdditionalUseScope(element: PsiElement): SearchScope? = when (element) {
        is TableGenClassStatement, is TableGenIdentifierElement, is TableGenDefineDirective ->
            GlobalSearchScope.getScopeRestrictedByFileTypes(
                GlobalSearchScope.allScope(element.project), TableGenFileType.INSTANCE
            )

        else -> null
    }
}
