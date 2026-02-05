package com.github.zero9178.mlirods.settings

import com.intellij.openapi.components.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

@Service
@State(
    name = "tableGen.tools.application",
    storages = [Storage("tableGen.tools.xml", roamingType = RoamingType.DEFAULT)],
    category = SettingsCategory.TOOLS
)
class TableGenToolsApplicationSettings(private val cs: CoroutineScope) :
    SerializablePersistentStateComponent<TableGenToolsApplicationSettings.State>(State()) {

    /**
     * Controls whether 'tblgen-lsp-server' should be used or not.
     */
    var lspEnabled: Boolean
        get() = state.lspEnabled
        set(value) {
            updateState {
                it.copy(lspEnabled = value)
            }
            cs.launch {
                myLspEnabledFlow.emit(value)
            }
        }

    private val myLspEnabledFlow = MutableSharedFlow<Boolean>(replay = 1)

    /**
     * Flow yielding a value anytime [lspEnabled] has changed.
     * Note that it only emits when the value has actually changed.
     */
    val lspEnabledFlow: Flow<Boolean>
        get() = myLspEnabledFlow.distinctUntilChanged()

    override fun loadState(state: State) {
        super.loadState(state)
        // Update the flow anytime state is reloaded from disk.
        cs.launch {
            myLspEnabledFlow.emit(state.lspEnabled)
        }
    }

    data class State(
        @JvmField val lspEnabled: Boolean = true
    )
}