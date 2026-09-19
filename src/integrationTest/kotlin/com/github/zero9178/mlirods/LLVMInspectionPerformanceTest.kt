package com.github.zero9178.mlirods

import com.intellij.driver.client.Remote
import com.intellij.driver.client.service
import com.intellij.driver.sdk.singleProject
import com.intellij.driver.sdk.waitFor
import com.intellij.ide.starter.di.di
import com.intellij.ide.starter.driver.engine.runIdeWithDriver
import com.intellij.ide.starter.models.IdeInfo
import com.intellij.ide.starter.models.TestCase
import com.intellij.ide.starter.path.GlobalPaths
import com.intellij.ide.starter.project.LocalProjectInfo
import com.intellij.ide.starter.runner.Starter
import com.intellij.ide.starter.runner.startIdeWithoutProject
import com.intellij.tools.ide.starter.product.idea.ultimate.IdeaUltimate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.kodein.di.DI
import org.kodein.di.bindSingleton
import java.lang.management.ManagementFactory
import kotlin.io.path.*
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@Remote("com.github.zero9178.mlirods.integration.ProjectOpener", plugin = TEST_PLUGIN_ID)
private interface ProjectOpener {
    fun open(path: String): Boolean
}

@Remote("com.github.zero9178.mlirods.integration.InspectionBenchmark", plugin = TEST_PLUGIN_ID)
private interface InspectionBenchmark {
    fun waitForSmartMode()

    fun getFilesWithContext(): Int

    fun getFileCount(): Int

    fun inspect(output: String): Long

    fun dropCaches()
}

/**
 * Measures how long IntelliJ takes to run all code analysis of the plugin on every TableGen file of LLVM.
 *
 * The project inspected is 'testData/llvm-project': a copy of all TableGen files of LLVM together with the compilation
 * commands CMake generated for them when configured with clang, flang and MLIR enabled. A plugin only installed by
 * these tests (see the 'integrationTestPlugin' source set) supplies the compilation commands to the plugin in place of
 * CMake and runs the inspections.
 */
@OptIn(ExperimentalPathApi::class)
class LLVMInspectionPerformanceTest {

    companion object {
        init {
            // The framework places the IDE and everything a run produces below a directory called 'out' within the
            // directory given here. It defaults to the root of the repository, where the IDE this plugin is developed
            // in would go on to index all of it.
            di = DI {
                extend(di)
                bindSingleton<GlobalPaths>(overrides = true) {
                    object : GlobalPaths(Path(System.getProperty("integration.test.dir"))) {}
                }
            }
        }
    }

    @Test
    fun inspectAllTableGenFiles() {
        val projectHome = Path(System.getProperty("integration.test.project"))
        // Do not let a previous run have an effect on this one.
        projectHome.resolve(".idea").deleteRecursively()

        val testCase = TestCase(IdeInfo.IdeaUltimate, LocalProjectInfo(projectHome))
            .withVersion(System.getProperty("platform.version"))
        val context = Starter.newContext("inspectAllTableGenFiles", testCase).apply {
            pluginConfigurator.installPluginFromPath(Path(System.getProperty("path.to.build.plugin")))
            pluginConfigurator.installPluginFromDir(Path(System.getProperty("integration.test.plugin")))
            addProjectToTrustedLocations()
            setMemorySize(8192)
            // Without any user interface by default. Showing it is of use when debugging the test.
            if (System.getProperty("integration.test.headless").toBoolean()) applyVMOptionsPatch { inHeadlessMode() }
        }

        val reportDir = Path(System.getProperty("integration.test.report.dir"))
        reportDir.deleteRecursively()
        reportDir.createDirectories()
        val iterations = System.getProperty("integration.test.iterations").toInt()

        // Anything else keeping the machine busy makes for slower and less stable times.
        val loadAverage = ManagementFactory.getOperatingSystemMXBean().systemLoadAverage
        var filesWithContext = 0
        var tableGenFiles = 0
        var cold = 0L
        var cached = 0L
        val measured = mutableListOf<Long>()
        // An IDE without a user interface ignores the project given on its command line. It is opened explicitly.
        context.runIdeWithDriver(commandLine = ::startIdeWithoutProject, runTimeout = 60.minutes).useDriverAndCloseIde {
            assertTrue(service<ProjectOpener>().open(projectHome.pathString)) { "failed to open project" }

            val benchmark = service<InspectionBenchmark>(singleProject())
            benchmark.waitForSmartMode()
            // The graph is built up incrementally. Only consider it done once it has stopped changing.
            waitFor("include graph to be built", 10.minutes, 1.seconds) {
                val previous = filesWithContext
                filesWithContext = benchmark.getFilesWithContext()
                filesWithContext != 0 && filesWithContext == previous
            }
            // The files of the graph have only become part of the project now if they were not already.
            benchmark.waitForSmartMode()

            tableGenFiles = benchmark.getFileCount()
            fun inspect(name: String) = benchmark.inspect(reportDir.resolve("problems/$name.tsv").pathString).also {
                println("Inspecting ($name) took $it ms")
            }

            // Dominated by the JVM still interpreting most code: what a user experiences right after opening a project.
            cold = inspect("cold")
            // The JVM is still noticeably busy compiling during the second run.
            benchmark.dropCaches()
            inspect("warmup")
            // The figure to compare between versions of the plugin: all code is analysed anew with the JVM warmed up.
            repeat(iterations) {
                benchmark.dropCaches()
                measured += inspect("run${it + 1}")
            }
            // Nothing has changed since the last run: what the caches of the plugin are worth.
            cached = inspect("cached")
        }

        val problems = readProblems(reportDir.resolve("problems/run$iterations.tsv"))
        val summary = Summary(
            ide = context.ide.build,
            llvmCommit = projectHome.resolve("llvm-commit.txt").readText().trim(),
            files = tableGenFiles,
            filesWithContext = filesWithContext,
            cold = cold,
            measured = measured,
            cached = cached,
            loadAverage = loadAverage,
            problems = problems,
        )
        reportDir.resolve("report.html").writeText(summary.toHtml())
        reportDir.resolve("summary.txt").writeText("$summary\n")
        reportDir.resolve("job-summary.html").writeText(summary.toJobSummary())
        println("Report: ${reportDir.resolve("report.html").toUri()}")
        println(summary)

        // Guard against measuring nothing, e.g. due to the compilation commands not having been picked up or the IDE
        // not actually inspecting anything.
        assertTrue(filesWithContext > 1000) { "expected more than $filesWithContext files with context" }
        assertEquals(projectHome.walk().count { it.extension == "td" }, tableGenFiles, "TableGen files inspected")
        assertTrue(measured.min() > tableGenFiles) { "inspecting $tableGenFiles files in ${measured.min()} ms is implausible" }
        // The analysis is deterministic. Anything else hints at results depending on the state of caches.
        assertEquals(problems, readProblems(reportDir.resolve("problems/cold.tsv")), "problems found by cold run")
        assertEquals(problems, readProblems(reportDir.resolve("problems/cached.tsv")), "problems found by cached run")
    }
}
