package com.github.zero9178.mlirods.language.format

import com.github.zero9178.mlirods.language.TableGenFileType
import com.intellij.psi.PsiFile
import com.intellij.psi.codeStyle.CommonCodeStyleSettings
import com.intellij.psi.codeStyle.FileTypeIndentOptionsProvider

private class TableGenFileTypeIndentOptionsProvider : FileTypeIndentOptionsProvider {
    override fun createIndentOptions() = CommonCodeStyleSettings.IndentOptions().apply {
        INDENT_SIZE = 2
        CONTINUATION_INDENT_SIZE = 4
        TAB_SIZE = 2
    }

    override fun getFileType() = TableGenFileType.INSTANCE

    override fun getPreviewText() = ""

    override fun prepareForReformat(psiFile: PsiFile?) {}
}