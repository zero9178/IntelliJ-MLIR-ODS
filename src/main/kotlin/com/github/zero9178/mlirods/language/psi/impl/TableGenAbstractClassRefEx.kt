package com.github.zero9178.mlirods.language.psi.impl

import com.github.zero9178.mlirods.language.generated.psi.TableGenClassStatement
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiPolyVariantReference
import com.intellij.util.containers.sequenceOfNotNull

interface TableGenAbstractClassRefEx : PsiElement {
    /**
     * Returns the class being referenced or null if resolution failed.
     */
    val referencedClass: TableGenClassStatement?
        get() = references.asSequence().flatMap {
            when (it) {
                is PsiPolyVariantReference -> it.multiResolve(false).asSequence().mapNotNull { result ->
                    result.element as? TableGenClassStatement
                }

                else -> sequenceOfNotNull(it.resolve() as? TableGenClassStatement)
            }
        }.partition {
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