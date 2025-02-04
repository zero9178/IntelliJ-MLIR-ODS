package com.github.zero9178.mlirods.lsp

import com.github.zero9178.mlirods.model.CompilationCommands
import com.intellij.openapi.components.service
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.lsp.api.LspServerSupportProvider

internal class TableGenLspServerSupportProvider : LspServerSupportProvider {

    override fun fileOpened(
        project: Project,
        file: VirtualFile,
        serverStarter: LspServerSupportProvider.LspServerStarter
    ) {
        if (!file.isTableGenFile) return


        // TODO: For testing only.
        project.service<CompilationCommands>()

        EP_NAME.findFirstSafe {
            it.fileOpened(project, file, serverStarter)
        }
    }
}

private val EP_NAME =
    ExtensionPointName.create<TableGenLspServerSupportProviderInterface>("com.github.zero9178.mlirods.tableGenServerSupportProvider")