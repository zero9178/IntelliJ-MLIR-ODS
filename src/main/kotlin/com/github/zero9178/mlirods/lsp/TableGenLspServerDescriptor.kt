package com.github.zero9178.mlirods.lsp

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.lsp.api.LspServerManager
import com.intellij.platform.lsp.api.ProjectWideLspServerDescriptor
import com.intellij.platform.lsp.api.customization.LspCodeActionsSupport

/**
 * Descriptor used to start and identify the LSP.
 * 'executable' should refer to "tblgen-lsp-server", while 'compileCommands' should refer directly to the
 * "tablegen_compile_commands.yml" file.
 *
 * The descriptor will automatically check for file changes of the compile commands and executable it was started with
 * and restart automatically if changed.
 */
class TableGenLspServerDescriptor(
    executable: VirtualFile, compileCommands: VirtualFile, project: Project
) : ProjectWideLspServerDescriptor(project, "TableGen") {

    // Paths in canonical form for comparison and command line creation.
    private val executablePath = executable.path
    private val compileCommandsPath = compileCommands.path

    override fun createCommandLine() = GeneralCommandLine().withExePath(executablePath)
        .withParameters("--tablegen-compilation-database=${compileCommandsPath}")

    override fun isSupportedFile(file: VirtualFile) = file.extension == "td"

    /**
     * tblgen-lsp-server at the moment does not support 'codeAction'.
     * See https://github.com/llvm/llvm-project/blob/05b907f66b6aed06b8ad3b27883b9108a77858d2/mlir/lib/Tools/tblgen-lsp-server/LSPServer.cpp#L196
     */
    override val lspCodeActionsSupport: LspCodeActionsSupport?
        get() = null
}

fun restartTableGenLSPAsync(project: Project) {
    LspServerManager.getInstance(project).stopAndRestartIfNeeded(TableGenLspServerSupportProvider::class.java)
}
