package com.github.zero9178.mlirods.language.insertion

import com.github.zero9178.mlirods.language.STRING_LITERALS
import com.github.zero9178.mlirods.language.TableGenFileType
import com.github.zero9178.mlirods.language.generated.TableGenTypes.BLOCK_STRING_LITERAL
import com.intellij.codeInsight.editorActions.TypedHandlerDelegate
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.psi.util.elementType
import com.intellij.psi.util.startOffset

private class TableGenTypedHandlerDelegate : TypedHandlerDelegate() {
    override fun charTyped(
        c: Char, project: Project, editor: Editor, file: PsiFile
    ): Result {
        if (file.fileType != TableGenFileType.Companion.INSTANCE) return Result.CONTINUE

        if (c != '{') return Result.CONTINUE

        val offset = editor.caretModel.offset
        if (offset < 2) return Result.CONTINUE

        file.findElementAt(offset)?.let {
            // We may be in one of two states:
            // * After typing '{' the Psi has already been commited, and we see a block string literal that was just
            // started by the typed '{'. We can detect this using the start offset and should insert a matching '}'.
            // * It has not yet been committed. In that case we only want to insert a matching brace if we are not yet
            // in a string literal.
            if (STRING_LITERALS.contains(it.elementType))
                if (it.elementType != BLOCK_STRING_LITERAL || it.startOffset != offset - 2) return Result.CONTINUE
        }

        val doc = editor.document
        if (offset + 1 > doc.textLength) return Result.CONTINUE

        if (editor.document.charsSequence.subSequence(offset - 2, offset + 1) != "[{]") return Result.CONTINUE

        doc.insertString(offset, "}")

        return Result.CONTINUE
    }
}