package com.github.zero9178.mlirods.language.psi.impl

import com.github.zero9178.mlirods.language.generated.psi.TableGenDefineDirective
import com.github.zero9178.mlirods.language.psi.TableGenReferencingElement
import com.github.zero9178.mlirods.model.TableGenCompilationContext

/**
 * Interface used to add extra methods to [com.github.zero9178.mlirods.language.generated.psi.TableGenIfdefIfndefDirective].
 */
interface TableGenIfdefIfndefDirectiveEx : TableGenMacroDirectiveEx, TableGenReferencingElement {
    /**
     * Returns the '#define' directive of the macro tested by this directive within [context] or null if there is none.
     * Of several '#define' directives of the macro, the first one found is returned.
     */
    override suspend fun referencedDefinition(context: TableGenCompilationContext): TableGenDefineDirective? =
        referencedDefinitionBlocking(context)

    override fun referencedDefinitionBlocking(context: TableGenCompilationContext): TableGenDefineDirective?
}
