package com.github.zero9178.mlirods.clion

import com.github.zero9178.mlirods.lsp.restartTableGenLSPAsync
import com.intellij.execution.*
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.jetbrains.cidr.cpp.cmake.workspace.CMakeWorkspace
import com.jetbrains.cidr.cpp.cmake.workspace.CMakeWorkspaceListener
import com.jetbrains.cidr.cpp.execution.CMakeBuildProfileExecutionTarget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

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

        project.service<CMakeActiveProfileService>().profileName = target.profileName
        LOG.info("Restarting LSP due to build profile change")
    }
}

/**
 * Service used to be able to get the current cmake build profile at any point in time.
 */
@Service(Service.Level.PROJECT)
class CMakeActiveProfileService(private val project: Project, private val cs: CoroutineScope) {

    private val myProfileNameFlow = MutableStateFlow("")
    internal val myProfileListFlow = MutableStateFlow(
        project.service<CMakeWorkspace>().profileInfos.toList()
    )

    init {
        project.messageBus.connect(cs).subscribe(CMakeWorkspaceListener.TOPIC, object : CMakeWorkspaceListener {
            override fun generationFinished() {
                myProfileListFlow.value =
                    project.service<CMakeWorkspace>().profileInfos.toList()
            }
        })

        cs.launch {
            myProfileNameFlow.filter { !it.isEmpty() }.collect {
                restartTableGenLSPAsync(project)
            }
        }
    }

    /**
     * Starts an asynchronous operation to initialize the profile from the currently active target.
     */
    internal fun initFromExecutionManager() = cs.launch {
        // TODO: Use serviceAsync once the API is public.
        val manager = project.service<ExecutionTargetManager>()
        val target = manager.activeTarget as? CMakeBuildProfileExecutionTarget ?: return@launch

        profileName = target.profileName
        LOG.info("Restarting LSP due to run manager being initialized")
    }

    /**
     * Returns the name of the current cmake build profile.
     */
    var profileName: String
        internal set(value) {
            myProfileNameFlow.value = value
        }
        get() = myProfileNameFlow.value

    /**
     * A flow yielding new profile names whenever the active profile name has changed.
     */
    val profileNameFlow: StateFlow<String>
        get() = myProfileNameFlow

    /**
     * A flow yielding the active profile whenever the active profile has changed.
     */
    val profileFlow = profileNameFlow.combine(myProfileListFlow) { name, list ->
        list.find {
            it.profile.name == name
        }
    }
}
