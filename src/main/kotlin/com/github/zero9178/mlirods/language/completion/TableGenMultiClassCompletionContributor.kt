package com.github.zero9178.mlirods.language.completion

import com.github.zero9178.mlirods.index.ALL_CLASSES_INDEX
import com.github.zero9178.mlirods.index.ALL_MULTICLASSES_INDEX
import com.github.zero9178.mlirods.index.processVisibleElements
import com.github.zero9178.mlirods.language.generated.TableGenTypes
import com.github.zero9178.mlirods.language.generated.psi.TableGenDefmStatement
import com.github.zero9178.mlirods.language.generated.psi.TableGenMultiClassRef
import com.github.zero9178.mlirods.language.generated.psi.TableGenMulticlassStatement
import com.github.zero9178.mlirods.language.psi.refersToClass
import com.github.zero9178.mlirods.model.TableGenCompilationContext
import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.CompletionUtil
import com.intellij.patterns.PlatformPatterns
import com.intellij.util.ProcessingContext

/**
 * Completion contributor suggesting what a name in the parent list of a 'defm' or 'multiclass' statement may refer to:
 * the visible multiclasses and, within a 'defm', the visible classes, see
 * [com.github.zero9178.mlirods.language.psi.refersToClass]. The first name of a 'defm' always refers to a multiclass,
 * and once a name refers to a class, so does every name following it.
 *
 * A multiclass is not suggested within its own parent list: TableGen accepts it there, but as the multiclass has no
 * entries yet at that point, deriving from it does nothing.
 */
internal class TableGenMultiClassCompletionContributor : CompletionContributor() {
    init {
        extend(
            null,
            PlatformPatterns.psiElement(TableGenTypes.IDENTIFIER).withParent(TableGenMultiClassRef::class.java),
            object : CompletionProvider<CompletionParameters>() {
                override fun addCompletions(
                    parameters: CompletionParameters,
                    context: ProcessingContext,
                    result: CompletionResultSet
                ) {
                    val position = parameters.position
                    val ref = position.parent as TableGenMultiClassRef

                    // Completion is where the IDE enters: everything below resolves in the context the file derives
                    // from the include graph.
                    val compilationContext = TableGenCompilationContext.activeFor(parameters.originalFile)
                    val seenFrom = compilationContext.at(position)

                    // A name only refers to a class if the name preceding it either does so or is not the first one.
                    val previous = (ref.parent as? TableGenDefmStatement)?.multiClassRefList?.let {
                        it.getOrNull(it.indexOf(ref) - 1)
                    }
                    val mayBeClass = previous != null
                    val mustBeClass = previous != null && refersToClass(previous, compilationContext)

                    if (!mustBeClass) {
                        // The index holds the statements of the original file rather than the copy completed in.
                        val enclosing = (ref.parent as? TableGenMulticlassStatement)?.let {
                            CompletionUtil.getOriginalOrSelf(it)
                        }
                        ALL_MULTICLASSES_INDEX.processVisibleElements(0, seenFrom) {
                            if (it != enclosing) result.addElement(createLookupElement(it, position))
                            !result.isStopped
                        }
                    }
                    if (mayBeClass) {
                        ALL_CLASSES_INDEX.processVisibleElements(0, seenFrom) {
                            result.addElement(createLookupElement(it, position))
                            !result.isStopped
                        }
                    }
                }
            }
        )
    }
}
