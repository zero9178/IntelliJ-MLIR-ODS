package com.github.zero9178.mlirods.clion

import com.github.zero9178.mlirods.lsp.LSPLocationProviderInterface
import com.intellij.openapi.project.Project
import com.jetbrains.cidr.project.workspace.CidrWorkspaceManager
import org.jetbrains.annotations.NonNls

class TestLSPLocationProvider : LSPLocationProviderInterface {
    override fun getLocation(project: Project): @NonNls String? {
        return CidrWorkspaceManager.getInstance(project).initializedWorkspaces.firstOrNull()?.clientKey
    }
}
