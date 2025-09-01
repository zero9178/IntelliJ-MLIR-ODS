package com.github.zero9178.mlirods.language.linemarkers

import com.github.zero9178.mlirods.MyIcons
import com.github.zero9178.mlirods.language.generated.TableGenTypes
import com.github.zero9178.mlirods.language.generated.psi.TableGenClassStatement
import com.intellij.codeInsight.daemon.GutterName
import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProviderDescriptor
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.psi.PsiElement
import com.intellij.psi.util.elementType
import javax.swing.Icon

/**
 * Adds line markers that allow navigation to every records that derives from a class.
 */
private class TableGenDerivesClassLineMarkerProvider : LineMarkerProviderDescriptor() {
    override fun getName(): @GutterName String {
        return "Derives class"
    }

    override fun getIcon(): Icon {
        return MyIcons.TableGenDerivedClass
    }

    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? {
        if (element.elementType != TableGenTypes.IDENTIFIER) return null

        val parent = element.parent as? TableGenClassStatement ?: return null

        val deriving = parent.allDerivedRecords.toList().ifEmpty {
            return null
        }

        return NavigationGutterIconBuilder.create(icon)
            .setTooltipText("Navigate to derived records of '${parent.name}'")
            .setAlignment(GutterIconRenderer.Alignment.RIGHT).setTargets(deriving).createLineMarkerInfo(element)
    }
}
