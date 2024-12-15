package com.github.zero9178.mlirods.clion

import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.jetbrains.cidr.execution.build.CidrBuildEvent
import com.jetbrains.cidr.execution.build.CidrBuildListener
import com.jetbrains.cidr.execution.build.CidrBuildResult
import com.github.zero9178.mlirods.lsp.restartTableGenLSPAsync
import com.jetbrains.cidr.cpp.cmake.model.CMakeConfiguration
import com.jetbrains.cidr.execution.build.CidrBuildTaskType

/**
 * Checks whether 'tblgen-lsp-server' has been rebuilt and if yes, restarts the server.
 */
class CMakeTableGenBuildListener(private val project: Project) : CidrBuildListener {
    override fun afterFinished(buildEvent: CidrBuildEvent, result: CidrBuildResult) {
        if (!result.succeeded || buildEvent.taskType != CidrBuildTaskType.BUILD) return

        val cmakeConfiguration = buildEvent.buildConfiguration as? CMakeConfiguration ?: return
        if (!cmakeConfiguration.target.isTableGenLspServer) return

        thisLogger().info("Restarting LSP due to rebuild")
        restartTableGenLSPAsync(project)
    }
}
