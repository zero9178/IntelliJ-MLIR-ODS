package com.github.zero9178.mlirods.language.psi.impl

import com.github.zero9178.mlirods.language.generated.psi.TableGenBangOperatorDefinition
import com.github.zero9178.mlirods.language.psi.TableGenIdentifierScopeNode
import com.github.zero9178.mlirods.language.psi.TableGenIdentifierScopeNode.IdMapEntry
import com.github.zero9178.mlirods.model.TableGenCompilationContext
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil

/**
 * A bang operator whose body is a scope of the names it defines, e.g. the iterator of '!foreach' or the accumulator
 * and iterator of '!foldl'.
 */
interface TableGenBangOperatorScopeNodeEx : PsiElement, TableGenIdentifierScopeNode {
    /**
     * Returns the names defined by the operator. A name defined more than once refers to its first definition.
     */
    override suspend fun directIdMap(context: TableGenCompilationContext): Map<String, List<IdMapEntry>> = buildMap {
        for (definition in PsiTreeUtil.getStubChildrenOfTypeAsList(this@TableGenBangOperatorScopeNodeEx, DEFINITION)) {
            definition.name?.let { putIfAbsent(it, listOf(IdMapEntry(definition))) }
        }
    }
}

private val DEFINITION = TableGenBangOperatorDefinition::class.java
