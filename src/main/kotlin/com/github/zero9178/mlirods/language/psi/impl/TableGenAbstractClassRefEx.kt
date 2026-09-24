package com.github.zero9178.mlirods.language.psi.impl

import com.github.zero9178.mlirods.language.generated.psi.TableGenClassStatement
import com.github.zero9178.mlirods.language.psi.TableGenClassReference
import com.github.zero9178.mlirods.model.TableGenCompilationContext
import com.intellij.psi.PsiElement

interface TableGenAbstractClassRefEx : PsiElement, TableGenAbstractRefEx {
    /**
     * Returns the class being referenced within [context] or null if resolution failed. Of the statements of the
     * class, its definition is preferred to its declarations.
     */
    override suspend fun referencedDefinition(context: TableGenCompilationContext): TableGenClassStatement? =
        referencedDefinitionBlocking(context)

    override fun referencedDefinitionBlocking(context: TableGenCompilationContext): TableGenClassStatement? =
        preferDefinition(TableGenClassReference.findVisibleClasses(this, context))

    /**
     * Returns the name of the class being referenced.
     */
    val className: String
}
