package com.github.zero9178.mlirods.model

import com.intellij.openapi.components.Service
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

private val EP_NAME =
    ExtensionPointName.create<CompilationCommandsProvider>("com.github.zero9178.mlirods.compilationCommandsProvider")

data class IncludePaths(val paths: List<String>)

data class CompilationCommandsState(val map: Map<String, IncludePaths> = emptyMap())

@Service(Service.Level.PROJECT)
class CompilationCommands(private val project: Project, private val cs: CoroutineScope) {

    private var myBackingFlowEmission: Job? = null
    private val myStateFlow = MutableStateFlow<CompilationCommandsState>(CompilationCommandsState())

    init {
        EP_NAME.addChangeListener(cs) {
            updateBackingFlow()
        }
        updateBackingFlow()
    }

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
