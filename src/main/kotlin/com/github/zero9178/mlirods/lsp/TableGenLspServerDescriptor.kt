package com.github.zero9178.mlirods.lsp

import com.github.zero9178.mlirods.service.PluginLifetimeService
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.AsyncFileListener
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.events.VFileContentChangeEvent
import com.intellij.openapi.vfs.newvfs.events.VFileCreateEvent
import com.intellij.openapi.vfs.newvfs.events.VFileDeleteEvent
import com.intellij.platform.lsp.api.LspServerListener
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
) : ProjectWideLspServerDescriptor(project, "TableGen"), LspServerListener, Disposable {

    // Paths in canonical form for comparison and command line creation.
    private val executablePath = executable.path
    private val compileCommandsPath = compileCommands.path

    override fun createCommandLine() = GeneralCommandLine().withExePath(executablePath)
        .withParameters("--tablegen-compilation-database=${compileCommandsPath}")

    init {
        // Dispose the descriptor and its children when the plugin is unloaded.
        Disposer.register(project.service<PluginLifetimeService>(), this)

        VirtualFileManager.getInstance().addAsyncFileListener(AsyncFileListener { events ->
            var lspChanged = false
            var executableExists = true
            var compileCommandsExist = true

            // Go over all events and figure out whether the server and compile commands have either changed
            // or exist again.
            for (event in events) {
                val executableAffected = event.path == executablePath
                val compileCommandsAffected = event.path == compileCommandsPath
                if (!executableAffected && !compileCommandsAffected) {
                    continue
                }

                when (event) {
                    is VFileCreateEvent -> {
                        lspChanged = true
                        if (executableAffected) {
                            executableExists = true
                        }
                        if (compileCommandsAffected) {
                            compileCommandsExist = true
                        }
                        continue
                    }

                    is VFileDeleteEvent -> {
                        lspChanged = true
                        if (executableAffected) {
                            executableExists = false
                        }
                        if (compileCommandsAffected) {
                            compileCommandsExist = false
                        }
                    }

                    is VFileContentChangeEvent -> {
                        lspChanged = true
                    }
                }
            }

            // No changes to LSP files.
            if (!lspChanged) return@AsyncFileListener null

            // If one of the two does not exist, do not restart the LSP as it will fail.
            // The LSP should be available as long as possible.
            if (!executableExists || !compileCommandsExist) return@AsyncFileListener null

            thisLogger().info("Restarting TableGen LSP due to file changes")
            return@AsyncFileListener object : AsyncFileListener.ChangeApplier {
                override fun afterVfsChange() {
                    restartTableGenLSPAsync(project)
                }
            }
        }, this)
    }

    override fun dispose() {}

    override fun isSupportedFile(file: VirtualFile) = file.extension == "td"

    override fun serverStopped(shutdownNormally: Boolean) {
        // Dispose the descriptor together with the server shutdown.
        Disposer.dispose(this)
    }

    /**
     * tblgen-lsp-server at the moment does not support 'codeAction'.
     * See https://github.com/llvm/llvm-project/blob/05b907f66b6aed06b8ad3b27883b9108a77858d2/mlir/lib/Tools/tblgen-lsp-server/LSPServer.cpp#L196
     */
    override val lspCodeActionsSupport: LspCodeActionsSupport?
        get() = null

    override val lspServerListener: LspServerListener
        get() = this
}

fun restartTableGenLSPAsync(project: Project) {
    LspServerManager.getInstance(project).stopAndRestartIfNeeded(TableGenLspServerSupportProvider::class.java)
}
