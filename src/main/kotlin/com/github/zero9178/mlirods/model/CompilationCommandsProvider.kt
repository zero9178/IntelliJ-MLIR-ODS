package com.github.zero9178.mlirods.model

import com.intellij.openapi.project.Project
import kotlinx.coroutines.flow.Flow

interface CompilationCommandsProvider {
    fun getCompilationCommandsFlow(project: Project): Flow<CompilationCommandsState>
}