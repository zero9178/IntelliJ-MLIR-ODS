package com.github.zero9178.mlirods

import com.github.zero9178.mlirods.model.*
import com.intellij.openapi.application.impl.TestOnlyThreading
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.ExtensionTestUtil
import com.intellij.testFramework.IndexingTestUtil
import com.intellij.testFramework.UsefulTestCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration.Companion.seconds

/**
 * Timeout applied to every wait for the include graph to settle. Generous, as it also covers the file indexing a graph
 * update triggers.
 */
private val TIMEOUT = 60.seconds

/**
 * Runs [action], which blocks the current thread until the include graph has settled.
 *
 * Tests run on the EDT with the write-intent lock held, while the graph applies its changes in background write
 * actions. Blocking the EDT without giving up that lock would therefore deadlock the graph update we are waiting for.
 */
private fun awaitOffEdt(action: suspend CoroutineScope.() -> Unit) =
    TestOnlyThreading.releaseTheAcquiredWriteIntentLockThenExecuteActionAndTakeWriteIntentLockBack {
        runBlocking {
            withTimeout(TIMEOUT) {
                action()
            }
        }
    }

/**
 * Waits until every graph update triggered so far has been applied.
 *
 * Updates are accounted for in [TableGenIncludeGraphService.updatesInFlight] the moment they are triggered, so this is
 * guaranteed to wait for e.g. the update caused by a preceding file modification.
 */
fun awaitIncludeGraph(project: Project) {
    val graphService = project.service<TableGenIncludeGraphService>()
    awaitOffEdt {
        graphService.updatesInFlight.first { it == 0 }
    }
    IndexingTestUtil.waitUntilIndexesAreReady(project)
}

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
 * [CompilationCommandsState] and waits for it, and any other update in flight, to finish being applied.
 *
 * Unlike [installCompileCommands], this masks the extension point only once and can therefore be used to apply several
 * states in sequence (the platform forbids re-masking an extension point). Applying the same state again is a valid way
 * of waiting for the graph to have caught up with changes made to files in the meantime.
 */
fun UsefulTestCase.compileCommandsUpdater(project: Project): (Map<VirtualFile, IncludePaths>) -> Unit {
    val flow = MutableSharedFlow<CompilationCommandsState>()
    ExtensionTestUtil.maskExtensions(
        getCompilationCommandsEP(),
        listOf(object : TableGenCompilationCommandsProvider {
            override fun getCompilationCommandsFlow(project: Project): Flow<CompilationCommandsState> = flow
        }),
        testRootDisposable,
        fireEvents = true
    )

    // Instantiate the graph service up front so it subscribes to the compile-commands flow before we push a new state,
    // and so it processes it and reports completion. Include resolution and context now flow through the include graph.
    val graphService = project.service<TableGenIncludeGraphService>()

    return { map ->
        val state = CompilationCommandsState(map)
        awaitOffEdt {
            // Subscribe before emitting, as the graph only reports a state once it has been applied.
            val graphJob = launch(start = CoroutineStart.UNDISPATCHED) {
                graphService.finishedCompileCommands.first { it == state }
            }
            flow.emit(state)
            graphJob.join()
        }
        // The new state having been applied does not imply updates triggered by something else (e.g. a file having been
        // modified) have been as well.
        awaitIncludeGraph(project)
    }
}
