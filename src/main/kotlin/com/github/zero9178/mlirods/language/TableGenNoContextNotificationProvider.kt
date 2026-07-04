package com.github.zero9178.mlirods.language

import com.github.zero9178.mlirods.MyBundle
import com.github.zero9178.mlirods.lsp.isTableGenFile
import com.github.zero9178.mlirods.model.TableGenContextService
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.components.serviceOrNull
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.EditorNotificationPanel
import com.intellij.ui.EditorNotificationProvider
import com.intellij.ui.EditorNotifications
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.function.Function
import javax.swing.JComponent

/**
 * Marks a [FileEditor] whose no-context banner the user has dismissed. Because the flag lives on the editor instance,
 * dismissal only lasts for as long as that editor is open: reopening the file creates a fresh editor and the banner
 * appears again.
 */
internal val NO_CONTEXT_BANNER_DISMISSED = Key.create<Boolean>("TableGen.noContextBanner.dismissed")

/**
 * Refreshes the editor banners contributed by [TableGenNoContextNotificationProvider] whenever the set of files with a
 * context may have changed, so that a banner appears or disappears as soon as a file gains or loses its context.
 */
@Service(Service.Level.PROJECT)
private class TableGenNoContextNotificationService(project: Project, cs: CoroutineScope) {
    init {
        cs.launch {
            project.service<TableGenContextService>().contextGeneration.collectLatest {
                EditorNotifications.getInstance(project).updateAllNotifications()
            }
        }
    }
}

/**
 * Shows a banner in a TableGen file that has no active context, i.e. that is not reachable from any file with compile
 * commands. Such a file's includes and references cannot be resolved, so the banner tells the user why analysis is
 * incomplete. The banner can be dismissed via its close button for the lifetime of the editor.
 */
internal class TableGenNoContextNotificationProvider : EditorNotificationProvider, DumbAware {
    override fun collectNotificationData(
        project: Project, file: VirtualFile
    ): Function<in FileEditor, out JComponent?>? {
        if (!file.isTableGenFile) return null

        val contextService = project.serviceOrNull<TableGenContextService>() ?: return null
        // Start the refresher so the banner is re-evaluated once contexts change.
        project.service<TableGenNoContextNotificationService>()
        if (contextService.getActiveContext(file) != null) return null

        return Function { editor ->
            if (editor.getUserData(NO_CONTEXT_BANNER_DISMISSED) == true) return@Function null

            EditorNotificationPanel(editor, EditorNotificationPanel.Status.Warning).apply {
                text = MyBundle.message("tableGen.noContext.bannerText")
                setCloseAction {
                    editor.putUserData(NO_CONTEXT_BANNER_DISMISSED, true)
                    EditorNotifications.getInstance(project).updateNotifications(file)
                }
            }
        }
    }
}
