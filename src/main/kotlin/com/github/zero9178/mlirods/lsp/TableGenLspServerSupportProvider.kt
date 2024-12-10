package com.github.zero9178.mlirods.lsp

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
        if (file.extension != "td") return

        EP_NAME.findFirstSafe {
            it.fileOpened(project, file, serverStarter)
        }
    }
}

private val EP_NAME =
    ExtensionPointName.create<TableGenLspServerSupportProviderInterface>("com.github.zero9178.mlirods.tableGenServerSupportProvider")