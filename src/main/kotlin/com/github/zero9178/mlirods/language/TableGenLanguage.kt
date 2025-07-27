package com.github.zero9178.mlirods.language

import com.github.zero9178.mlirods.MyIcons
import com.intellij.lang.Language
import com.intellij.openapi.fileTypes.LanguageFileType
import javax.swing.Icon

class TableGenLanguage private constructor() : Language("TableGen") {
    companion object {
        val INSTANCE = TableGenLanguage()
    }
}

class TableGenFileType : LanguageFileType(TableGenLanguage.INSTANCE) {

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
