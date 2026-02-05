package com.github.zero9178.mlirods.clion

import com.github.zero9178.mlirods.MyBundle
import com.github.zero9178.mlirods.lsp.isTableGenFile
import com.github.zero9178.mlirods.settings.TableGenToolsApplicationSettings
import com.intellij.openapi.application.readAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.EditorNotificationPanel
import com.intellij.ui.EditorNotificationProvider
import com.intellij.ui.EditorNotifications
import com.intellij.util.concurrency.annotations.RequiresReadLock
import com.intellij.util.getValue
import com.intellij.util.setValue
import com.jetbrains.cidr.cpp.cmake.model.CMakeConfiguration
import com.jetbrains.cidr.cpp.cmake.workspace.CMakeWorkspace
import com.jetbrains.cidr.cpp.execution.CMakeAppRunConfiguration
import com.jetbrains.cidr.cpp.execution.build.CMakeBuild
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicReference
import java.util.function.Function
import javax.swing.JComponent

/**
 * Service used to control displaying banners in editors to build the "tblgen-lsp-server" executable.
 */
@Service(Service.Level.PROJECT)
class CMakeTableGenBuildNotificationProviderService(private val project: Project, private val cs: CoroutineScope) {

    init {
        cs.launch {
            service<TableGenToolsApplicationSettings>().lspEnabledFlow.filter { !it }.collect {
                // Clear the notification anytime the LSP is turned off.
                clearNotifications()
            }
        }
    }

    private var myConfiguration: CMakeConfiguration? by AtomicReference(null)

    /**
     * Shows a banner in all TableGen editors that prompts users to build the "tblgen-lsp-server" target.
     * Uses [configuration] for the build action.
     *
     * If a banner is currently active, the banner is replaced with the new [configuration].
     *
     * May be called from any thread.
     */
    fun showBuildNotification(configuration: CMakeConfiguration) = cs.launch {
        val workSpace = project.service<CMakeWorkspace>()
        val hostMachine = readAction {
            workSpace
                .profileInfos
        }.find {
            it.profile.name == configuration.profileName
        }?.environment?.hostMachine
        // Do not show notification for remote machines. We do not support running LSPs on them.
        myConfiguration = if (hostMachine?.isRemote ?: true) {
            null
        } else {
            configuration
        }
        EditorNotifications.getInstance(project).updateAllNotifications()
    }

    /**
     * Returns true if notifications should be shown in TableGen files.
     */
    val shouldShowNotification: Boolean
        get() = myConfiguration != null

    /**
     * Clears any build prompt banners in editors.
     *
     * May be called from any thread.
     */
    fun clearNotifications() {
        myConfiguration = null
        EditorNotifications.getInstance(project).updateAllNotifications()
    }

    /**
     * Asynchronously starts the build action according to the last used configuration set in [showBuildNotification].
     *
     * May be called from any thread.
     */
    internal fun performBuild() {
        val value = myConfiguration ?: return
        cs.launch {
            CMakeBuild.build(project, CMakeAppRunConfiguration.BuildAndRunConfigurations(value))
        }
    }
}

private class CMakeTableGenBuildNotificationProvider : EditorNotificationProvider, DumbAware {

    @RequiresReadLock
    override fun collectNotificationData(
        project: Project, file: VirtualFile
    ): Function<in FileEditor, out JComponent?>? {
        if (!file.isTableGenFile) return null
        if (!service<TableGenToolsApplicationSettings>().lspEnabled) return null

        val service = project.service<CMakeTableGenBuildNotificationProviderService>()
        if (!service.shouldShowNotification) return null

        return Function<FileEditor, JComponent> {
            val editorNotificationPanel = EditorNotificationPanel(it, EditorNotificationPanel.Status.Error)
            editorNotificationPanel.createActionLabel(MyBundle.message("tableGenBuildBannerAction")) {
                service.performBuild()
            }
            editorNotificationPanel.text = MyBundle.message("tableGenBuildBannerText")
            editorNotificationPanel
        }
    }
}