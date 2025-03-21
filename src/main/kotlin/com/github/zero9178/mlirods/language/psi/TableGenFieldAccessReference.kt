package com.github.zero9178.mlirods.language.psi

import com.github.zero9178.mlirods.language.generated.psi.TableGenFieldAccessValue
import com.github.zero9178.mlirods.language.generated.psi.TableGenFieldIdentifier
import com.github.zero9178.mlirods.language.types.TableGenRecordType
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.PsiReferenceBase
import com.intellij.psi.ResolveResult
import com.intellij.psi.util.parentOfType
import com.intellij.util.concurrency.annotations.RequiresReadLock


class TableGenFieldAccessReference(element: TableGenFieldIdentifier) :
    PsiReferenceBase.Poly<TableGenFieldIdentifier>(element) {

    @RequiresReadLock
    override fun multiResolve(incompleteCode: Boolean): Array<out ResolveResult> {
        val fieldAccess = element.parentOfType<TableGenFieldAccessValue>() ?: return emptyArray()

        val type = fieldAccess.value.type
        return when (type) {
            is TableGenRecordType -> {
                type.record.fields[element]?.let {
                    arrayOf(PsiElementResolveResult(it))
                } ?: emptyArray()
            }

            else -> emptyArray()
        }
    }
}
