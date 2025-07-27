package com.github.zero9178.mlirods.language.highlighting

import com.github.zero9178.mlirods.color.FIELD
import com.github.zero9178.mlirods.color.PREPROCESSOR_MACRO_NAME
import com.github.zero9178.mlirods.color.SKIPPED_CODE
import com.github.zero9178.mlirods.language.psi.TableGenFile
import com.github.zero9178.mlirods.language.generated.psi.*
import com.github.zero9178.mlirods.language.psi.TableGenFieldIdentifierNode
import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.codeInsight.daemon.impl.HighlightInfoType
import com.intellij.codeInsight.daemon.impl.HighlightVisitor
import com.intellij.codeInsight.daemon.impl.analysis.HighlightInfoHolder
import com.intellij.openapi.project.DumbAware
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile

private class TableGenDumbAwareSemanticTokensAnnotator : HighlightVisitor, DumbAware {

    private var myHolder: HighlightInfoHolder? = null


    private fun addInfo(highlightInfo: HighlightInfo?) {
        myHolder!!.add(highlightInfo)
    }

    override fun clone(): HighlightVisitor {
        return TableGenDumbAwareSemanticTokensAnnotator()
    }

    override fun suitableForFile(file: PsiFile): Boolean {
        return file is TableGenFile
    }

    override fun analyze(
        file: PsiFile,
        updateWholeFile: Boolean,
        holder: HighlightInfoHolder,
        action: Runnable
    ): Boolean {
        myHolder = holder
        action.run()
        return true
    }

    override fun visit(element: PsiElement) {
        when (element) {
            is TableGenFieldIdentifierNode -> {
                element.fieldIdentifier?.let {
                    addInfo(
                        HighlightInfo.newHighlightInfo(HighlightInfoType.TEXT_ATTRIBUTES)
                            .range(it)
                            .textAttributes(FIELD)
                            .create()
                    )
                }
            }

            is TableGenDefineDirective -> {
                val identifier = element.identifier ?: return
                addInfo(
                    HighlightInfo.newHighlightInfo(HighlightInfoType.TEXT_ATTRIBUTES)
                        .range(identifier)
                        .textAttributes(PREPROCESSOR_MACRO_NAME)
                        .create()
                )
            }

            is TableGenIfdefIfndefDirective -> {
                val identifier = element.identifier ?: return
                addInfo(
                    HighlightInfo.newHighlightInfo(HighlightInfoType.TEXT_ATTRIBUTES)
                        .range(identifier)
                        .textAttributes(PREPROCESSOR_MACRO_NAME)
                        .create()
                )
            }

            is TableGenSkippedCodeBlock -> {
                addInfo(
                    HighlightInfo.newHighlightInfo(HighlightInfoType.TEXT_ATTRIBUTES)
                        .range(element)
                        .textAttributes(SKIPPED_CODE)
                        .create()
                )
            }
        }
    }
}

private class TableGenSemanticTokensAnnotator : HighlightVisitor {

    private var myHolder: HighlightInfoHolder? = null


    private fun addInfo(highlightInfo: HighlightInfo?) {
        myHolder!!.add(highlightInfo)
    }

    override fun clone(): HighlightVisitor {
        return TableGenSemanticTokensAnnotator()
    }

    override fun suitableForFile(file: PsiFile): Boolean {
        return file is TableGenFile
    }

    override fun analyze(
        file: PsiFile,
        updateWholeFile: Boolean,
        holder: HighlightInfoHolder,
        action: Runnable
    ): Boolean {
        myHolder = holder
        action.run()
        return true
    }

    override fun visit(element: PsiElement) {
        when (element) {
            is TableGenIdentifierValue -> {
                if (element.reference?.resolve() !is TableGenFieldBodyItem)
                    return

                addInfo(
                    HighlightInfo.newHighlightInfo(HighlightInfoType.TEXT_ATTRIBUTES)
                        .range(element)
                        .textAttributes(FIELD)
                        .create()
                )
            }
        }
    }
}