package com.github.zero9178.mlirods.language.psi.impl

import com.github.zero9178.mlirods.language.generated.psi.TableGenFieldAccessValueNode
import com.github.zero9178.mlirods.language.generated.psi.TableGenFieldBodyItem
import com.github.zero9178.mlirods.language.psi.TableGenFieldAccessReference
import com.github.zero9178.mlirods.language.psi.TableGenFieldReferencingElement
import com.github.zero9178.mlirods.model.TableGenCompilationContext
import com.intellij.psi.PsiElement
import com.intellij.util.concurrency.annotations.RequiresReadLock

interface TableGenFieldAccessValueNodeEx : PsiElement, TableGenFieldReferencingElement {
    /**
     * Returns the field accessed within [context], see [TableGenFieldAccessReference.findField].
     */
    @RequiresReadLock
    override suspend fun referencedDefinition(context: TableGenCompilationContext): TableGenFieldBodyItem? =
        TableGenFieldAccessReference.findField(this as TableGenFieldAccessValueNode, context)
}
