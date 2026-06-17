package com.github.zero9178.mlirods.language.inspection

import com.github.zero9178.mlirods.MyBundle
import com.github.zero9178.mlirods.language.generated.psi.TableGenFieldBodyItem
import com.github.zero9178.mlirods.language.generated.psi.TableGenVisitor
import com.github.zero9178.mlirods.language.psi.TableGenRecord
import com.github.zero9178.mlirods.language.psi.createLetBodyItem
import com.github.zero9178.mlirods.language.stubs.disallowTreeLoading
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.util.parentOfType

/**
 * Reports a [TableGenFieldBodyItem] that redefines a field already defined by the enclosing scope or one of its base
 * classes. While legal TableGen, redefining a field merely to assign it a new value is a code smell: a `let` statement
 * conveys the intent of overriding an inherited field's value without shadowing its declaration.
 */
internal class TableGenRedefinedFieldInspection : LocalInspectionTool() {

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor =
        object : TableGenVisitor<Unit>() {
            override fun visitFieldBodyItem(element: TableGenFieldBodyItem) = disallowTreeLoading {
                val definingField = element.definingFieldBodyItem
                if (definingField == element)
                    return@disallowTreeLoading

                val fieldIdentifier = element.fieldIdentifier ?: return@disallowTreeLoading
                val fieldName = definingField.fieldName ?: return@disallowTreeLoading
                val parentRecordName = definingField.parentOfType<TableGenRecord>()?.name ?: "<anonymous>"
                // The quick fix can only produce a 'let' statement if there is a value to assign.
                val fix =
                    if (element.valueNode != null) arrayOf(ConvertToLetQuickFix) else LocalQuickFix.EMPTY_ARRAY

                holder.registerProblem(
                    fieldIdentifier,
                    MyBundle.message("tableGen.inspection.redefinedField.message", parentRecordName, fieldName),
                    *fix
                )
            }
        }
}

/**
 * Replaces a field redefinition such as `int x = 5;` with the equivalent `let x = 5;` that overrides the inherited
 * field's value instead of redeclaring it.
 */
object ConvertToLetQuickFix : LocalQuickFix {

    override fun getFamilyName(): String = MyBundle.message("tableGen.inspection.redefinedField.quickFix")

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val field = descriptor.psiElement.parentOfType<TableGenFieldBodyItem>(true) ?: return
        val fieldName = field.fieldName ?: return
        val value = field.valueNode ?: return
        field.replace(createLetBodyItem(project, fieldName, value.text))
    }
}
