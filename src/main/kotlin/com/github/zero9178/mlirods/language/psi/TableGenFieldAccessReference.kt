package com.github.zero9178.mlirods.language.psi

import com.github.zero9178.mlirods.language.generated.psi.TableGenFieldAccessValueNode
import com.github.zero9178.mlirods.language.types.TableGenRecordType
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.PsiReferenceBase
import com.intellij.psi.ResolveResult
import com.intellij.util.concurrency.annotations.RequiresReadLock


class TableGenFieldAccessReference(element: TableGenFieldAccessValueNode) :
    PsiReferenceBase.Poly<TableGenFieldAccessValueNode>(element) {

    @RequiresReadLock
    override fun multiResolve(incompleteCode: Boolean): Array<out ResolveResult> {
        val fieldName = element.fieldName ?: return emptyArray()
        val type = element.valueNode.type
        return when (type) {
            is TableGenRecordType -> {
                type.record?.fields?.get(fieldName)?.let {
                    arrayOf(PsiElementResolveResult(it))
                } ?: emptyArray()
            }

            else -> emptyArray()
        }
    }
}
