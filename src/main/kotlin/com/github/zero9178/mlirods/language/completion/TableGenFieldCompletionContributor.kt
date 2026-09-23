package com.github.zero9178.mlirods.language.completion

import com.github.zero9178.mlirods.language.generated.TableGenTypes
import com.github.zero9178.mlirods.language.generated.psi.TableGenFieldAccessValueNode
import com.github.zero9178.mlirods.language.generated.psi.TableGenLetBodyItem
import com.github.zero9178.mlirods.language.psi.TableGenFieldScopeNode
import com.github.zero9178.mlirods.language.types.TableGenRecordType
import com.github.zero9178.mlirods.model.TableGenCompilationContext
import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.util.parentOfType
import com.intellij.util.ProcessingContext

internal class TableGenFieldCompletionContributor : CompletionContributor() {
    init {

        extend(
            null,
            PlatformPatterns.psiElement(TableGenTypes.IDENTIFIER).withParent(
                PlatformPatterns.psiElement(TableGenLetBodyItem::class.java)
            ),
            object : CompletionProvider<CompletionParameters>() {
                override fun addCompletions(
                    parameters: CompletionParameters,
                    context: ProcessingContext,
                    result: CompletionResultSet
                ) {
                    val scope = parameters.position.parentOfType<TableGenFieldScopeNode>() ?: return
                    val compilationContext = TableGenCompilationContext.activeFor(parameters.originalFile)
                    // References are resolved from the original file rather than its copy, which no index knows about.
                    CompletionUtil.getOriginalOrSelf(scope).allFields(compilationContext).forEach {
                        result.addElement(LookupElementBuilder.create(it))
                    }
                }
            }
        )
        extend(
            null,
            PlatformPatterns.psiElement(TableGenTypes.IDENTIFIER).withParent(
                TableGenFieldAccessValueNode::class.java
            ), object : CompletionProvider<CompletionParameters>() {
                override fun addCompletions(
                    parameters: CompletionParameters,
                    context: ProcessingContext,
                    result: CompletionResultSet
                ) {
                    val fieldAccess = parameters.position.parentOfType<TableGenFieldAccessValueNode>() ?: return
                    val compilationContext = TableGenCompilationContext.activeFor(parameters.originalFile)
                    // References are resolved from the original file rather than its copy, which no index knows about.
                    val recordType = CompletionUtil.getOriginalOrSelf(fieldAccess.valueNode)
                        .typeBlocking(compilationContext) as? TableGenRecordType ?: return
                    recordType.record(compilationContext)?.allFields(compilationContext)?.forEach {
                        result.addElement(LookupElementBuilder.create(it))
                    }
                }
            })
    }
}