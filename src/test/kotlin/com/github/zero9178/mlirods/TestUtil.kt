package com.github.zero9178.mlirods

import com.github.zero9178.mlirods.model.*
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.ExtensionTestUtil
import com.intellij.testFramework.IndexingTestUtil
import com.intellij.testFramework.UsefulTestCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking

/**
 * Testing utility which installs the include paths given by [map] into [project].
 * Include file related functionality is guaranteed to work afterwards.
 */
fun UsefulTestCase.installCompileCommands(project: Project, map: Map<VirtualFile, IncludePaths>) {
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

    // Make sure service is initialized (its purposefully racy otherwise).
    runBlocking {
        project.service<TableGenIncludeGraph>().myBaseMapFlow.filter {
            it.isNotEmpty()
        }.firstOrNull()
    }
    IndexingTestUtil.waitUntilIndexesAreReady(project)
}