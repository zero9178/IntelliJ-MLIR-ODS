package com.github.zero9178.mlirods.language.completion

import com.github.zero9178.mlirods.language.generated.TableGenTypes
import com.github.zero9178.mlirods.language.psi.TableGenClassReference
import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.openapi.project.DumbAware
import com.intellij.patterns.PlatformPatterns
import com.intellij.util.ProcessingContext

/**
 * Injects the values returned by [TableGenClassReference] into contexts accepting a
 * [com.github.zero9178.mlirods.language.generated.psi.TableGenIdentifierValue].
 * This is needed due to a special case: Unless brackets are present, the parser will be parse initial identifiers
 * as an identifier value rather than a class instantiation and never call [TableGenClassReference.getVariants].
 * We detect this case and inject it here manually.
 */
private class TableGenClassReferenceCompletionContributor : CompletionContributor(), DumbAware {
    init {
        extend(
            null,
            PlatformPatterns.psiElement(TableGenTypes.IDENTIFIER).withParent(
                PlatformPatterns.psiElement(TableGenTypes.IDENTIFIER_VALUE),
            ),
            object : CompletionProvider<CompletionParameters>() {
                override fun addCompletions(
                    parameters: CompletionParameters,
                    context: ProcessingContext,
                    result: CompletionResultSet
                ) {
                    result.addAllElements(TableGenClassReference.getVariants(parameters.position).toList())
                }
            }
        )
    }
}