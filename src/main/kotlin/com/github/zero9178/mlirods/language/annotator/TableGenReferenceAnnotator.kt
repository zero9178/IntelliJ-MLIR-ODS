package com.github.zero9178.mlirods.language.annotator

import com.github.zero9178.mlirods.MyBundle
import com.github.zero9178.mlirods.language.generated.psi.TableGenIncludeDirective
import com.intellij.lang.annotation.AnnotationHolder

/**
 * Flags an [element] whose include path does not resolve to a file, be it because a directory along the path or the
 * file itself is missing.
 */
private fun checkInclude(element: TableGenIncludeDirective, holder: AnnotationHolder) {
    if (element.includedFile != null) return

    val string = element.string ?: return
    holder.referenceAnnotation(MyBundle.message("tableGen.reference.unresolvedInclude", element.includeSuffix))
        .range(string).create()
}

private val ANNOTATIONS = arrayOf(
    addAnnotationFor { element: TableGenIncludeDirective, holder -> checkInclude(element, holder) },
)

/**
 * Annotator reporting problems with references, currently limited to include paths that do not resolve. Its diagnostics
 * are tagged with [REFERENCE_PROBLEM_GROUP] via [referenceAnnotation] so that [TableGenHighlightInfoFilter] can suppress
 * them in a file without an active context, where references cannot be resolved anyway.
 */
internal class TableGenReferenceAnnotator : TableGenAnnotator(ANNOTATIONS.asIterable())
