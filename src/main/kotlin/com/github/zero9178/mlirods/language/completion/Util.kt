package com.github.zero9178.mlirods.language.completion

import com.github.zero9178.mlirods.language.generated.psi.TableGenClassRef
import com.github.zero9178.mlirods.language.generated.psi.TableGenClassStatement
import com.github.zero9178.mlirods.language.generated.psi.TableGenClassTypeNode
import com.github.zero9178.mlirods.language.generated.psi.TableGenIdentifierValueNode
import com.github.zero9178.mlirods.language.stubs.disallowTreeLoading
import com.intellij.codeInsight.AutoPopupController
import com.intellij.codeInsight.completion.InsertHandler
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.editorActions.TabOutScopesTracker
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.EditorModificationUtilEx
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNamedElement

/**
 * The end of the type argument starting with the '<' at [start]: the offset of the matching '>', or, if the '<' is not
 * followed by one, the offset the missing '>' has to be inserted at, which is the first character that cannot be part
 * of a type. A type never spans multiple lines.
 */
private fun typeArgumentEnd(chars: CharSequence, start: Int): Int {
    var depth = 1
    for (i in start + 1..<chars.length) {
        val c = chars[i]
        when {
            c == '<' -> depth++
            c == '>' -> if (--depth == 0) return i
            c == '\n' -> return i
            !c.isLetterOrDigit() && c != '_' && c != ',' && !c.isWhitespace() -> return i
        }
    }
    return chars.length
}

/**
 * A balanced '<...>' established by [establishAngleBrackets]. [start] and [end] are the offsets of the '<' and '>'.
 * [insertedClosing] is set if the '>' had to be inserted rather than being reused.
 */
internal class AngleBrackets(val start: Int, val end: Int, val insertedClosing: Boolean) {
    /**
     * Whether there is no text between the brackets yet.
     */
    val isEmpty get() = end == start + 1
}

/**
 * Establishes a balanced '<...>' holding a type argument at [offset], reusing as much as the user has already written:
 * an existing '<' is kept and only closed with a '>' if the type text following it lacks one; without an existing '<'
 * an empty pair is inserted.
 */
internal fun establishAngleBrackets(document: Document, offset: Int): AngleBrackets {
    if (document.charsSequence.getOrNull(offset) == '<') {
        val end = typeArgumentEnd(document.charsSequence, offset)
        val insertedClosing = document.charsSequence.getOrNull(end) != '>'
        if (insertedClosing) document.insertString(end, ">")
        return AngleBrackets(offset, end, insertedClosing)
    }
    document.insertString(offset, "<>")
    return AngleBrackets(offset, offset + 1, insertedClosing = true)
}

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

        // A '<' the completion was selected with is established by this handler already and must not be typed into the
        // brackets the caret is placed in.
        if (context.completionChar == '<') context.setAddCompletionChar(false)

        val editor = context.editor
        val document = editor.document
        val offset = editor.caretModel.offset
        if (document.charsSequence.getOrNull(offset) == '<') {
            // Enter brackets the user has already written if they are still empty and the class has template arguments
            // to fill in. Unlike a type argument, filled or unclosed brackets are left alone: template arguments are
            // arbitrary values, whose end cannot reliably be scanned for.
            if (!hasParams || document.charsSequence.getOrNull(offset + 1) != '>') return@disallowTreeLoading
            editor.caretModel.moveToOffset(offset + 1)
        } else {
            EditorModificationUtilEx.insertStringAtCaret(editor, "<>", false, if (hasParams) 1 else 2)
            if (!hasParams) return@disallowTreeLoading
        }

        TabOutScopesTracker.getInstance().registerEmptyScopeAtCaret(editor)

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