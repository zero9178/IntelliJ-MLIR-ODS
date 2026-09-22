package com.github.zero9178.mlirods.language.insertion

import com.github.zero9178.mlirods.language.STRING_LITERALS
import com.github.zero9178.mlirods.language.TableGenFileType
import com.github.zero9178.mlirods.language.generated.TableGenTypes.BLOCK_STRING_LITERAL
import com.github.zero9178.mlirods.language.generated.TableGenTypes.LANGLE
import com.intellij.codeInsight.AutoPopupController
import com.intellij.codeInsight.CodeInsightSettings
import com.intellij.codeInsight.editorActions.TabOutScopesTracker
import com.intellij.codeInsight.editorActions.TypedHandler
import com.intellij.codeInsight.editorActions.TypedHandlerDelegate
import com.intellij.codeInsight.highlighting.BraceMatchingUtil
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.psi.util.elementType
import com.intellij.psi.util.startOffset

internal class TableGenTypedHandlerDelegate : TypedHandlerDelegate() {
    override fun checkAutoPopup(
        charTyped: Char, project: Project, editor: Editor, file: PsiFile
    ): Result {
        if (file.fileType != TableGenFileType.INSTANCE) return Result.CONTINUE

        if (charTyped != '!') return Result.CONTINUE

        // A '!' starts a bang operator, but as it is not an identifier character the platform would not consider it
        // worth showing completion for on its own.
        AutoPopupController.getInstance(project).scheduleAutoPopup(editor)
        return Result.STOP
    }

    override fun beforeCharTyped(
        c: Char, project: Project, editor: Editor, file: PsiFile, fileType: FileType
    ): Result {
        if (file.fileType != TableGenFileType.INSTANCE) return Result.CONTINUE

        if (c != '>') return Result.CONTINUE

        // The platform steps over the closing bracket of the '(', '[' and '{' pairs it inserts itself, but knows
        // nothing about '<'. Its logic is generic over the brace matcher though, which pairs '<' with '>'.
        return if (TypedHandler.handleRParen(editor, fileType, c)) Result.STOP else Result.CONTINUE
    }

    override fun charTyped(
        c: Char, project: Project, editor: Editor, file: PsiFile
    ): Result {
        if (file.fileType != TableGenFileType.INSTANCE) return Result.CONTINUE

        when (c) {
            '{' -> closeBlockStringLiteral(editor, file)
            '<' -> insertMatchingRAngle(editor, file.fileType)
        }
        return Result.CONTINUE
    }

    /**
     * Inserts a '>' after a '<' just typed at the caret, unless a '>' following the caret already closes it.
     *
     * The platform only pairs '(', '[' and '{', so this replicates its behaviour for '<'.
     */
    private fun insertMatchingRAngle(editor: Editor, fileType: FileType) {
        if (!CodeInsightSettings.getInstance().AUTOINSERT_PAIR_BRACKET) return

        val offset = editor.caretModel.offset
        val document = editor.document
        val text = document.charsSequence

        // Position the iterator on the typed '<'. It is not a bracket if it is part of a string or comment.
        var iterator = editor.highlighter.createIterator(offset)
        if (offset != document.textLength) iterator.retreat()
        if (iterator.atEnd() || iterator.tokenType != LANGLE) return

        // Like the platform, leave the brackets alone if the leftmost unclosed '<' before the caret is balanced by
        // the '>'s following it.
        val lAngleOffset = BraceMatchingUtil.findLeftmostLParen(iterator, LANGLE, text, fileType).coerceAtLeast(0)
        iterator = editor.highlighter.createIterator(lAngleOffset)
        if (BraceMatchingUtil.matchBrace(text, fileType, iterator, /*forward=*/true, /*isStrict=*/true)) return

        document.insertString(offset, ">")
        TabOutScopesTracker.getInstance().registerEmptyScope(editor, offset)
    }

    private fun closeBlockStringLiteral(editor: Editor, file: PsiFile) {
        val offset = editor.caretModel.offset
        if (offset < 2) return

        file.findElementAt(offset)?.let {
            // We may be in one of two states:
            // * After typing '{' the Psi has already been commited, and we see a block string literal that was just
            // started by the typed '{'. We can detect this using the start offset and should insert a matching '}'.
            // * It has not yet been committed. In that case we only want to insert a matching brace if we are not yet
            // in a string literal.
            if (STRING_LITERALS.contains(it.elementType))
                if (it.elementType != BLOCK_STRING_LITERAL || it.startOffset != offset - 2) return
        }

        val doc = editor.document
        if (offset + 1 > doc.textLength) return

        if (editor.document.charsSequence.subSequence(offset - 2, offset + 1) != "[{]") return

        doc.insertString(offset, "}")
    }
}
