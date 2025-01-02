package com.github.zero9178.mlirods.language

import com.github.zero9178.mlirods.MyIcons
import com.intellij.extapi.psi.PsiFileBase
import com.intellij.lang.Language
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.fileTypes.LanguageFileType
import com.intellij.psi.FileViewProvider
import javax.swing.Icon

class TableGenLanguage private constructor() : Language("TableGen") {
    companion object {
        val INSTANCE = TableGenLanguage()
    }
}

class TableGenFile(viewProvider: FileViewProvider) : PsiFileBase(viewProvider, TableGenLanguage.INSTANCE) {
    override fun getFileType(): FileType = TableGenFileType.INSTANCE
}

internal class TableGenFileType : LanguageFileType(TableGenLanguage.INSTANCE) {

    @Suppress("CompanionObjectInExtension")
    companion object {
        @JvmField
        val INSTANCE = TableGenFileType()
    }

    override fun getName(): String {
        return "TableGen"
    }

    override fun getDescription(): String {
        return "LLVM TableGen langauge"
    }

    override fun getDefaultExtension(): String {
        return "td";
    }

    override fun getIcon(): Icon {
        return MyIcons.TableGenIcon
    }
}
