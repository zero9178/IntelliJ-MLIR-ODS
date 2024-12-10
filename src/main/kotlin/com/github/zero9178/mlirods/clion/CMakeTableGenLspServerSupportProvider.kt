package com.github.zero9178.mlirods.clion

import com.github.zero9178.mlirods.lsp.TableGenLspServerDescriptor
import com.github.zero9178.mlirods.lsp.TableGenLspServerSupportProviderInterface
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.lsp.api.LspServerSupportProvider
import com.jetbrains.cidr.cpp.cmake.workspace.CMakeWorkspace

internal class CMakeTableGenLspServerSupportProvider : TableGenLspServerSupportProviderInterface {
    override fun fileOpened(
        project: Project,
        file: VirtualFile,
        serverStarter: LspServerSupportProvider.LspServerStarter
    ): Boolean {
        val target = project.service<CMakeWorkspace>().modelTargets.firstOrNull {
            it.isExecutable && it.name == "tblgen-lsp-server"
        } ?: return false

        // TODO: Should this switch and use the build config that is currently active?
        //       Or should it have a heuristic (e.g. use the release). Users may not want to
        //       use a debug build e.g.
        val buildConfig = target.buildConfigurations.firstOrNull() ?: return false
        val productFile = buildConfig.productFile ?: return false

        // TODO: Check whether the LSP has been built and notify the user otherwise.

        val executable = LocalFileSystem.getInstance().findFileByIoFile(productFile) ?: return false

        /// TODO: This assumes layout as in the LLVM monorepo. No clue whether this holds!
        val compileCommands = LocalFileSystem.getInstance()
            .findFileByIoFile(buildConfig.configurationGenerationDir.resolve("tablegen_compile_commands.yml"))
            ?: return false

        serverStarter.ensureServerStarted(
            TableGenLspServerDescriptor(
                executable,
                compileCommands,
                project
            )
        )
        return true
    }
}
