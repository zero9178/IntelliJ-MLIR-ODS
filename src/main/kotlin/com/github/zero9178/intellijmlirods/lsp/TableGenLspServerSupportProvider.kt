package com.github.zero9178.intellijmlirods.lsp

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.lsp.api.LspServerSupportProvider
import com.intellij.platform.lsp.api.ProjectWideLspServerDescriptor

internal class TableGenLspServerSupportProvider : LspServerSupportProvider {
    override fun fileOpened(
        project: Project,
        file: VirtualFile,
        serverStarter: LspServerSupportProvider.LspServerStarter
    ) {
        if (file.extension != "td") return

        serverStarter.ensureServerStarted(TableGenLspServerDescriptor(project))
    }
}

private class TableGenLspServerDescriptor(project: Project) : ProjectWideLspServerDescriptor(project, "TableGen") {
    override fun createCommandLine(): GeneralCommandLine {
        TODO("Start LSP server with TableGen commands file")
    }

    override fun isSupportedFile(file: VirtualFile) = file.extension == "td"
}