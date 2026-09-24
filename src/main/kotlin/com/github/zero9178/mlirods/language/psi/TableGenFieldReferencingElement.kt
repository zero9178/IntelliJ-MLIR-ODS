package com.github.zero9178.mlirods.language.psi

import com.github.zero9178.mlirods.language.generated.psi.TableGenFieldBodyItem
import com.github.zero9178.mlirods.model.TableGenCompilationContext

/**
 * An element referring to a field, i.e. a field access or a 'let' body item assigning a new value to a field.
 */
interface TableGenFieldReferencingElement : TableGenReferencingElement {
    /**
     * Returns the field being referenced within [context] or null if there is none.
     */
    override suspend fun referencedDefinition(context: TableGenCompilationContext): TableGenFieldBodyItem? =
        referencedDefinitionBlocking(context)

    override fun referencedDefinitionBlocking(context: TableGenCompilationContext): TableGenFieldBodyItem?
}
