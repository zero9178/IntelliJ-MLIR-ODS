package com.github.zero9178.mlirods.language.completion

import com.github.zero9178.mlirods.language.generated.psi.TableGenClassRef
import com.github.zero9178.mlirods.language.generated.psi.TableGenClassStatement
import com.github.zero9178.mlirods.language.generated.psi.TableGenClassTypeNode
import com.github.zero9178.mlirods.language.generated.psi.TableGenIdentifierValue
import com.intellij.codeInsight.completion.InsertHandler
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.editor.EditorModificationUtilEx
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNamedElement

private class ClassAngleBracketsInsertHandler(private val identifier: PsiElement) : InsertHandler<LookupElement> {
    override fun handleInsert(
        context: InsertionContext,
        item: LookupElement
    ) {
        val classStatement = item.psiElement as? TableGenClassStatement ?: return
        var caretShift = 1
        when (identifier.parent) {
            // Type node never needs brackets.
            is TableGenClassTypeNode -> return
            // Class instantiations always do.
            is TableGenIdentifierValue -> {
                //
                if (classStatement.templateArgDeclList.isEmpty())
                    caretShift = 2
            }
            // Class ref does depending on whether the class template arguments or not.
            is TableGenClassRef -> {
                // TODO: Double check how default template arguments must be handled here.
                if (classStatement.templateArgDeclList.isEmpty()) return
            }

            else -> return
        }

        val editor = context.editor
        val document = editor.document
        val offset = editor.caretModel.offset
        // Don't insert brackets if they are already there!
        if (offset < document.textLength && document.charsSequence[offset] == '<')
            return

        EditorModificationUtilEx.insertStringAtCaret(editor, "<>", false, caretShift)
        editor.project?.let { PsiDocumentManager.getInstance(it).commitDocument(document) }
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