package com.github.zero9178.mlirods.language

import com.github.zero9178.mlirods.language.generated.psi.TableGenBlockStringValue
import com.intellij.codeInsight.editorActions.TypedHandlerDelegate
import com.intellij.codeInsight.editorActions.enter.EnterHandlerDelegate
import com.intellij.injected.editor.EditorWindow
import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile

/**
 * Restores the indentation of a block string line before a character is typed into a language injected into it.
 *
 * The injection leaves out the indentation shared by all lines of the block string. Typing at the start of an empty
 * line would therefore insert text at the very start of the host line, making that line the least indented one and
 * shifting the whole injected fragment on the next injection. Indenting the line first keeps the fragment stable and
 * matches what typing in the block string outside of an injection would produce.
 */
internal class TableGenBlockStringTypedHandler : TypedHandlerDelegate() {
    override fun beforeCharTyped(c: Char, project: Project, editor: Editor, file: PsiFile, fileType: FileType): Result {
        indentInjectedLine(editor)
        return Result.CONTINUE
    }
}

/**
 * Indents the line created by pressing enter within a language injected into a block string. See
 * [TableGenBlockStringTypedHandler] for why this is needed.
 */
internal class TableGenBlockStringEnterHandler : EnterHandlerDelegate {
    override fun postProcessEnter(file: PsiFile, editor: Editor, dataContext: DataContext): EnterHandlerDelegate.Result {
        indentInjectedLine(editor)
        return EnterHandlerDelegate.Result.Continue
    }
}

/**
 * Indents the host line at the caret of [editor] to the indentation shared by the other non-blank lines of the block
 * string if [editor] is the editor of a language injected into a [TableGenBlockStringValue] and the line is indented
 * less. The platform hands the injected editor to typed and enter handlers whenever the caret is within an injection.
 */
private fun indentInjectedLine(editor: Editor) {
    val injectedEditor = editor as? EditorWindow ?: return
    val injectedFile = injectedEditor.injectedFile
    val host = InjectedLanguageManager.getInstance(injectedFile.project).getInjectionHost(injectedFile)
    if (host !is TableGenBlockStringValue) return

    // Work on the host document rather than the PSI: the document is up to date even while the PSI is not yet
    // committed, which is the case right after enter has been processed.
    val hostEditor = injectedEditor.delegate
    val hostDocument = hostEditor.document
    val hostText = hostDocument.charsSequence
    val hostRanges = injectedEditor.document.hostRanges
    fun lineText(line: Int) =
        hostText.subSequence(hostDocument.getLineStartOffset(line), hostDocument.getLineEndOffset(line))

    val caretOffset = hostEditor.caretModel.offset
    val caretLine = hostDocument.getLineNumber(caretOffset)
    val firstLine = hostDocument.getLineNumber(hostRanges.first().startOffset)
    val lastLine = hostDocument.getLineNumber(hostRanges.last().endOffset)
    // The caret line itself does not count: It is the line being fixed up and may currently be the least indented one.
    val indentation = commonIndentation((firstLine..lastLine).asSequence().filter { it != caretLine }.map(::lineText))
        ?: return
    val existing = lineText(caretLine).takeWhile(Char::isWhitespace)
    if (existing.length >= indentation.length) return

    val missing = indentation.subSequence(existing.length, indentation.length)
    hostDocument.insertString(hostDocument.getLineStartOffset(caretLine) + existing.length, missing)
    hostEditor.caretModel.moveToOffset(caretOffset + missing.length)
}
