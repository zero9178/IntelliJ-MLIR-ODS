package com.github.zero9178.mlirods.language.psi

import com.intellij.navigation.NavigationItem
import com.intellij.openapi.util.NlsSafe
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNameIdentifierOwner

/**
 * Interface used for any [com.intellij.psi.PsiElement]s that contain a TableGen identifier defining an element to be
 * found by 'def'-lookup.
 */
interface TableGenDefNameIdentifierOwner : PsiNameIdentifierOwner, NavigationItem {
    override fun setName(name: @NlsSafe String): PsiElement? {
        TODO("Not yet implemented")
    }
}