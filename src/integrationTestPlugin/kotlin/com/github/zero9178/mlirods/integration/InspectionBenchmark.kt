package com.github.zero9178.mlirods.integration

import com.github.zero9178.mlirods.language.TableGenFileType
import com.github.zero9178.mlirods.language.TableGenLanguage
import com.github.zero9178.mlirods.model.TableGenIncludeGraphService
import com.intellij.analysis.AnalysisScope
import com.intellij.codeInsight.daemon.HighlightDisplayKey
import com.intellij.codeInspection.CommonProblemDescriptor
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemDescriptorUtil
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ex.InspectionToolWrapper
import com.intellij.codeInspection.reference.RefEntity
import com.intellij.codeInspection.ui.DefaultInspectionToolPresentation
import com.intellij.codeInspection.ui.InspectionToolPresentation
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.codeInsight.daemon.impl.HighlightVisitorBasedInspection
import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.ex.GlobalInspectionContextImpl
import com.intellij.codeInspection.ex.InspectionManagerEx
import com.intellij.codeInspection.ex.InspectionProfileImpl
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runReadActionBlocking
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.util.ProgressIndicatorBase
import com.intellij.openapi.progress.util.ProgressIndicatorWithDelayedPresentation
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.profile.codeInspection.InspectionProfileManager
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.ProjectScope
import java.util.concurrent.ConcurrentHashMap
import kotlin.io.path.Path
import kotlin.io.path.createParentDirectories
import kotlin.io.path.writeLines

/**
 * Inspections insist on an indicator of this kind, which is usually the one of a modal progress dialog.
 */
private class Indicator : ProgressIndicatorBase(), ProgressIndicatorWithDelayedPresentation {
    override fun setDelayInMillis(delayInMillis: Int) {}
}

/**
 * Context that records the problems found rather than keeping them around to show or export them later.
 *
 * Annotators and highlight visitors not only report problems but every piece of semantic highlighting as well: some
 * 200000 of these in LLVM. Having the IDE export them accounts for a good part of the time inspecting takes.
 */
private class RecordingContext(project: Project) : GlobalInspectionContextImpl(
    project, (InspectionManager.getInstance(project) as InspectionManagerEx).contentManager
) {
    /**
     * Lines as documented by [InspectionBenchmark.inspect]. A set as the IDE inspects a file anew, reporting its
     * problems once more, if interrupted by a write action.
     */
    val problems: MutableSet<String> = ConcurrentHashMap.newKeySet()

    private val myPresentations = ConcurrentHashMap<InspectionToolWrapper<*, *>, InspectionToolPresentation>()

    init {
        // Inspecting is otherwise cancelled right away, without any error, in an IDE with a user interface: it is taken
        // as the user having closed the tool window the results are to be shown in.
        myViewClosed = false
    }

    override fun getPresentation(toolWrapper: InspectionToolWrapper<*, *>) = myPresentations.computeIfAbsent(toolWrapper) {
        object : DefaultInspectionToolPresentation(toolWrapper, this) {
            override fun addProblemElement(
                refElement: RefEntity?, filterSuppressed: Boolean, vararg descriptors: CommonProblemDescriptor
            ) = descriptors.filterIsInstance<ProblemDescriptor>().forEach { record(toolWrapper, it) }
        }
    }

    private fun record(toolWrapper: InspectionToolWrapper<*, *>, descriptor: ProblemDescriptor) {
        val element = descriptor.psiElement ?: return
        // Semantic highlighting is reported as weak warnings without a description.
        val description = ProblemDescriptorUtil.renderDescriptionMessage(descriptor, element)
        if (description.isBlank()) return

        val severity = when (descriptor.highlightType) {
            ProblemHighlightType.INFORMATION -> return
            ProblemHighlightType.ERROR, ProblemHighlightType.GENERIC_ERROR, ProblemHighlightType.LIKE_UNKNOWN_SYMBOL ->
                HighlightSeverity.ERROR

            ProblemHighlightType.WARNING, ProblemHighlightType.LIKE_UNUSED_SYMBOL, ProblemHighlightType.LIKE_DEPRECATED,
            ProblemHighlightType.LIKE_MARKED_FOR_REMOVAL -> HighlightSeverity.WARNING

            ProblemHighlightType.WEAK_WARNING -> HighlightSeverity.WEAK_WARNING
            // Up to the severity the inspection has been configured with.
            else -> currentProfile.getErrorLevel(HighlightDisplayKey.find(toolWrapper.shortName)!!, element).severity
        }

        val file = element.containingFile?.virtualFile
        val root = project.guessProjectDir()
        problems += listOf(
            severity.name,
            file?.let { root?.let { root -> VfsUtilCore.getRelativePath(it, root) } ?: it.path }.orEmpty(),
            (descriptor.lineNumber + 1).toString(),
            toolWrapper.displayName,
            description,
        ).joinToString("\t") { it.replace(Regex("\\s+"), " ") }
    }
}

/**
 * Runs the code analysis of the plugin as batch inspections on behalf of the integration tests.
 *
 * All methods are called by the tests from outside the IDE. Their parameters and return values are therefore limited
 * to what can be transferred between processes.
 */
@Service(Service.Level.PROJECT)
class InspectionBenchmark(private val project: Project) {

    companion object {
        private val LOGGER = logger<InspectionBenchmark>()
    }

    /**
     * All TableGen files that are part of a module of the project.
     */
    private val scope: GlobalSearchScope
        get() = GlobalSearchScope.getScopeRestrictedByFileTypes(
            ProjectScope.getContentScope(project), TableGenFileType.INSTANCE
        )

    /**
     * Returns once the IDE is done indexing.
     */
    fun waitForSmartMode() = DumbService.getInstance(project).waitForSmartMode()

    /**
     * Returns the number of files that have a context or 0 while the plugin is still busy determining these.
     */
    fun getFilesWithContext(): Int {
        val graph = project.service<TableGenIncludeGraphService>()
        return if (graph.updatesInFlight.value != 0) 0 else runReadActionBlocking { graph.getFilesWithContext().size }
    }

    /**
     * Returns the number of files [inspect] inspects.
     */
    fun getFileCount(): Int = runReadActionBlocking { FileTypeIndex.getFiles(TableGenFileType.INSTANCE, scope).size }

    /**
     * Inspects all files, writes the problems found to the file [output] and returns the time inspecting took in
     * milliseconds.
     *
     * The file consists of one line per problem made up of the severity, the path of the file relative to the project,
     * the line within the file, the name of the inspection and the description of the problem separated by tabs.
     */
    fun inspect(output: String): Long {
        val context = RecordingContext(project)
        context.setExternalProfile(createProfile())

        val outputFile = Path(output)
        val analysisScope = AnalysisScope(scope, project)
        val duration = try {
            val start = System.nanoTime()
            ProgressManager.getInstance().runProcess({
                // Nothing is ever written to the directory given due to the context not keeping any problems.
                context.launchInspectionsOffline(analysisScope, outputFile.parent, false, mutableListOf())
            }, Indicator())
            (System.nanoTime() - start) / 1_000_000
        } finally {
            context.cleanup()
        }

        outputFile.createParentDirectories().writeLines(context.problems.sorted())
        return duration
    }

    /**
     * Returns a profile consisting of nothing but the code analysis of the plugin. Inspections of the platform that
     * apply to any language, spell checking for instance, would otherwise account for part of the time and problems.
     */
    private fun createProfile(): InspectionProfileImpl {
        val profile = InspectionProfileImpl("TableGen")
        profile.copyFrom(InspectionProfileManager.getInstance(project).currentProfile)
        profile.disableAllTools(project)

        profile.getInspectionTools(null).filter { it.language == TableGenLanguage.INSTANCE.id }.forEach {
            profile.enableTool(it.shortName, project)
        }

        // Batch inspections run neither annotators nor highlight visitors despite these performing most of the code
        // analysis of the plugin. This inspection of the platform, disabled by default, runs them as an inspection.
        profile.enableTool(HighlightVisitorBasedInspection.SHORT_NAME, project)
        val annotators = profile.getInspectionTool(HighlightVisitorBasedInspection.SHORT_NAME, project)!!.tool
        (annotators as HighlightVisitorBasedInspection).setHighlightErrorElements(true).setRunAnnotators(true)
            .setRunVisitors(true)
        LOGGER.info("inspecting using ${profile.getAllEnabledInspectionTools(project).map { it.shortName }}")
        return profile
    }

    /**
     * Discards everything the IDE and the plugin have cached about the code, making the next call of [inspect] analyse
     * all code anew. Indices are kept as is the machine code the JVM generated.
     */
    fun dropCaches() {
        ApplicationManager.getApplication().invokeAndWait {
            runWriteAction { PsiManager.getInstance(project).dropPsiCaches() }
        }
        System.gc()
    }
}
