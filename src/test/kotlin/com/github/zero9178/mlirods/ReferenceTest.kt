package com.github.zero9178.mlirods

import com.github.zero9178.mlirods.model.*
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.psi.PsiFile
import com.intellij.testFramework.ExtensionTestUtil
import com.intellij.testFramework.IndexingTestUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlin.io.path.Path
import kotlin.io.path.absolute

class ReferenceTest : BasePlatformTestCase() {
    fun `test include`() {
        val reference =
            myFixture.getReferenceAtCaretPositionWithAssertion("ReferenceTestData.td")
        val virtualFile = myFixture.copyFileToProject("HasCompileCommands.td")

        ExtensionTestUtil.maskExtensions(
            getCompilationCommandsEP(),
            listOf(object : TableGenCompilationCommandsProvider {
                override fun getCompilationCommandsFlow(project: Project): Flow<CompilationCommandsState> {
                    return flowOf(
                        CompilationCommandsState(
                            mapOf(
                                virtualFile to IncludePaths(listOf(Path("$testDataPath").absolute()))
                            )
                        )
                    )
                }
            }),
            disposeOnTearDown(Disposer.newDisposable()),
            fireEvents = true
        )

        // Make sure service is initialized (its purposefully racy otherwise).
        runBlocking {
            project.service<TableGenIncludeGraph>().myBaseMapFlow.filter {
                it.isNotEmpty()
            }.firstOrNull()
        }
        IndexingTestUtil.waitUntilIndexesAreReady(project)

        val element = assertInstanceOf(reference.resolve(), PsiFile::class.java)
        assertEquals(element.viewProvider.virtualFile.name, "test.td")
    }

    override fun getTestDataPath(): String? {
        return "src/test/testData/references"
    }
}