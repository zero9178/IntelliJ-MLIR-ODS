package com.github.zero9178.mlirods.clion

import com.github.zero9178.mlirods.lsp.restartTableGenLSPAsync
import com.intellij.execution.ExecutionTargetListener
import com.intellij.execution.ExecutionTargetManager
import com.intellij.execution.RunManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.serviceAsync
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.progress.runBlockingCancellable
import com.intellij.openapi.project.Project
import com.intellij.util.getValue
import com.intellij.util.setValue
import com.jetbrains.cidr.cpp.execution.CMakeBuildProfileExecutionTarget
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import java.util.concurrent.atomic.AtomicReference

/**
 * Service used to be able to get the current cmake build profile at any point in time.
 */
@Service(Service.Level.PROJECT)
class CMakeActiveProfileService(project: Project, cs: CoroutineScope) {

    private var myProfile: Deferred<String> by AtomicReference(cs.async {
        // Make sure the run manager is ready. This avoids a warning in the log that it is being created before
        // initialized.
        project.serviceAsync<RunManager>()
        val manager = project.serviceAsync<ExecutionTargetManager>()
        val target = manager.activeTarget
        (target as? CMakeBuildProfileExecutionTarget)?.profileName ?: ""
    })

    /**
     * Returns the name of the current cmake build profile.
     * This operation may block for a short time during project opening, but returns quickly otherwise.
     */
    fun fetchProfile() = runBlockingCancellable { myProfile.await() }

    init {
        project.messageBus.connect(cs).subscribe(
            ExecutionTargetManager.TOPIC, ExecutionTargetListener { newTarget ->
                val target = newTarget as? CMakeBuildProfileExecutionTarget ?: return@ExecutionTargetListener

                myProfile = CompletableDeferred(target.profileName)
                thisLogger().info("Restarting LSP due to build profile change")
                restartTableGenLSPAsync(project)
            })
    }
}
