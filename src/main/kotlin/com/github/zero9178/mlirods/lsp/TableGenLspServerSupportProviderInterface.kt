package com.github.zero9178.mlirods.lsp

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.lsp.api.LspServerSupportProvider

/**
 * Interface similar to [LspServerSupportProvider] but only for TableGen files.
 * This is meant to select one of multiple providers that are able to start an LSP server.
 */
interface TableGenLspServerSupportProviderInterface {

    /**
     * Called when an editor is opened for [file] within [project].
     * [file] is guaranteed to be a TableGen file.
     * [serverStarter] may be used to immediately start an LSP server.
     * Longer running startup-tasks (such as downloading the server),
     * may also start the download process and later use the
     * [com.intellij.platform.lsp.api.LspServerManager] to start the LSP.
     *
     * See also [TableGenLspServerDescriptor] for a descriptor to use.
     *
     * Returns true if the implementation takes responsibility of starting the
     * server, false otherwise.
     * If responsibility is not taken, the next interface implementation in the extension
     * point is queried.
     */
    fun fileOpened(
        project: Project, file: VirtualFile, serverStarter: LspServerSupportProvider.LspServerStarter
    ): Boolean
}