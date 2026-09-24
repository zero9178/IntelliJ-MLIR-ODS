package com.github.zero9178.mlirods.language.documentation

import com.intellij.platform.backend.documentation.DocumentationLinkHandler
import com.intellij.platform.backend.documentation.DocumentationTarget
import com.intellij.platform.backend.documentation.LinkResolveResult

/**
 * Resolves the hyperlinks within TableGen documentation to the documentation of the elements they lead to.
 */
internal class TableGenDocumentationLinkHandler : DocumentationLinkHandler {
    override fun resolveLink(target: DocumentationTarget, url: String): LinkResolveResult? {
        if (target !is TableGenDocumentationTarget) return null

        val linked = resolveDocumentationLink(target.element, url) ?: return null
        return LinkResolveResult.resolvedTarget(TableGenDocumentationTarget(linked))
    }
}
