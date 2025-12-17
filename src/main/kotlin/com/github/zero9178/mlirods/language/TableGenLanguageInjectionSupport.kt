package com.github.zero9178.mlirods.language

import com.github.zero9178.mlirods.language.generated.psi.TableGenBlockStringValue
import com.github.zero9178.mlirods.language.patterns.TableGenPatterns
import com.intellij.openapi.util.NlsSafe
import com.intellij.psi.PsiLanguageInjectionHost
import org.intellij.plugins.intelliLang.inject.AbstractLanguageInjectionSupport

private class TableGenLanguageInjectionSupport : AbstractLanguageInjectionSupport() {
    override fun getId(): @NlsSafe String = TableGenLanguage.INSTANCE.id

    override fun getPatternClasses(): Array<out Class<*>?> {
        return arrayOf(TableGenPatterns::class.java)
    }

    override fun isApplicableTo(host: PsiLanguageInjectionHost?): Boolean {
        return host is TableGenBlockStringValue
    }

    override fun useDefaultInjector(host: PsiLanguageInjectionHost?) = true
}