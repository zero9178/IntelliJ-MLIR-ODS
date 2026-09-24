package com.github.zero9178.mlirods.language.psi.impl

import com.github.zero9178.mlirods.language.generated.psi.TableGenAbstractClassStatement
import com.github.zero9178.mlirods.language.generated.psi.TableGenClassStatement
import com.github.zero9178.mlirods.language.generated.psi.TableGenMultiClassRef
import com.github.zero9178.mlirods.language.psi.TableGenMultiClassReference
import com.github.zero9178.mlirods.language.psi.TableGenReferencingElement
import com.github.zero9178.mlirods.model.TableGenCompilationContext
import com.intellij.psi.PsiElement

/**
 * Returns the statement a reference to a class or multiclass resolving to [statements], in text order, refers to. A
 * class may be declared any number of times before it is defined, so its definition is always preferred to its
 * declarations, only returning a declaration if there is no definition.
 */
internal fun <T : TableGenAbstractClassStatement> preferDefinition(statements: List<T>): T? =
    statements.partition {
        it is TableGenClassStatement && it.isDeclaration
    }.let { (decls, defs) ->
        defs.lastOrNull() ?: decls.lastOrNull()
    }

/**
 * Interface used to inject methods into [com.github.zero9178.mlirods.language.generated.psi.TableGenAbstractRef], the
 * base of the references to classes and multiclasses.
 */
interface TableGenAbstractRefEx : PsiElement, TableGenReferencingElement {
    /**
     * Returns the class or multiclass being referenced within [context] or null if resolution failed. Of the
     * statements of a class, its definition is preferred to its declarations.
     *
     * The references to classes narrow this in [TableGenAbstractClassRefEx], leaving the names of 'defm' and
     * 'multiclass' statements, which refer to a multiclass or, within a 'defm', to a class.
     */
    override suspend fun referencedDefinition(context: TableGenCompilationContext): TableGenAbstractClassStatement? =
        referencedDefinitionBlocking(context)

    override fun referencedDefinitionBlocking(context: TableGenCompilationContext): TableGenAbstractClassStatement? =
        preferDefinition(
            TableGenMultiClassReference.findTargets(this as TableGenMultiClassRef, context)
                .filterIsInstance<TableGenAbstractClassStatement>()
        )
}
