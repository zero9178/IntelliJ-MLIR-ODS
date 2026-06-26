package com.github.zero9178.mlirods

import com.github.zero9178.mlirods.model.*
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.ExtensionTestUtil
import com.intellij.testFramework.IndexingTestUtil
import com.intellij.testFramework.UsefulTestCase
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * Testing utility which installs the include paths given by [map] into [project].
 * Include-file-related functionality is guaranteed to work for any files given as keys of the map.
 */
fun UsefulTestCase.installCompileCommands(
    project: Project, map: Map<VirtualFile, IncludePaths>
) {
    assert(map.isNotEmpty())
    compileCommandsUpdater(project)(map)
}

/**
 * Installs a single mutable compile-commands provider into [project] and returns a function that applies a new
 * [CompilationCommandsState] and waits for it to finish being applied.
 *
 * Unlike [installCompileCommands], this masks the extension point only once and can therefore be used to apply several
 * states in sequence (the platform forbids re-masking an extension point).
 */
fun UsefulTestCase.compileCommandsUpdater(project: Project): (Map<VirtualFile, IncludePaths>) -> Unit {
    val flow = MutableStateFlow(CompilationCommandsState())
    ExtensionTestUtil.maskExtensions(
        getCompilationCommandsEP(),
        listOf(object : TableGenCompilationCommandsProvider {
            override fun getCompilationCommandsFlow(project: Project): Flow<CompilationCommandsState> = flow
        }),
        testRootDisposable,
        fireEvents = true
    )

    return { map ->
        val state = CompilationCommandsState(map)
        runBlocking {
            val job = launch(start = CoroutineStart.UNDISPATCHED) {
                project.service<TableGenContextService>().finishedCompileCommands.first { it == state }
            }
            flow.value = state
            job.join()
        }
        IndexingTestUtil.waitUntilIndexesAreReady(project)
    }
}