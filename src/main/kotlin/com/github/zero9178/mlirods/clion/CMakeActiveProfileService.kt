package com.github.zero9178.mlirods.clion

import com.github.zero9178.mlirods.lsp.restartTableGenLSPAsync
import com.intellij.execution.*
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.util.getValue
import com.intellij.util.setValue
import com.jetbrains.cidr.cpp.execution.CMakeBuildProfileExecutionTarget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicReference

private val LOG = logger<CMakeActiveProfileService>()

private class RunManagerInitializedListener(private val project: Project) : RunManagerListener {
    override fun stateLoaded(runManager: RunManager, isFirstLoadState: Boolean) {
        if (!isFirstLoadState) return

        project.service<CMakeActiveProfileService>().initFromExecutionManager()
    }
}

private class CMakeExecutionTargetListener(private val project: Project) : ExecutionTargetListener {
    override fun activeTargetChanged(newTarget: ExecutionTarget) {
        val target = newTarget as? CMakeBuildProfileExecutionTarget ?: return

        project.service<CMakeActiveProfileService>().profile = target.profileName
        LOG.info("Restarting LSP due to build profile change")
        restartTableGenLSPAsync(project)
    }
}

/**
 * Service used to be able to get the current cmake build profile at any point in time.
 */
@Service(Service.Level.PROJECT)
class CMakeActiveProfileService(private val project: Project, private val cs: CoroutineScope) {

    /**
     * Starts an asynchronous operation to initialize the profile from the currently active target.
     */
    internal fun initFromExecutionManager() = cs.launch {
        // TODO: Use serviceAsync once the API is public.
        val manager = project.service<ExecutionTargetManager>()
        val target = manager.activeTarget as? CMakeBuildProfileExecutionTarget ?: return@launch

        profile = target.profileName
        LOG.info("Restarting LSP due to run manager being initialized")
        restartTableGenLSPAsync(project)
    }

    /**
     * Returns the name of the current cmake build profile.
     */
    var profile: String by AtomicReference("")
        internal set
}
