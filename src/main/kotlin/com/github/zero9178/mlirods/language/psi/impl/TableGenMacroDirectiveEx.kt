package com.github.zero9178.mlirods.language.psi.impl

import com.intellij.psi.PsiElement

/**
 * Interface used to add extra methods to the preprocessor directives carrying a macro name, i.e.
 * [com.github.zero9178.mlirods.language.generated.psi.TableGenDefineDirective] and
 * [com.github.zero9178.mlirods.language.generated.psi.TableGenIfdefIfndefDirective].
 */
interface TableGenMacroDirectiveEx : PsiElement {
    /**
     * Name of the macro defined ('#define') or tested ('#ifdef'/'#ifndef') by this directive.
     */
    val macroName: String?
}
