package com.github.zero9178.mlirods.language.annotator

import com.github.zero9178.mlirods.MyBundle
import com.github.zero9178.mlirods.language.generated.psi.TableGenAbstractClassRef
import com.github.zero9178.mlirods.language.generated.psi.TableGenIncludeDirective
import com.github.zero9178.mlirods.language.psi.TableGenFile
import com.github.zero9178.mlirods.model.TableGenContextService
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.components.serviceOrNull
import com.intellij.psi.PsiElement

/**
 * Flags an [element] whose include path does not resolve to a file, be it because a directory along the path or the
 * file itself is missing.
 */
private fun checkInclude(element: TableGenIncludeDirective, holder: AnnotationHolder) {
    if (element.includedFile != null) return

    val string = element.string ?: return
    holder.newAnnotation(
        HighlightSeverity.ERROR, MyBundle.message("tableGen.reference.unresolvedInclude", element.includeSuffix)
    ).range(string).create()
}

/**
 * Flags a class reference (in an inheritance list, a `def`'s parent class, a value's type or a class instantiation)
 * that does not resolve to any class.
 */
private fun checkClassReference(element: TableGenAbstractClassRef, holder: AnnotationHolder) {
    if (element.referencedClass != null) return

    holder.newAnnotation(
        HighlightSeverity.ERROR, MyBundle.message("tableGen.reference.unresolvedClass", element.className)
    ).range(element.classIdentifier).create()
}

private val ANNOTATIONS = arrayOf(
    addAnnotationFor { element: TableGenIncludeDirective, holder -> checkInclude(element, holder) },
    addAnnotationFor { element: TableGenAbstractClassRef, holder -> checkClassReference(element, holder) },
)

/**
 * Annotator reporting problems with references.
 */
internal class TableGenReferenceAnnotator : TableGenAnnotator(ANNOTATIONS.asIterable()) {
    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        // References cannot be resolved in a file without an active context (i.e. one not reachable from any compile
        // commands). The no-context banner already explains this, so skip annotating to avoid flagging every reference.
        val file = element.containingFile as? TableGenFile ?: return
        val virtualFile = file.originalFile.virtualFile ?: return
        if (file.project.serviceOrNull<TableGenContextService>()?.getActiveContext(virtualFile) == null) return

        super.annotate(element, holder)
    }
}
