package com.github.zero9178.mlirods.model

import com.intellij.openapi.project.Project
import kotlinx.coroutines.flow.Flow

/**
 * Interface used to provide compilation commands of TableGen files.
 */
interface TableGenCompilationCommandsProvider {
    /**
     * Returns a flow that should yield compilation commands for TableGen files.
     * Each time the active compilation commands changes a new item should be emitted.
     */
    fun getCompilationCommandsFlow(project: Project): Flow<CompilationCommandsState>
}