package com.github.zero9178.mlirods.language

import com.github.zero9178.mlirods.MyIcons
import com.intellij.lang.Language
import com.intellij.openapi.fileTypes.FileTypeRegistry
import com.intellij.openapi.fileTypes.LanguageFileType
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.concurrency.annotations.RequiresReadLock
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

/**
 * Returns true if this virtual file is a TableGen file, i.e. an existing file – never a directory – that the IDE
 * associates with [TableGenFileType].
 */
val VirtualFile.isTableGenFile: Boolean
    @RequiresReadLock
    get() = isValid && !isDirectory && FileTypeRegistry.getInstance().isFileOfType(this, TableGenFileType.INSTANCE)

/**
 * Returns true if a file called this is a TableGen file. Unlike [isTableGenFile] this does not require the file to
 * exist.
 */
val String.isTableGenFileName: Boolean
    get() = FileTypeRegistry.getInstance().getFileTypeByFileName(this) == TableGenFileType.INSTANCE
