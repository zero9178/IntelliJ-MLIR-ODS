package com.github.zero9178.mlirods.model

import com.intellij.openapi.components.Service
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.jetbrains.annotations.TestOnly
import java.nio.file.Path

private val EP_NAME =
    ExtensionPointName.create<TableGenCompilationCommandsProvider>("com.github.zero9178.mlirods.compilationCommandsProvider")

@TestOnly
fun getCompilationCommandsEP() = EP_NAME

data class IncludePaths(val paths: List<Path>)

data class CompilationCommandsState(val map: Map<VirtualFile, IncludePaths> = emptyMap())

/**
 * Service used to provide the active compilation commands for TableGen files.
 */
@Service(Service.Level.PROJECT)
class CompilationCommands(private val project: Project, private val cs: CoroutineScope) {

    private var myBackingFlowEmission: Job? = null
    private val myStateFlow = MutableStateFlow(CompilationCommandsState())

    /**
     * Flow containing yielding the currently active compilation commands when changed.
     */
    val stateFlow: StateFlow<CompilationCommandsState>
        get() = myStateFlow

    init {
        EP_NAME.addChangeListener(cs) {
            updateBackingFlow()
        }
        updateBackingFlow()
    }

    /**
     * Reroutes the publicly exposed state flow such that it collects the state from the first non-throwing extension
     * point.
     */
    private fun updateBackingFlow() {
        myBackingFlowEmission?.cancel()
        myBackingFlowEmission = null

        val newFlow = EP_NAME.computeSafeIfAny {
            it.getCompilationCommandsFlow(project)
        } ?: return
        myBackingFlowEmission = cs.launch {
            newFlow.collect {
                myStateFlow.value = it
            }
        }
    }
}
