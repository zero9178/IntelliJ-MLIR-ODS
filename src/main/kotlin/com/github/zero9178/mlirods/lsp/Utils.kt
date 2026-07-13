package com.github.zero9178.mlirods.lsp

import com.intellij.openapi.util.io.FileUtilRt
import com.intellij.openapi.vfs.VirtualFile

/**
 * Returns true if this virtual file refers to a TableGen file.
 */
val VirtualFile.isTableGenFile: Boolean
    get() = extension == "td"

/**
 * Returns true if a file called this is a TableGen file. Unlike [isTableGenFile] this does not require the file to
 * exist, which is what a VFS event about a file being created or renamed reports.
 */
val String.isTableGenFileName: Boolean
    get() = FileUtilRt.getExtension(this) == "td"