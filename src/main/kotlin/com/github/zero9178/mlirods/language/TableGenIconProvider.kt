package com.github.zero9178.mlirods.language

import com.github.zero9178.mlirods.MyIcons
import com.github.zero9178.mlirods.language.generated.psi.TableGenClassStatement
import com.github.zero9178.mlirods.language.psi.TableGenIdentifierElement
import com.intellij.ide.IconProvider
import com.intellij.psi.PsiElement
import javax.swing.Icon

private class TableGenIconProvider : IconProvider() {
    override fun getIcon(element: PsiElement, flags: Int): Icon? {
        if (element.language != TableGenLanguage.INSTANCE) return null

        when (element) {
            is TableGenClassStatement, is TableGenIdentifierElement -> return element.presentation?.getIcon(false)
        }

        return MyIcons.TableGenIcon
    }
}
