package com.github.zero9178.mlirods.language.completion

import com.github.zero9178.mlirods.language.generated.TableGenTypes
import com.github.zero9178.mlirods.language.generated.psi.TableGenFieldAccessValue
import com.github.zero9178.mlirods.language.generated.psi.TableGenIdentifierValue
import com.github.zero9178.mlirods.language.generated.psi.TableGenLetBodyItem
import com.github.zero9178.mlirods.language.psi.TableGenFieldScopeNode
import com.github.zero9178.mlirods.language.types.TableGenRecordType
import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.patterns.PlatformPatterns
import com.intellij.patterns.StandardPatterns
import com.intellij.psi.util.parentOfType
import com.intellij.util.ProcessingContext

private class TableGenFieldCompletionContributor : CompletionContributor() {
    init {

        extend(
            null,
            PlatformPatterns.psiElement(TableGenTypes.IDENTIFIER).withParent(
                StandardPatterns.or(
                    PlatformPatterns.psiElement(TableGenIdentifierValue::class.java),
                    PlatformPatterns.psiElement(TableGenLetBodyItem::class.java),
                )
            ),
            object : CompletionProvider<CompletionParameters>() {
                override fun addCompletions(
                    parameters: CompletionParameters,
                    context: ProcessingContext,
                    result: CompletionResultSet
                ) {
                    val scope = parameters.position.parentOfType<TableGenFieldScopeNode>() ?: return
                    scope.allFields.forEach {
                        result.addElement(LookupElementBuilder.create(it))
                    }
                }
            }
        )
        extend(
            null,
            PlatformPatterns.psiElement(TableGenTypes.IDENTIFIER).withParent(
                TableGenFieldAccessValue::class.java
            ), object : CompletionProvider<CompletionParameters>() {
                override fun addCompletions(
                    parameters: CompletionParameters,
                    context: ProcessingContext,
                    result: CompletionResultSet
                ) {
                    val fieldAccess = parameters.position.parentOfType<TableGenFieldAccessValue>() ?: return
                    val recordType = fieldAccess.value.type as? TableGenRecordType ?: return
                    recordType.record?.allFields?.forEach {
                        result.addElement(LookupElementBuilder.create(it))
                    }
                }
            })
    }
}