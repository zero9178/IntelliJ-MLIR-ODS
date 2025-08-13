package com.github.zero9178.mlirods.language.psi

import com.intellij.navigation.NavigationItem
import com.intellij.psi.PsiNameIdentifierOwner

/**
 * Interface used for any [com.intellij.psi.PsiElement]s that contain a TableGen identifier defining an element to be
 * found by identifier-lookup.
 */
interface TableGenIdentifierElement : PsiNameIdentifierOwner, NavigationItem
