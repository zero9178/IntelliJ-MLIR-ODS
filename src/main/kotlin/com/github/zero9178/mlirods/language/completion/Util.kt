package com.github.zero9178.mlirods.language.completion

import com.github.zero9178.mlirods.language.generated.psi.TableGenClassRef
import com.github.zero9178.mlirods.language.generated.psi.TableGenClassStatement
import com.github.zero9178.mlirods.language.generated.psi.TableGenClassTypeNode
import com.github.zero9178.mlirods.language.generated.psi.TableGenIdentifierValueNode
import com.github.zero9178.mlirods.language.stubs.disallowTreeLoading
import com.intellij.codeInsight.AutoPopupController
import com.intellij.codeInsight.completion.InsertHandler
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.editor.EditorModificationUtilEx
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNamedElement

private class ClassAngleBracketsInsertHandler(private val identifier: PsiElement) : InsertHandler<LookupElement> {
    override fun handleInsert(
        context: InsertionContext,
        item: LookupElement
    ): Unit = disallowTreeLoading {
        val classStatement = item.psiElement as? TableGenClassStatement ?: return@disallowTreeLoading
        var hasParams = true
        when (identifier.parent) {
            // Type node never needs brackets.
            is TableGenClassTypeNode -> return@disallowTreeLoading
            // Class instantiations always do.
            is TableGenIdentifierValueNode -> {
                if (classStatement.templateArgDeclList.isEmpty()) hasParams = false
            }
            // Class ref does depending on whether the class template arguments or not.
            is TableGenClassRef -> {
                // TODO: Double check how default template arguments must be handled here.
                if (classStatement.templateArgDeclList.isEmpty()) return@disallowTreeLoading
            }

            else -> return@disallowTreeLoading
        }

        val editor = context.editor
        val document = editor.document
        val offset = editor.caretModel.offset
        // Don't insert brackets if they are already there!
        if (offset < document.textLength && document.charsSequence[offset] == '<')
            return@disallowTreeLoading

        EditorModificationUtilEx.insertStringAtCaret(editor, "<>", false, if (hasParams) 1 else 2)

        if (!hasParams) return@disallowTreeLoading

        // Invoke parameters popup.
        AutoPopupController.getInstance(context.project)
            .autoPopupParameterInfo(editor, classStatement)
    }
}

/**
 * Function used to create a [LookupElement] from a TableGen Psi member.
 * This should be used to create consistent [LookupElement] regardless of the contributor used.
 */
fun createLookupElement(toSuggest: PsiNamedElement, positionToken: PsiElement): LookupElement {
    return when (toSuggest) {
        is TableGenClassStatement -> LookupElementBuilder.createWithIcon(toSuggest)
            .withInsertHandler(ClassAngleBracketsInsertHandler(positionToken))

        else -> LookupElementBuilder.createWithIcon(toSuggest)
    }
}