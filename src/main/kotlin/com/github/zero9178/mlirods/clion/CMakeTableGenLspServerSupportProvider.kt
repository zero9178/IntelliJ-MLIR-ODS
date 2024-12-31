package com.github.zero9178.mlirods.clion

import com.github.zero9178.mlirods.lsp.LspLifetimeListener
import com.github.zero9178.mlirods.lsp.TableGenLspServerDescriptor
import com.github.zero9178.mlirods.lsp.TableGenLspServerSupportProviderInterface
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.lsp.api.LspServerSupportProvider
import com.jetbrains.cidr.cpp.cmake.model.CMakeTarget
import com.jetbrains.cidr.cpp.cmake.workspace.CMakeWorkspace
import org.eclipse.lsp4j.InitializeResult

internal class CMakeTableGenLspServerSupportProvider : TableGenLspServerSupportProviderInterface {
    override fun fileOpened(
        project: Project,
        file: VirtualFile,
        serverStarter: LspServerSupportProvider.LspServerStarter
    ): Boolean {
        val target =
            project.service<CMakeWorkspace>().modelTargets.firstOrNull(CMakeTarget::isTableGenLspServer)
        if (target == null) {
            thisLogger().info("Project has no 'tblgen-lsp-server' cmake target")
            return false
        }

        val activeConfig = project.service<CMakeActiveProfileService>().profile
        val buildConfig = target.buildConfigurations.find {
            it.name == activeConfig
        }
        if (buildConfig == null) {
            thisLogger().info("'tblgen-lsp-server' has no build configuration called '$activeConfig'")
            return false
        }

        val productFile = buildConfig.productFile ?: return false

        // TODO: Check whether the LSP has been built and notify the user otherwise.

        /// TODO: This assumes layout as in the LLVM monorepo. No clue whether this holds!
        val compileCommands = buildConfig.configurationGenerationDir.resolve("tablegen_compile_commands.yml")
        serverStarter.ensureServerStarted(
            TableGenLspServerDescriptor(
                productFile,
                compileCommands,
                project,
                object : LspLifetimeListener {
                    override fun serverInitialized(params: InitializeResult) {
                        project.service<CMakeTableGenBuildNotificationProviderService>()
                            .clearNotifications()
                    }

                    override fun serverFailedToStart() {
                        project.service<CMakeTableGenBuildNotificationProviderService>()
                            .showBuildNotification(buildConfig)
                    }
                }
            )
        )
        return true
    }
}
