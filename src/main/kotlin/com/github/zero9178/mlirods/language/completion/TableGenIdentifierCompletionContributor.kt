package com.github.zero9178.mlirods.language.completion

import com.github.zero9178.mlirods.index.ALL_IDENTIFIERS_INDEX
import com.github.zero9178.mlirods.index.processVisibleElements
import com.github.zero9178.mlirods.language.generated.TableGenTypes
import com.github.zero9178.mlirods.language.psi.TableGenIdentifierReference
import com.github.zero9178.mlirods.model.TableGenVisibility
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
internal class TableGenInterFileIdentifierCompletionContributor : CompletionContributor() {
    init {
        extend(
            null, PlatformPatterns.psiElement(TableGenTypes.IDENTIFIER).withParent(
                PlatformPatterns.psiElement().withReference(TableGenIdentifierReference::class.java)
            ), object : CompletionProvider<CompletionParameters>() {
                override fun addCompletions(
                    parameters: CompletionParameters, context: ProcessingContext, result: CompletionResultSet
                ) {
                    val visibility = TableGenVisibility(parameters.position)

                    // Whatever the reference finds within the file itself is suggested by the reference already.
                    val local = parameters.position.parent.references.flatMapTo(mutableSetOf()) { reference ->
                        reference.variants.mapNotNull { (it as? LookupElement)?.lookupString }
                    }

                    ALL_IDENTIFIERS_INDEX.processVisibleElements(0, visibility) {
                        if (!visibility.isInSameFile(it) || it.name !in local)
                            result.addElement(createLookupElement(it, parameters.position))
                        !result.isStopped
                    }
                }
            })
    }
}
