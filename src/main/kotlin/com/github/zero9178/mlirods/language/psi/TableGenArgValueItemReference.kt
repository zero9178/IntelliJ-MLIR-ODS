package com.github.zero9178.mlirods.language.psi

import com.github.zero9178.mlirods.language.generated.psi.TableGenAbstractClassRef
import com.github.zero9178.mlirods.language.generated.psi.TableGenArgValueItem
import com.github.zero9178.mlirods.language.generated.psi.TableGenTemplateArgDecl
import com.github.zero9178.mlirods.language.psi.impl.TableGenEvaluationContext
import com.github.zero9178.mlirods.language.stubs.disallowTreeLoading
import com.github.zero9178.mlirods.language.values.TableGenStringValue
import com.github.zero9178.mlirods.model.TableGenCompilationContext
import com.github.zero9178.mlirods.model.getProjectContextDependentCache
import com.intellij.openapi.diagnostic.thisLogger
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

    companion object {
        /**
         * Returns the template argument [element] assigns a value to within [context], be it by name or by position,
         * or `null` if there is none. This is what resolving the reference and every lookup on the PSI itself are
         * built on; results are cached per [element] and [context].
         */
        @RequiresReadLock
        fun findTemplateArgDecl(
            element: TableGenArgValueItem, context: TableGenCompilationContext
        ): TableGenTemplateArgDecl? = getProjectContextDependentCache(element, context) { element ->
            disallowTreeLoading {
                val classRef = element.parentOfType<TableGenAbstractClassRef>() ?: return@disallowTreeLoading null
                val targetClass = classRef.referencedClass(context) ?: return@disallowTreeLoading null

                if (element.isNamedArgument) {
                    val identifierName = element.identifierName
                    val nameNode = element.nameNode
                    val argumentName = when {
                        identifierName != null -> identifierName
                        nameNode != null -> when (val result = nameNode.evaluateBlocking(TableGenEvaluationContext(context))) {
                            is TableGenStringValue -> result.value
                            else -> return@disallowTreeLoading null
                        }

                        else -> return@disallowTreeLoading null
                    }
                    targetClass.templateArgDeclList.find {
                        it.name == argumentName
                    }
                } else {
                    val index = classRef.argValueItemList.binarySearchBy(element.startOffsetInParent) {
                        it.startOffsetInParent
                    }
                    // 'element' is one of the arg-value items of the class ref it was reached through, so the search
                    // always finds it.
                    if (index < 0) {
                        thisLogger().error("Positional argument is not among the arg-value items of ${classRef.text}")
                        return@disallowTreeLoading null
                    }
                    targetClass.templateArgDeclList.getOrNull(index)
                }
            }
        }
    }

    /**
     * Resolves the reference in the context the platform sees the file in, i.e. the one it derives from the include
     * graph. Anything that already has a context in hand should use [findTemplateArgDecl] instead.
     */
    @RequiresReadLock
    override fun multiResolve(incompleteCode: Boolean): Array<out ResolveResult> {
        val context = TableGenCompilationContext.activeFor(element)
        return PsiElementResolveResult.createResults(listOfNotNull(findTemplateArgDecl(element, context)))
    }
}
