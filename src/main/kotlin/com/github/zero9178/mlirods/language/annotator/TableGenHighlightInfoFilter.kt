package com.github.zero9178.mlirods.language.annotator

import com.github.zero9178.mlirods.language.psi.TableGenFile
import com.github.zero9178.mlirods.model.TableGenContextService
import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.codeInsight.daemon.impl.HighlightInfoFilter
import com.intellij.openapi.components.serviceOrNull
import com.intellij.psi.PsiFile

/**
 * Drops diagnostics emitted from a reference annotator in a [TableGenFile] that has no active context. Such a file's
 * includes and references cannot be resolved, so every reference would otherwise be reported as unresolved. Purely
 * syntactic errors (from the parser or [TableGenSyntaxAnnotator]) do not originate from a reference annotator and are
 * therefore kept.
 *
 * Reference-annotator diagnostics are recognized by [REFERENCE_PROBLEM_GROUP]; see [TableGenAnnotator]. Filtering the
 * highlights centrally keeps the annotators oblivious to this special case.
 */
internal class TableGenHighlightInfoFilter : HighlightInfoFilter {
    override fun accept(highlightInfo: HighlightInfo, file: PsiFile?): Boolean {
        // Only diagnostics emitted from a reference annotator are candidates for suppression.
        if (highlightInfo.problemGroup !== REFERENCE_PROBLEM_GROUP) return true

        val tableGenFile = file as? TableGenFile ?: return true
        val virtualFile = tableGenFile.originalFile.virtualFile ?: return true
        val service = tableGenFile.project.serviceOrNull<TableGenContextService>() ?: return true
        // A file without an active context cannot resolve its references, so drop the diagnostic.
        return service.getActiveContext(virtualFile) != null
    }
}
