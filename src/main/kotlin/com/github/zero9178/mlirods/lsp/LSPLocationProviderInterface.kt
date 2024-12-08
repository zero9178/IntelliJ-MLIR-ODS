package com.github.zero9178.mlirods.lsp

import com.intellij.openapi.project.Project

interface LSPLocationProviderInterface {
    fun getLocation(project: Project): String?
}