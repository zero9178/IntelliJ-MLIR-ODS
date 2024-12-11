package com.github.zero9178.mlirods.service

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service

/**
 * Service used as a proxy for the lifetime of the plugin.
 */
@Service(Service.Level.PROJECT)
class PluginLifetimeService : Disposable {
    override fun dispose() {

    }
}