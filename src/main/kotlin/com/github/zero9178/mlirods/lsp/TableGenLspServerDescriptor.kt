package com.github.zero9178.mlirods.lsp

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.OSProcessHandler
import com.intellij.execution.process.ProcessNotCreatedException
import com.intellij.openapi.components.serviceIfCreated
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.SystemInfoRt
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.lsp.api.LspServerListener
import com.intellij.platform.lsp.api.LspServerManager
import com.intellij.platform.lsp.api.ProjectWideLspServerDescriptor
import com.intellij.platform.lsp.api.customization.*
import com.intellij.util.io.BaseDataReader
import com.intellij.util.io.BaseOutputReader
import java.io.File
import java.io.IOException

/**
 * Interface supplementing [LspServerListener] to add more lifetime callbacks.
 */
interface LspLifetimeListener : LspServerListener {
    /**
     * Called if the Lsp server failed to start, most often due to IO errors.
     */
    fun serverFailedToStart() {}
}

/**
 * Descriptor used to start and identify the LSP.
 * [executable] should refer to "tblgen-lsp-server", while [compileCommands] should refer directly to the
 * "tablegen_compile_commands.yml" file.
 *
 * [listener] may be used to receive callbacks about the lifetime of the Lsp server created from the descriptor.
 * Note that if a Lsp server in the same project already exists, no second server is started and the listener will
 * never be called.
 */
class TableGenLspServerDescriptor(
    private val executable: File, private val compileCommands: File, project: Project,
    private val listener: LspLifetimeListener? = null
) : ProjectWideLspServerDescriptor(project, "TableGen") {

    companion object {
        private val LOGGER = logger<TableGenLspServerDescriptor>()
    }

    private val useTempExecutable: Boolean
        get() = SystemInfoRt.isWindows

    override fun createCommandLine() = GeneralCommandLine().withExePath(executable.absolutePath)
        .withParameters("--tablegen-compilation-database=${compileCommands.absolutePath}").apply {
            if (!useTempExecutable)
                return@apply

            // Windows locks files that are executing, making it impossible for us to rebuild the executable if
            // currently in use.
            // Work around this by making a temp of the executable and running that.
            // TODO: If any of the servers have DLL dependencies, this won't be good enough.
            val originalFile = File(exePath)
            val tempFile =
                FileUtil.createTempFile(originalFile.parentFile, ".${originalFile.name}", ".tmp")
            try {
                FileUtil.copy(originalFile, tempFile)
            } catch (e: IOException) {
                // Falling back to the original executable reintroduces exactly the file locking this copy exists to
                // avoid, so the user is about to find that rebuilding the server fails for no visible reason.
                LOGGER.warn(
                    "Failed to copy $originalFile to $tempFile; rebuilding the server while it runs will fail", e
                )
                return@apply
            }
            withExePath(tempFile.absolutePath)
        }

    private fun deleteExecutable(executable: String) {
        FileUtil.delete(File(executable))
    }

    override fun startServerProcess(): OSProcessHandler {
        try {
            val commandLine = createCommandLine().withCharset(Charsets.UTF_8)
            try {
                return object : OSProcessHandler(commandLine) {
                    override fun readerOptions(): BaseOutputReader.Options = object : BaseOutputReader.Options() {
                        override fun policy(): BaseDataReader.SleepingPolicy = forMostlySilentProcess().policy()

                        // Must not loose '\r' in "\r\n" line endings. They affect char count, which must match `Content-Length`
                        override fun splitToLines(): Boolean = false
                    }

                    override fun onOSProcessTerminated(exitCode: Int) {
                        // Delete the temporary executable once the process was terminated.
                        try {
                            super.onOSProcessTerminated(exitCode)
                        } finally {
                            if (useTempExecutable)
                                deleteExecutable(commandLine.exePath)
                        }
                    }
                }
            } catch (e: Throwable) {
                if (useTempExecutable)
                    deleteExecutable(commandLine.exePath)
                throw e
            }
        } catch (e: ProcessNotCreatedException) {
            // The banner the listener puts up tells the user to build the server but not why starting it failed, which
            // is the only place that answer exists.
            LOGGER.warn("Failed to start the TableGen LSP server", e)
            listener?.serverFailedToStart()
            // TODO: Ideally we could throw this exception to silently fail without logging, but this is currently not
            //       the case.
            throw ProcessCanceledException()
        }
    }

    override val lspServerListener: LspServerListener?
        get() = listener

    override fun isSupportedFile(file: VirtualFile) = file.extension == "td"

    override val lspCustomization: LspCustomization
        // We want to disable everything that is provided by the plugin as our implementation is hopefully higher
        // quality than what the LSP interface allows.
        get() = object : LspCustomization() {
            /**
             * tblgen-lsp-server at the moment does not support 'codeAction'.
             * See https://github.com/llvm/llvm-project/blob/05b907f66b6aed06b8ad3b27883b9108a77858d2/mlir/lib/Tools/tblgen-lsp-server/LSPServer.cpp#L196
             */
            override val codeActionsCustomizer: LspCodeActionsCustomizer
                get() = LspCodeActionsDisabled

            override val completionCustomizer: LspCompletionCustomizer
                get() = LspCompletionDisabled

            override val diagnosticsCustomizer: LspDiagnosticsCustomizer
                get() = LspDiagnosticsSupport()

            override val findReferencesCustomizer: LspFindReferencesCustomizer
                get() = LspFindReferencesDisabled

            override val goToDefinitionCustomizer: LspGoToDefinitionCustomizer
                get() = LspGoToDefinitionDisabled

            override val goToTypeDefinitionCustomizer: LspGoToTypeDefinitionCustomizer
                get() = LspGoToTypeDefinitionDisabled

            override val semanticTokensCustomizer: LspSemanticTokensCustomizer
                get() = LspSemanticTokensDisabled

            override val documentLinkCustomizer: LspDocumentLinkCustomizer
                get() = LspDocumentLinkDisabled
        }
}

fun restartTableGenLSPAsync(project: Project) {
    project.serviceIfCreated<LspServerManager>()?.stopAndRestartIfNeeded(TableGenLspServerSupportProvider::class.java)
}
