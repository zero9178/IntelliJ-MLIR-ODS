package com.github.zero9178.mlirods.lsp

import com.intellij.openapi.vfs.VirtualFile

/**
 * Returns true if this virtual file refers to a TableGen file.
 */
val VirtualFile.isTableGenFile: Boolean
    get() = extension == "td"