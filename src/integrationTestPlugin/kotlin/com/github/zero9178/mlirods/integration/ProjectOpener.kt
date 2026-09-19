package com.github.zero9178.mlirods.integration

import com.intellij.ide.impl.ProjectUtil
import com.intellij.openapi.components.Service
import kotlin.io.path.Path

/**
 * Opens projects on behalf of the integration tests. An IDE without a user interface does not open the project given
 * on its command line.
 */
// Only ever called by the tests, from outside the IDE.
@Suppress("unused")
@Service(Service.Level.APP)
class ProjectOpener {

    /**
     * Opens the project at [path] and returns whether it succeeded in doing so.
     */
    fun open(path: String): Boolean = ProjectUtil.openOrImport(Path(path)) != null
}
