package com.github.zero9178.mlirods.language.insertion

import com.github.zero9178.mlirods.language.TableGenFileType
import com.github.zero9178.mlirods.language.generated.TableGenTypes.RANGLE
import com.intellij.codeInsight.CodeInsightSettings
import com.intellij.codeInsight.editorActions.BackspaceHandlerDelegate
import com.intellij.codeInsight.highlighting.BraceMatchingUtil
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiFile

/**
 * Deletes the '>' paired with a '<' when the '<' is deleted, the way the platform does for '(', '[' and '{'.
 */
internal class TableGenBackspaceHandlerDelegate : BackspaceHandlerDelegate() {
    override fun beforeCharDeleted(c: Char, file: PsiFile, editor: Editor) {}

    override fun charDeleted(c: Char, file: PsiFile, editor: Editor): Boolean {
        if (file.fileType != TableGenFileType.INSTANCE) return false

        if (c != '<') return false
        if (!CodeInsightSettings.getInstance().AUTOINSERT_PAIR_BRACKET) return false

        val offset = editor.caretModel.offset
        val text = editor.document.charsSequence
        if (offset >= text.length || text[offset] != '>') return false

        // Not a bracket if part of a string or comment.
        var iterator = editor.highlighter.createIterator(offset)
        if (iterator.tokenType != RANGLE) return false

        // Keep the '>' if it closes another '<' before the caret.
        val fileType = file.fileType
        val rAngleOffset = BraceMatchingUtil.findRightmostRParen(iterator, RANGLE, text, fileType)
        if (rAngleOffset >= 0) {
            iterator = editor.highlighter.createIterator(rAngleOffset)
            if (BraceMatchingUtil.matchBrace(text, fileType, iterator, /*forward=*/false, /*isStrict=*/true)) return false
        }

        editor.document.deleteString(offset, offset + 1)
        return true
    }
}
