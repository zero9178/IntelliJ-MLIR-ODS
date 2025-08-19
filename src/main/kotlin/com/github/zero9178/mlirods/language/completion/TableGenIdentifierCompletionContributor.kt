package com.github.zero9178.mlirods.language.completion

import com.github.zero9178.mlirods.index.ALL_IDENTIFIERS_INDEX
import com.github.zero9178.mlirods.index.processElements
import com.github.zero9178.mlirods.language.generated.TableGenTypes
import com.github.zero9178.mlirods.language.psi.TableGenIdentifierReference
import com.github.zero9178.mlirods.model.TableGenIncludedSearchScope
import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.patterns.PlatformPatterns
import com.intellij.util.ProcessingContext

/**
 * Completion contributor suggesting 'def' names of included files as well.
 */
private class TableGenInterFileIdentifierCompletionContributor : CompletionContributor() {
    init {
        extend(
            null, PlatformPatterns.psiElement(TableGenTypes.IDENTIFIER).withParent(
                PlatformPatterns.psiElement().withReference(TableGenIdentifierReference::class.java)
            ), object : CompletionProvider<CompletionParameters>() {
                override fun addCompletions(
                    parameters: CompletionParameters, context: ProcessingContext, result: CompletionResultSet
                ) {
                    val project = parameters.position.project
                    val scope = TableGenIncludedSearchScope(parameters.position, project)

                    result.startBatch()
                    ALL_IDENTIFIERS_INDEX.processElements(0, project, scope) {
                        result.addElement(createLookupElement(it, parameters.position))
                        !result.isStopped
                    }
                    result.endBatch()
                }
            })
    }
}
