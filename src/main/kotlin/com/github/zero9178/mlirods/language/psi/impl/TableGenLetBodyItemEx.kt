package com.github.zero9178.mlirods.language.psi.impl

import com.github.zero9178.mlirods.language.generated.psi.TableGenFieldBodyItem
import com.github.zero9178.mlirods.language.generated.psi.TableGenLetBodyItem
import com.github.zero9178.mlirods.language.psi.TableGenFieldReferencingElement
import com.github.zero9178.mlirods.language.psi.TableGenLetReference
import com.github.zero9178.mlirods.model.TableGenCompilationContext
import com.intellij.psi.PsiElement
import com.intellij.util.concurrency.annotations.RequiresReadLock

interface TableGenLetBodyItemEx : PsiElement, TableGenFieldReferencingElement {
    /**
     * Returns the field assigned to within [context], see [TableGenLetReference.findField].
     */
    @RequiresReadLock
    override suspend fun referencedDefinition(context: TableGenCompilationContext): TableGenFieldBodyItem? =
        TableGenLetReference.findField(this as TableGenLetBodyItem, context)
}
