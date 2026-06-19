package com.github.zero9178.mlirods.language.annotator

import com.github.zero9178.mlirods.MyBundle
import com.github.zero9178.mlirods.language.generated.psi.TableGenAbstractClassRef
import com.github.zero9178.mlirods.language.generated.psi.TableGenArgValueItem
import com.github.zero9178.mlirods.language.generated.psi.TableGenClassInstantiationValueNode
import com.github.zero9178.mlirods.language.generated.psi.TableGenClassRef
import com.github.zero9178.mlirods.language.generated.psi.TableGenTemplateArgDecl
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.HighlightSeverity

/**
 * Validates that the arguments passed to a class reference match its template argument declarations:
 * 1. No template argument is assigned more than once.
 * 2. Every argument resolves to a template argument declaration.
 * 3. Every template argument declaration without a default value is assigned a value.
 */
private fun checkArguments(element: TableGenAbstractClassRef, holder: AnnotationHolder) {
    val targetClass = element.referencedClass ?: return

    // Map each referenced declaration to the arguments assigning a value to it.
    val itemsByDecl = mutableMapOf<TableGenTemplateArgDecl, MutableList<TableGenArgValueItem>>()
    for (item in element.argValueItemList) {
        val decl = item.referencedTemplateArgDecl
        if (decl != null) {
            itemsByDecl.getOrPut(decl) { mutableListOf() }.add(item)
            continue
        }

        // 2.) Every argument must reference a template argument declaration.
        val message = if (item.isNamedArgument) {
            MyBundle.message(
                "tableGen.syntax.unknownNamedArgument", targetClass.name ?: "", item.identifierName ?: ""
            )
        } else {
            MyBundle.message(
                "tableGen.syntax.tooManyArguments", targetClass.name ?: "", targetClass.templateArgDeclList.size
            )
        }
        holder.newAnnotation(HighlightSeverity.ERROR, message).range(item).create()
    }

    // 1.) A template argument must not be assigned more than once; only flag the redundant later assignments.
    for ((decl, items) in itemsByDecl) {
        items.drop(1).forEach {
            holder.newAnnotation(
                HighlightSeverity.ERROR, MyBundle.message("tableGen.syntax.duplicateArgument", decl.name ?: "")
            ).range(it).create()
        }
    }

    // 3.) Every template argument without a default value must be assigned a value.
    for (decl in targetClass.templateArgDeclList) {
        if (decl.valueNode != null || decl in itemsByDecl) continue

        holder.newAnnotation(
            HighlightSeverity.ERROR, MyBundle.message("tableGen.syntax.missingArgument", decl.name ?: "")
        ).range(element.classIdentifier).create()
    }
}

private val ANNOTATIONS = arrayOf(
    // Only validate arguments for class references in an inheritance list and for class instantiations; other
    // references (such as a class used as a type) do not pass template arguments.
    addAnnotationFor { element: TableGenClassRef, holder -> checkArguments(element, holder) },
    addAnnotationFor { element: TableGenClassInstantiationValueNode, holder -> checkArguments(element, holder) },
)

internal class TableGenSemanticAnnotator : TableGenAnnotator(ANNOTATIONS.asIterable())
