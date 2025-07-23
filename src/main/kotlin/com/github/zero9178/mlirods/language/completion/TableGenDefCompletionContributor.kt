package com.github.zero9178.mlirods.language.completion

import com.github.zero9178.mlirods.index.DEF_INDEX
import com.github.zero9178.mlirods.index.getElements
import com.github.zero9178.mlirods.index.processAllKeys
import com.github.zero9178.mlirods.language.generated.TableGenTypes
import com.github.zero9178.mlirods.language.psi.TableGenDefReference
import com.github.zero9178.mlirods.model.TableGenIncludedSearchScope
import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.patterns.PlatformPatterns
import com.intellij.util.ProcessingContext

/**
 * Completion contributor suggesting 'def' names of included files as well.
 */
private class TableGenInterFileDefCompletionContributor : CompletionContributor() {
    init {
        extend(
            null, PlatformPatterns.psiElement(TableGenTypes.IDENTIFIER).withParent(
                PlatformPatterns.psiElement().withReference(TableGenDefReference::class.java)
            ), object : CompletionProvider<CompletionParameters>() {
                override fun addCompletions(
                    parameters: CompletionParameters, context: ProcessingContext, result: CompletionResultSet
                ) {
                    val project = parameters.position.project
                    val scope = TableGenIncludedSearchScope(parameters.position, project)
                    val list = mutableListOf<LookupElement>()
                    DEF_INDEX.processAllKeys({ key ->
                        DEF_INDEX.getElements(key, project, scope).forEach {
                            list.add(
                                createLookupElement(it, parameters.position)
                            )
                        }
                        !result.isStopped
                    }, scope)
                    result.addAllElements(list)
                }
            })
    }
}