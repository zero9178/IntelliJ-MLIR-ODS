package com.github.zero9178.mlirods.clion

import com.jetbrains.cidr.cpp.cmake.model.CMakeTarget

/**
 * Returns true if this cmake target refers to 'tblgen-lsp-server'.
 */
val CMakeTarget.isTableGenLspServer: Boolean
    get() = isExecutable && name == "tblgen-lsp-server"