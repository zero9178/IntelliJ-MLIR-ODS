package com.github.zero9178.mlirods.language.annotator

import com.github.zero9178.mlirods.MyBundle
import com.github.zero9178.mlirods.language.generated.psi.TableGenAbstractClassRef
import com.github.zero9178.mlirods.language.generated.psi.TableGenFieldAccessValueNode
import com.github.zero9178.mlirods.language.generated.psi.TableGenIncludeDirective
import com.github.zero9178.mlirods.language.generated.psi.TableGenMultiClassRef
import com.github.zero9178.mlirods.language.psi.TableGenClassReference
import com.github.zero9178.mlirods.language.psi.TableGenFieldAccessReference
import com.github.zero9178.mlirods.language.psi.TableGenMultiClassReference
import com.github.zero9178.mlirods.language.psi.refersToClass
import com.github.zero9178.mlirods.language.types.TableGenRecordType
import com.github.zero9178.mlirods.model.TableGenCompilationContext
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.HighlightSeverity
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
 * that does not resolve to any class, which a class deriving from itself never does.
 */
private fun checkClassReference(
    element: TableGenAbstractClassRef, holder: AnnotationHolder, context: TableGenCompilationContext
) {
    if (element.referencedDefinitionBlocking(context) != null) return

    val message = if (TableGenClassReference.isDerivingFromOwnClass(element)) {
        MyBundle.message("tableGen.reference.selfDerivedClass", element.className)
    } else {
        MyBundle.message("tableGen.reference.unresolvedClass", element.className)
    }
    holder.newAnnotation(HighlightSeverity.ERROR, message).range(element.classIdentifier).create()
}

/**
 * Flags a name in the parent list of a 'defm' or 'multiclass' statement that does not resolve to anything. The name is
 * reported as an unresolved class if it is one of the trailing class names of a 'defm', and as an unresolved
 * multiclass otherwise.
 */
private fun checkMultiClassReference(
    element: TableGenMultiClassRef, holder: AnnotationHolder, context: TableGenCompilationContext
) {
    if (TableGenMultiClassReference.findTargets(element, context).isNotEmpty()) return

    val message =
        if (refersToClass(element, context)) MyBundle.message("tableGen.reference.unresolvedClass", element.className)
        else MyBundle.message("tableGen.reference.unresolvedMulticlass", element.className)
    holder.newAnnotation(HighlightSeverity.ERROR, message).range(element.identifier).create()
}

/**
 * Flags a field access `x.field` whose left-hand side is a record that does not contain (nor inherit) a field named
 * `field`. Field accesses on a non-record value, or on a record whose class reference is itself unresolved, are left
 * alone: the former is not a reference problem and the latter is already reported as an unresolved class.
 */
private fun checkFieldAccess(
    element: TableGenFieldAccessValueNode, holder: AnnotationHolder, context: TableGenCompilationContext
) {
    val fieldIdentifier = element.fieldIdentifier ?: return
    val fieldName = element.fieldName ?: return
    if (TableGenFieldAccessReference.findField(element, context) != null) return

    val type = element.valueNode.typeBlocking(context) as? TableGenRecordType ?: return
    if (type.record(context) == null) return

    holder.newAnnotation(
        HighlightSeverity.ERROR, MyBundle.message("tableGen.reference.unknownField", type.recordName, fieldName)
    ).range(fieldIdentifier).create()
}

private val ANNOTATIONS = arrayOf(
    addAnnotationFor { element: TableGenIncludeDirective, holder -> checkInclude(element, holder) },
    addAnnotationFor { element: TableGenAbstractClassRef, holder, context ->
        checkClassReference(element, holder, context)
    },
    addAnnotationFor { element: TableGenMultiClassRef, holder, context ->
        checkMultiClassReference(element, holder, context)
    },
    addAnnotationFor { element: TableGenFieldAccessValueNode, holder, context ->
        checkFieldAccess(element, holder, context)
    },
)

/**
 * Annotator reporting problems with references.
 */
internal class TableGenReferenceAnnotator : TableGenAnnotator(ANNOTATIONS.asIterable()) {
    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        // References cannot be resolved in a file without an active context (i.e. one not reachable from any compile
        // commands). The no-context banner already explains this, so skip annotating to avoid flagging every reference.
        if (holder.currentAnnotationSession.compilationContext !is TableGenCompilationContext.Rooted) return

        super.annotate(element, holder)
    }
}
