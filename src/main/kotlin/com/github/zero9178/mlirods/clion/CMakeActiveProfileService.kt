package com.github.zero9178.mlirods.clion

import com.github.zero9178.mlirods.lsp.restartTableGenLSPAsync
import com.intellij.execution.ExecutionTargetListener
import com.intellij.execution.ExecutionTargetManager
import com.intellij.execution.RunManager
import com.intellij.execution.RunManagerListener
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.progress.runBlockingCancellable
import com.intellij.openapi.project.Project
import com.intellij.util.getValue
import com.intellij.util.setValue
import com.jetbrains.cidr.cpp.execution.CMakeBuildProfileExecutionTarget
import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicReference

/**
 * Service used to be able to get the current cmake build profile at any point in time.
 */
@Service(Service.Level.PROJECT)
class CMakeActiveProfileService(project: Project, cs: CoroutineScope) {

    private var myProfile: Deferred<String> by AtomicReference(cs.async {
        val connection = project.messageBus.connect(this)
        try {
            suspendCancellableCoroutine { cont ->
                connection.subscribe(RunManagerListener.TOPIC, object : RunManagerListener {
                    override fun stateLoaded(runManager: RunManager, isFirstLoadState: Boolean) {
                        cont.resumeWith(Result.success(Unit))
                    }
                })
            }
        } finally {
            connection.disconnect()
        }

        val manager = project.service<ExecutionTargetManager>()
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
