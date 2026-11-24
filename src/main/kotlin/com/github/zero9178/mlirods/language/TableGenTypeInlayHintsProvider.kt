package com.github.zero9178.mlirods.language

import com.github.zero9178.mlirods.language.generated.psi.TableGenBangOperatorDefinition
import com.github.zero9178.mlirods.language.generated.psi.TableGenBinaryIntegerValueNode
import com.github.zero9178.mlirods.language.generated.psi.TableGenBoolValueNode
import com.github.zero9178.mlirods.language.generated.psi.TableGenClassInstantiationValueNode
import com.github.zero9178.mlirods.language.generated.psi.TableGenDefvarStatement
import com.github.zero9178.mlirods.language.generated.psi.TableGenForeachIterator
import com.github.zero9178.mlirods.language.generated.psi.TableGenIntegerValueNode
import com.github.zero9178.mlirods.language.generated.psi.TableGenSingleBit
import com.github.zero9178.mlirods.language.generated.psi.TableGenStringValueNode
import com.github.zero9178.mlirods.language.generated.psi.TableGenValueNode
import com.github.zero9178.mlirods.language.psi.TableGenIdentifierElement
import com.github.zero9178.mlirods.language.psi.TableGenTypedElement
import com.github.zero9178.mlirods.language.types.TableGenListType
import com.github.zero9178.mlirods.language.types.TableGenRecordType
import com.github.zero9178.mlirods.language.types.TableGenType
import com.github.zero9178.mlirods.language.types.TableGenUndefType
import com.github.zero9178.mlirods.language.types.TableGenUnknownType
import com.github.zero9178.mlirods.model.TableGenCompilationContext
import com.intellij.codeInsight.hints.declarative.HintFormat
import com.intellij.codeInsight.hints.declarative.InlayActionData
import com.intellij.codeInsight.hints.declarative.InlayHintsCollector
import com.intellij.codeInsight.hints.declarative.InlayHintsProvider
import com.intellij.codeInsight.hints.declarative.InlayTreeSink
import com.intellij.codeInsight.hints.declarative.InlineInlayPosition
import com.intellij.codeInsight.hints.declarative.PresentationTreeBuilder
import com.intellij.codeInsight.hints.declarative.PsiPointerInlayActionNavigationHandler
import com.intellij.codeInsight.hints.declarative.PsiPointerInlayActionPayload
import com.intellij.codeInsight.hints.declarative.SharedBypassCollector
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.createSmartPointer

/**
 * Shows the type of named definitions whose type is not written in the source, e.g. a 'defvar', a 'foreach' iterator
 * or the variables of bang operators like '!foreach'. Like a written type, the hint precedes the name.
 */
class TableGenTypeInlayHintsProvider : InlayHintsProvider {
    override fun createCollector(file: PsiFile, editor: Editor): InlayHintsCollector =
        Collector(TableGenCompilationContext.activeFor(file))

    private class Collector(private val context: TableGenCompilationContext) : SharedBypassCollector {
        override fun collectFromElement(element: PsiElement, sink: InlayTreeSink) {
            if (element !is TableGenTypedElement || element !is TableGenIdentifierElement) return
            if (!isTypeHidden(element)) return

            val type = element.typeBlocking(context)
            if (type is TableGenUnknownType || type is TableGenUndefType) return

            val name = element.nameIdentifier ?: return
            val position = InlineInlayPosition(name.textRange.startOffset, relatedToPrevious = false)
            sink.addPresentation(position, hintFormat = HintFormat.default) {
                TypePresentationBuilder(this).apply { appendType(type) }.flush()
            }
        }

        /**
         * Returns true if the type of [element] can neither be read from its source nor is obvious from the value it
         * is defined as, e.g. a literal.
         */
        private fun isTypeHidden(element: PsiElement): Boolean = when (element) {
            is TableGenDefvarStatement -> !isObviouslyTyped(element.valueNode)
            // A range iterates over integers, as does an iterator given a single integer.
            is TableGenForeachIterator -> !element.isRangeList &&
                    (element.rangePieceList.singleOrNull() as? TableGenSingleBit)?.let {
                        !isObviouslyTyped(it.valueNode)
                    } ?: false

            is TableGenBangOperatorDefinition -> true
            else -> false
        }

        private fun isObviouslyTyped(value: TableGenValueNode?): Boolean = when (value) {
            is TableGenIntegerValueNode, is TableGenBinaryIntegerValueNode, is TableGenBoolValueNode,
            is TableGenStringValueNode, is TableGenClassInstantiationValueNode -> true

            else -> false
        }

        /**
         * Appends types to [builder] in TableGen syntax, making the classes within them navigate to their definition.
         * Text between class names is appended as a single piece once [flush]ed.
         */
        private inner class TypePresentationBuilder(private val builder: PresentationTreeBuilder) {
            private val pending = StringBuilder()

            fun appendType(type: TableGenType) {
                when (type) {
                    is TableGenListType -> {
                        pending.append("list<")
                        appendType(type.elementType)
                        pending.append(">")
                    }

                    is TableGenRecordType -> {
                        val record = type.record(context)
                        if (record == null) {
                            pending.append(type.recordName)
                            return
                        }
                        flush()
                        val action = InlayActionData(
                            PsiPointerInlayActionPayload(record.createSmartPointer()),
                            PsiPointerInlayActionNavigationHandler.HANDLER_ID
                        )
                        builder.text(type.recordName, action)
                    }

                    else -> pending.append(type.toString())
                }
            }

            fun flush() {
                if (pending.isEmpty()) return
                builder.text(pending.toString())
                pending.clear()
            }
        }
    }
}
