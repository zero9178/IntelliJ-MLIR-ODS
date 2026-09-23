package com.github.zero9178.mlirods.language.annotator

import com.github.zero9178.mlirods.MyBundle
import com.github.zero9178.mlirods.language.generated.psi.TableGenAbstractClassRef
import com.github.zero9178.mlirods.language.generated.psi.TableGenArgValueItem
import com.github.zero9178.mlirods.language.generated.psi.TableGenClassInstantiationValueNode
import com.github.zero9178.mlirods.language.generated.psi.TableGenClassRef
import com.github.zero9178.mlirods.language.generated.psi.TableGenClassStatement
import com.github.zero9178.mlirods.language.generated.psi.TableGenMulticlassStatement
import com.github.zero9178.mlirods.language.generated.psi.TableGenTemplateArgDecl
import com.github.zero9178.mlirods.language.psi.findVisibleMulticlass
import com.github.zero9178.mlirods.language.types.TableGenType
import com.github.zero9178.mlirods.language.types.TableGenUnknownType
import com.github.zero9178.mlirods.model.TableGenCompilationContext
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.modcommand.ActionContext
import com.intellij.modcommand.ModCommand
import com.intellij.modcommand.PsiBasedModCommandAction
import com.intellij.openapi.progress.runBlockingCancellable
import com.intellij.psi.PsiNameIdentifierOwner

/**
 * Validates that the arguments passed to a class reference match its template argument declarations:
 * 1. No template argument is assigned more than once.
 * 2. Every argument resolves to a template argument declaration.
 * 3. Every template argument declaration without a default value is assigned a value.
 * 4. Every argument's value is of a type assignable to its template argument declaration.
 */
private fun checkArguments(
    element: TableGenAbstractClassRef, holder: AnnotationHolder, context: TableGenCompilationContext
) {
    val targetClass = element.referencedClass(context) ?: return

    // Entered once per class reference, with the types and declarations requested one after the other: almost every
    // argument is a literal or an identifier, whose type costs less than a coroutine of its own would.
    val items = element.argValueItemList
    val resolved = runBlockingCancellable {
        items.map { (it.valueNode?.type(context) ?: TableGenUnknownType) to it.referencedTemplateArgDecl(context) }
    }

    // Map each referenced declaration to the arguments assigning a value to it.
    val itemsByDecl = mutableMapOf<TableGenTemplateArgDecl, MutableList<TableGenArgValueItem>>()
    for ((item, typeAndDecl) in items.zip(resolved)) {
        val (valueType, decl) = typeAndDecl
        if (decl != null) {
            itemsByDecl.getOrPut(decl) { mutableListOf() }.add(item)
            checkArgumentType(item, valueType, decl, holder, context)
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

/**
 * Flags an argument [item] whose value is of [valueType], a type that cannot be assigned to its resolved template
 * argument declaration [decl]. Mirroring TableGen, the check uses type convertibility; if either type is unknown (or otherwise
 * indeterminate) no error is reported.
 */
private fun checkArgumentType(
    item: TableGenArgValueItem,
    valueType: TableGenType,
    decl: TableGenTemplateArgDecl,
    holder: AnnotationHolder,
    context: TableGenCompilationContext,
) {
    val valueNode = item.valueNode ?: return
    val declaredType = decl.typeNode.toType()

    // Only report a definite mismatch; an indeterminate result (null) leaves the argument alone.
    if (valueType.isConvertibleTo(declaredType, context) != false) return

    holder.newAnnotation(
        HighlightSeverity.ERROR, MyBundle.message(
            "tableGen.syntax.argumentTypeMismatch",
            valueType.toString(), decl.name ?: "", declaredType.toString()
        )
    ).range(valueNode).create()
}

/**
 * Navigates to the definition a class or multiclass statement clashes with.
 */
private class NavigateToPreviousDefinitionFix(definition: PsiNameIdentifierOwner) :
    PsiBasedModCommandAction<PsiNameIdentifierOwner>(definition) {

    override fun getFamilyName() = MyBundle.message("tableGen.syntax.classRedefinition.navigate")

    override fun perform(context: ActionContext, element: PsiNameIdentifierOwner): ModCommand =
        ModCommand.select(element.nameIdentifier ?: element)
}

/**
 * Flags a class statement if the class has already been defined by a statement preceding it. Mirroring TableGen, this
 * includes declarations: a class may be declared any number of times, but only up until it is defined. The definition
 * reported is the one closest to [element].
 */
private fun checkRedefinition(
    element: TableGenClassStatement, holder: AnnotationHolder, context: TableGenCompilationContext
) {
    val identifier = element.nameIdentifier ?: return
    val previous = element.previousStatements(context).lastOrNull { !it.isDeclaration } ?: return

    holder.newAnnotation(
        HighlightSeverity.ERROR, MyBundle.message("tableGen.syntax.classRedefinition", element.name ?: "")
    ).range(identifier).withFix(NavigateToPreviousDefinitionFix(previous)).create()
}

/**
 * Flags a multiclass statement if the multiclass has already been defined by a statement preceding it. Unlike a class,
 * a multiclass cannot be declared: every statement of it defines it, making every statement following the first one an
 * error in TableGen. The definition reported is the one closest to [element].
 */
private fun checkRedefinition(
    element: TableGenMulticlassStatement, holder: AnnotationHolder, context: TableGenCompilationContext
) {
    val identifier = element.nameIdentifier ?: return
    val name = element.name ?: return
    val previous = findVisibleMulticlass(name, element, context) ?: return

    holder.newAnnotation(
        HighlightSeverity.ERROR, MyBundle.message("tableGen.syntax.multiclassRedefinition", name)
    ).range(identifier).withFix(NavigateToPreviousDefinitionFix(previous)).create()
}

private val ANNOTATIONS = arrayOf(
    // Only validate arguments for class references in an inheritance list and for class instantiations; other
    // references (such as a class used as a type) do not pass template arguments.
    addAnnotationFor { element: TableGenClassRef, holder, context -> checkArguments(element, holder, context) },
    addAnnotationFor { element: TableGenClassInstantiationValueNode, holder, context ->
        checkArguments(element, holder, context)
    },
    addAnnotationFor { element: TableGenClassStatement, holder, context ->
        checkRedefinition(element, holder, context)
    },
    addAnnotationFor { element: TableGenMulticlassStatement, holder, context ->
        checkRedefinition(element, holder, context)
    },
)

internal class TableGenSemanticAnnotator : TableGenAnnotator(ANNOTATIONS.asIterable())
