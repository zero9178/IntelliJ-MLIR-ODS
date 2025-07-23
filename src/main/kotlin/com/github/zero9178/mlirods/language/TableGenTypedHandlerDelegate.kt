package com.github.zero9178.mlirods.language

import com.intellij.codeInsight.editorActions.TypedHandlerDelegate
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile

private class TableGenTypedHandlerDelegate : TypedHandlerDelegate() {
    override fun charTyped(
        c: Char,
        project: Project,
        editor: Editor,
        file: PsiFile
    ): Result {
        if (file.fileType != TableGenFileType.INSTANCE)
            return Result.CONTINUE

        if (c != '{')
            return Result.CONTINUE

        val offset = editor.caretModel.offset
        if (offset < 2)
            return Result.CONTINUE

        val doc = editor.document
        if (offset + 1 > doc.textLength)
            return Result.CONTINUE

        if (editor.document.charsSequence.subSequence(offset - 2, offset + 1) != "[{]")
            return Result.CONTINUE

        PsiDocumentManager.getInstance(project).commitDocument(doc)
        doc.insertString(offset, "}")

        return Result.CONTINUE
    }
}