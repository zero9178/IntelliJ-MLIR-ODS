package com.github.zero9178.mlirods

import com.github.zero9178.mlirods.model.CompilationCommandsState
import com.github.zero9178.mlirods.model.IncludePaths
import com.github.zero9178.mlirods.model.TableGenCompilationCommandsProvider
import com.github.zero9178.mlirods.model.getCompilationCommandsEP
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.ExtensionTestUtil
import com.intellij.testFramework.IndexingTestUtil
import com.intellij.testFramework.UsefulTestCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * Testing utility which installs the include paths given by [map] into [project].
 * Include file related functionality is guaranteed to work afterwards.
 */
fun UsefulTestCase.installCompileCommands(
    project: Project, map: Map<VirtualFile, IncludePaths>
) {
    assert(map.isNotEmpty())

    ExtensionTestUtil.maskExtensions(
        getCompilationCommandsEP(),
        listOf(object : TableGenCompilationCommandsProvider {
            override fun getCompilationCommandsFlow(project: Project): Flow<CompilationCommandsState> {
                return flowOf(
                    CompilationCommandsState(
                        map
                    )
                )
            }
        }),
        testRootDisposable,
        fireEvents = true
    )

    IndexingTestUtil.waitUntilIndexesAreReady(project)
}