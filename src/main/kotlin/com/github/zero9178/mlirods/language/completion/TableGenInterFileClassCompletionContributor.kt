package com.github.zero9178.mlirods.language.completion

import com.github.zero9178.mlirods.index.CLASS_INDEX
import com.github.zero9178.mlirods.index.getElements
import com.github.zero9178.mlirods.index.processAllKeys
import com.github.zero9178.mlirods.language.generated.TableGenTypes
import com.github.zero9178.mlirods.language.psi.TableGenClassReference
import com.github.zero9178.mlirods.model.TableGenIncludedSearchScope
import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.patterns.PlatformPatterns
import com.intellij.patterns.StandardPatterns
import com.intellij.util.ProcessingContext

/**
 * Completion contributor suggesting class statements of included files as well.
 */
private class TableGenInterFileCompletionContributor : CompletionContributor() {
    init {
        extend(
            null,
            PlatformPatterns.psiElement(TableGenTypes.IDENTIFIER).withParent(
                StandardPatterns.or(
                    PlatformPatterns.psiElement().withReference(TableGenClassReference::class.java),
                    // Plain identifiers in values are treated as 'defs' due to the missing brackets.
                    // We need to also inject ourselves into identifier value lookup.
                    PlatformPatterns.psiElement(TableGenTypes.IDENTIFIER_VALUE_NODE),
                )
            ),
            object : CompletionProvider<CompletionParameters>() {
                override fun addCompletions(
                    parameters: CompletionParameters,
                    context: ProcessingContext,
                    result: CompletionResultSet
                ) {
                    val project = parameters.position.project
                    val scope = TableGenIncludedSearchScope(parameters.position, project)
                    val list = mutableListOf<LookupElement>()
                    CLASS_INDEX.processAllKeys({ key ->
                        CLASS_INDEX.getElements(key, project, scope).forEach {
                            list.add(
                                createLookupElement(it, parameters.position)
                            )
                        }
                        !result.isStopped
                    }, scope)
                    result.addAllElements(list)
                }
            }
        )
    }
}