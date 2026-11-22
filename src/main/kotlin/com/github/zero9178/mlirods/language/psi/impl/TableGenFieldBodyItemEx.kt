package com.github.zero9178.mlirods.language.psi.impl

import com.github.zero9178.mlirods.language.generated.psi.TableGenFieldBodyItem
import com.github.zero9178.mlirods.language.psi.TableGenFieldAssignmentNode
import com.github.zero9178.mlirods.language.psi.TableGenFieldReferencingElement
import com.github.zero9178.mlirods.language.psi.TableGenFieldRedefinitionReference
import com.github.zero9178.mlirods.model.TableGenCompilationContext
import com.intellij.util.concurrency.annotations.RequiresReadLock

interface TableGenFieldBodyItemEx : TableGenFieldAssignmentNode, TableGenFieldReferencingElement {

    /**
     * Returns the field this redefines within [context], see [TableGenFieldRedefinitionReference.findRedefinedField].
     */
    @RequiresReadLock
    override suspend fun referencedDefinition(context: TableGenCompilationContext): TableGenFieldBodyItem? =
        TableGenFieldRedefinitionReference.findRedefinedField(this as TableGenFieldBodyItem, context)
}
