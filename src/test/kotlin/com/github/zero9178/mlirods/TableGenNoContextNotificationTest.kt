package com.github.zero9178.mlirods

import com.github.zero9178.mlirods.language.NO_CONTEXT_BANNER_DISMISSED
import com.github.zero9178.mlirods.language.TableGenNoContextNotificationProvider
import com.github.zero9178.mlirods.model.IncludePaths
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class TableGenNoContextNotificationTest : BasePlatformTestCase() {

    private val provider = TableGenNoContextNotificationProvider()

    fun `test banner shown when file has no context`() {
        val file = myFixture.configureByText("test.td", "class C;")
        assertNotNull(provider.collectNotificationData(project, file.virtualFile))
    }

    fun `test no banner when file has a context`() {
        val file = myFixture.configureByText("test.td", "class C;")
        installCompileCommands(
            project, mapOf(
                file.virtualFile to IncludePaths(emptyList())
            )
        )
        assertNull(provider.collectNotificationData(project, file.virtualFile))
    }

    fun `test dismissal is scoped to the editor instance`() {
        val file = myFixture.configureByText("test.td", "class C;")
        val data = provider.collectNotificationData(project, file.virtualFile)
        assertNotNull(data)

        val editor = FileEditorManager.getInstance(project).openFile(file.virtualFile, true).first()
        // The banner is shown for a fresh editor...
        assertNotNull(data!!.apply(editor))

        // ...but hidden for an editor that has dismissed it (as the close button does). A reopened file gets a new
        // editor without the flag, so the banner would appear again.
        editor.putUserData(NO_CONTEXT_BANNER_DISMISSED, true)
        assertNull(data.apply(editor))
    }
}
