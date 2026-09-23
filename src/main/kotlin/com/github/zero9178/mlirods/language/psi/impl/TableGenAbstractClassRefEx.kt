package com.github.zero9178.mlirods.language.psi.impl

import com.github.zero9178.mlirods.language.generated.psi.TableGenClassStatement
import com.github.zero9178.mlirods.language.psi.TableGenClassReference
import com.github.zero9178.mlirods.model.TableGenCompilationContext
import com.intellij.psi.PsiElement
import com.intellij.util.concurrency.annotations.RequiresReadLock

interface TableGenAbstractClassRefEx : PsiElement {
    /**
     * Returns the class being referenced within [context] or null if resolution failed.
     */
    @RequiresReadLock
    fun referencedClass(context: TableGenCompilationContext): TableGenClassStatement? =
        TableGenClassReference.findVisibleClasses(this, context).partition {
            it.isDeclaration
        }.let { (decls, defs) ->
            // Always prefer definitions to declarations, only returning a declaration if there is no definition.
            defs.lastOrNull() ?: decls.lastOrNull()
        }

    /**
     * Returns the name of the class being referenced.
     */
    val className: String
}
