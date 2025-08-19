package com.github.zero9178.mlirods

import com.github.zero9178.mlirods.language.psi.TableGenFile
import com.github.zero9178.mlirods.model.CompilationCommandsState
import com.github.zero9178.mlirods.model.IncludePaths
import com.github.zero9178.mlirods.model.TableGenCompilationCommandsProvider
import com.github.zero9178.mlirods.model.getCompilationCommandsEP
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.testFramework.ExtensionTestUtil
import com.intellij.testFramework.IndexingTestUtil
import com.intellij.testFramework.PlatformTestUtil
import com.intellij.testFramework.UsefulTestCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * Testing utility which installs the include paths given by [map] into [project].
 * Include-file-related functionality is guaranteed to work for any files given as keys of the map.
 * Additional files whose context should be resolved through propagation should be given in [additionalPropagation].
 */
fun UsefulTestCase.installCompileCommands(
    project: Project, map: Map<VirtualFile, IncludePaths>,
    additionalPropagation: List<Pair<VirtualFile, IncludePaths>> = emptyList()
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

    val list = map.toList() + additionalPropagation
    PlatformTestUtil.waitWhileBusy {
        list.any {
            val file =
                PsiManager.getInstance(project).findFile(it.first) as? TableGenFile
            if (file == null)
                true
            else file.context.includePaths != it.second.paths
        }
    }

    IndexingTestUtil.waitUntilIndexesAreReady(project)
}