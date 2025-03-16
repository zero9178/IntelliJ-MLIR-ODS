package com.github.zero9178.mlirods.language.psi

import com.github.zero9178.mlirods.language.generated.psi.TableGenClassStatement
import com.github.zero9178.mlirods.language.generated.psi.TableGenDefStatement
import com.github.zero9178.mlirods.language.generated.psi.TableGenLetBodyItem
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.PsiReferenceBase
import com.intellij.psi.ResolveResult
import com.intellij.psi.util.parentOfTypes
import com.intellij.util.concurrency.annotations.RequiresReadLock

/**
 * Resolution used to find fields referenced by 'let' body items.
 */
class TableGenLetReference(element: TableGenLetBodyItem) : PsiReferenceBase.Poly<TableGenLetBodyItem>(element) {

    @RequiresReadLock
    override fun multiResolve(incompleteCode: Boolean): Array<out ResolveResult> {
        val identifier = element.fieldIdentifier ?: return emptyArray()
        val parent =
            element.parentOfTypes(TableGenClassStatement::class, TableGenDefStatement::class) ?: return emptyArray()
        parent.fields[identifier]?.let {
            return arrayOf(PsiElementResolveResult(it))
        }
        return emptyArray()
    }
}
