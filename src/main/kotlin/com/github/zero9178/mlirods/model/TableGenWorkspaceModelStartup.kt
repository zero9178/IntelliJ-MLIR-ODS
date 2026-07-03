package com.github.zero9178.mlirods.model

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity

/**
 * Eagerly instantiates [TableGenWorkspaceModelService] on project open so it starts tracking the compilation commands
 * and contributing content roots without waiting for a TableGen file to be opened first.
 */
internal class TableGenWorkspaceModelStartup : ProjectActivity {
    override suspend fun execute(project: Project) {
        // Light test projects share a single project across tests and forbid module additions, which the service would
        // attempt as soon as any test installs compilation commands. Tests that exercise the service (see
        // WorkspaceModelTest) start it explicitly instead.
        if (ApplicationManager.getApplication().isUnitTestMode) return

        project.service<TableGenWorkspaceModelService>()
    }
}
