package com.github.zero9178.mlirods.language.linemarkers

import com.github.zero9178.mlirods.MyIcons
import com.github.zero9178.mlirods.language.generated.TableGenTypes
import com.github.zero9178.mlirods.language.generated.psi.TableGenFieldBodyItem
import com.github.zero9178.mlirods.language.generated.psi.TableGenLetBodyItem
import com.github.zero9178.mlirods.language.psi.TableGenFieldScopeNode
import com.intellij.codeInsight.daemon.GutterName
import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProviderDescriptor
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.psi.PsiElement
import com.intellij.psi.util.elementType
import com.intellij.psi.util.parentOfType
import javax.swing.Icon

/**
 * Adds line markers to any 'let' body items, that allows navigating to whichever value it overrides.
 * Allows users to navigate and find out which value it'd have if they removed the 'let' body item.
 *
 * Currently, does not insert the line marker if it directly overrides a field (since users can navigate to the field
 * by using field name). This may change in the future depending on how intuitive it is.
 */
private class TableGenOverridingFieldAssignmentLineMarkerProvider : LineMarkerProviderDescriptor() {
    override fun getName(): @GutterName String {
        return "Overriding"
    }

    override fun getIcon(): Icon {
        return MyIcons.TableGenOverriding
    }

    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? {
        if (element.elementType != TableGenTypes.IDENTIFIER) return null

        val parent = element.parent as? TableGenLetBodyItem ?: return null

        val scope = parent.parentOfType<TableGenFieldScopeNode>() ?: return null
        val fieldName = parent.fieldName
        val overwriting =
            scope.allFieldAssignments[fieldName ?: return null].orEmpty().asReversed().asSequence().dropWhile {
                it != parent
            }.drop(1).firstOrNull() ?: return null

        // There is no value in showing a gutter for overwriting a field: The user could simply navigate to the
        // definition via the field identifier.
        if (overwriting is TableGenFieldBodyItem)
            return null

        return LineMarkerInfo(
            element, element.textRange, icon, {
                "Navigate to previous value of '$fieldName'"
            }, { _, _ ->
                overwriting.navigate(true)
            },
            GutterIconRenderer.Alignment.RIGHT
        ) {
            "Navigate to previous value of '$fieldName'"
        }
    }
}
