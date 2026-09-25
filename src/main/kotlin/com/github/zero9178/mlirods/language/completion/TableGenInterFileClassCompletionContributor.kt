package com.github.zero9178.mlirods.language.completion

import com.github.zero9178.mlirods.index.ALL_CLASSES_INDEX
import com.github.zero9178.mlirods.index.processVisibleElements
import com.github.zero9178.mlirods.language.generated.TableGenTypes
import com.github.zero9178.mlirods.language.psi.TableGenClassReference
import com.github.zero9178.mlirods.model.TableGenCompilationContext
import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.patterns.PlatformPatterns
import com.intellij.patterns.StandardPatterns
import com.intellij.util.ProcessingContext

/**
 * Completion contributor suggesting class statements of included files as well.
 */
internal class TableGenInterFileCompletionContributor : CompletionContributor() {
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
                    // Completion is where the IDE enters: everything below resolves in the context the file derives
                    // from the include graph.
                    val seenFrom = TableGenCompilationContext.activeFor(parameters.originalFile).at(parameters.position)

                    ALL_CLASSES_INDEX.processVisibleElements(0, seenFrom) {
                        // The classes of the file itself are suggested by the reference already.
                        if (!seenFrom.isInSameFile(it) &&
                            !TableGenClassReference.isOwnClassVariant(it, parameters.position)
                        ) result.addElement(createLookupElement(it, parameters.position))
                        !result.isStopped
                    }
                }
            }
        )
    }
}