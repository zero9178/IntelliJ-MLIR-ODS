package com.github.zero9178.mlirods.lsp

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.extensions.ExtensionPointName
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

    companion object {
        private val EP_NAME =
            ExtensionPointName.create<LSPLocationProviderInterface>("com.github.zero9178.mlirods.lspLocationProvider")
    }

    override fun createCommandLine(): GeneralCommandLine {
        val loc = EP_NAME.computeSafeIfAny {
            it.getLocation(project)
        }
        thisLogger().warn(loc.toString())
        TODO("Start LSP server with TableGen commands file")
    }

    override fun isSupportedFile(file: VirtualFile) = file.extension == "td"
}