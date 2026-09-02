package com.github.zero9178.mlirods.language.documentation

import com.intellij.model.Pointer
import com.intellij.navigation.NavigationItem
import com.intellij.platform.backend.documentation.DocumentationResult
import com.intellij.platform.backend.documentation.DocumentationTarget
import com.intellij.platform.backend.documentation.PsiDocumentationTargetProvider
import com.intellij.platform.backend.presentation.TargetPresentation
import com.intellij.pom.Navigatable
import com.intellij.psi.PsiElement
import com.intellij.psi.createSmartPointer

/**
 * Provides quick documentation for TableGen elements by rendering the comment block directly preceding them.
 */
internal class TableGenDocumentationTargetProvider : PsiDocumentationTargetProvider {
    override fun documentationTarget(element: PsiElement, originalElement: PsiElement?): DocumentationTarget? {
        if (!isDocumentedTableGenElement(element)) return null

        return TableGenDocumentationTarget(element)
    }
}

internal class TableGenDocumentationTarget(private val element: PsiElement) : DocumentationTarget {
    override fun createPointer(): Pointer<out DocumentationTarget> {
        val pointer = element.createSmartPointer()
        return Pointer.delegatingPointer(pointer, ::TableGenDocumentationTarget)
    }

    override fun computePresentation(): TargetPresentation {
        val presentation = (element as? NavigationItem)?.presentation
        return TargetPresentation.builder(presentation?.presentableText.orEmpty())
            .icon(presentation?.getIcon(false))
            .locationText(presentation?.locationString)
            .presentation()
    }

    override val navigatable: Navigatable?
        get() = element as? Navigatable

    override fun computeDocumentation(): DocumentationResult? = generateDocumentation(element)?.let {
        DocumentationResult.documentation(it)
    }

    override fun computeDocumentationHint(): String? = generateDocumentation(element)
}
