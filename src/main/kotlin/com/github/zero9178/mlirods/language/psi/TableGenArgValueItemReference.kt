package com.github.zero9178.mlirods.language.psi

import com.github.zero9178.mlirods.language.generated.psi.TableGenAbstractClassRef
import com.github.zero9178.mlirods.language.generated.psi.TableGenArgValueItem
import com.github.zero9178.mlirods.language.psi.impl.TableGenEvaluationContext
import com.github.zero9178.mlirods.language.stubs.disallowTreeLoading
import com.github.zero9178.mlirods.language.values.TableGenStringValue
import com.github.zero9178.mlirods.model.getProjectContextDependentCache
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.PsiReferenceBase
import com.intellij.psi.ResolveResult
import com.intellij.psi.util.parentOfType
import com.intellij.util.concurrency.annotations.RequiresReadLock


class TableGenArgValueItemReference(element: TableGenArgValueItem) :
    PsiReferenceBase.Poly<TableGenArgValueItem>(element) {

    override fun hashCode(): Int {
        return element.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return element === (other as? TableGenArgValueItemReference)?.element
    }

    @RequiresReadLock
    override fun multiResolve(incompleteCode: Boolean): Array<out ResolveResult> =
        getProjectContextDependentCache(element) {
            disallowTreeLoading {
                val classRef = element.parentOfType<TableGenAbstractClassRef>()
                    ?: return@disallowTreeLoading emptyArray<ResolveResult>()
                val targetClass = classRef.referencedClass ?: return@disallowTreeLoading emptyArray<ResolveResult>()

                val templateArg = if (element.isNamedArgument) {
                    val identifierName = element.identifierName
                    val nameNode = element.nameNode
                    val argumentName = when {
                        identifierName != null -> identifierName
                        nameNode != null -> when (val result = nameNode.evaluate(TableGenEvaluationContext())) {
                            is TableGenStringValue -> result.value
                            else -> return@disallowTreeLoading emptyArray<ResolveResult>()
                        }

                        else -> return@disallowTreeLoading emptyArray<ResolveResult>()
                    }
                    targetClass.templateArgDeclList.find {
                        it.name == argumentName
                    }
                } else {
                    val index = classRef.argValueItemList.binarySearchBy(element.startOffsetInParent) {
                        it.startOffsetInParent
                    }
                    assert(index >= 0) {
                        "element should be guaranteed to be within the arg-value item list"
                    }
                    targetClass.templateArgDeclList.getOrNull(index)
                }
                if (templateArg == null) return@disallowTreeLoading emptyArray<ResolveResult>()

                arrayOf(PsiElementResolveResult(templateArg))
            }
        }
}
