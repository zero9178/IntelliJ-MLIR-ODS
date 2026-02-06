package com.github.zero9178.mlirods.settings

import com.github.zero9178.mlirods.MyBundle
import com.github.zero9178.mlirods.lsp.restartTableGenLSPAsync
import com.intellij.openapi.components.service
import com.intellij.openapi.components.serviceIfCreated
import com.intellij.openapi.observable.properties.AtomicBooleanProperty
import com.intellij.openapi.options.SearchableConfigurable
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.util.NlsContexts
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.panel
import org.jetbrains.annotations.NonNls

internal class TableGenToolsApplicationConfigurable : SearchableConfigurable {
    private val settings = service<TableGenToolsApplicationSettings>()
    private val myLspEnabled = AtomicBooleanProperty(settings.lspEnabled)

    override fun getId(): @NonNls String = "tools.tableGen"

    override fun getDisplayName(): @NlsContexts.ConfigurableName String = MyBundle.message("tableGen.tools.displayName")

    override fun isModified(): Boolean {
        return settings.lspEnabled != myLspEnabled.get()
    }

    override fun createComponent() = panel {
        group("Language Server") {
            row {
                checkBox(MyBundle.message("tableGen.lsp.enable"))
                    .bindSelected(myLspEnabled)
            }
        }
    }

    override fun apply() {
        settings.lspEnabled = myLspEnabled.get()

        serviceIfCreated<ProjectManager>()?.openProjects?.forEach {
            restartTableGenLSPAsync(it)
        }
    }

    override fun reset() {
        myLspEnabled.set(settings.lspEnabled)
    }
}