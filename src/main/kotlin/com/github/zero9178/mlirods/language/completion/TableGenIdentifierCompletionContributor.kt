package com.github.zero9178.mlirods.language.completion

import com.github.zero9178.mlirods.index.ALL_IDENTIFIERS_INDEX
import com.github.zero9178.mlirods.index.processVisibleElements
import com.github.zero9178.mlirods.language.generated.TableGenTypes
import com.github.zero9178.mlirods.language.generated.psi.TableGenIdentifierValueNode
import com.github.zero9178.mlirods.language.psi.TableGenIdentifierReference
import com.github.zero9178.mlirods.model.TableGenVisibility
import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.DumbService
import com.intellij.patterns.PlatformPatterns
import com.intellij.util.ProcessingContext

/**
 * Completion contributor suggesting everything a plain identifier may refer to.
 *
 * The declarations of the enclosing scopes are suggested first. A reference resolves to them before consulting the
 * index, so a declaration of the index sharing a name with one of them cannot be referred to from the position and is
 * left out, no matter which file it is in.
 */
internal class TableGenIdentifierCompletionContributor : CompletionContributor(), DumbAware {
    init {
        extend(
            null, PlatformPatterns.psiElement(TableGenTypes.IDENTIFIER).withParent(
                TableGenIdentifierValueNode::class.java
            ), object : CompletionProvider<CompletionParameters>() {
                override fun addCompletions(
                    parameters: CompletionParameters, context: ProcessingContext, result: CompletionResultSet
                ) {
                    val position = parameters.position
                    val node = position.parent as TableGenIdentifierValueNode

                    val shadowed = HashSet<String>()
                    for (element in TableGenIdentifierReference.findLocalElements(node)) {
                        shadowed += element.name ?: continue
                        result.addElement(createLookupElement(element, position))
                    }

                    // The enclosing scopes are all that is known without the index.
                    if (DumbService.isDumb(position.project)) return

                    ALL_IDENTIFIERS_INDEX.processVisibleElements(0, TableGenVisibility(position)) {
                        if (it.name !in shadowed) result.addElement(createLookupElement(it, position))
                        !result.isStopped
                    }
                }
            })
    }
}
