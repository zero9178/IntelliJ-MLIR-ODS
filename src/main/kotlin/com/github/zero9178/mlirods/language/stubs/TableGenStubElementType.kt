package com.github.zero9178.mlirods.language.stubs

import com.github.zero9178.mlirods.language.TableGenLanguage
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.*

/**
 * Base class of all stub element types used within TableGen.
 */
abstract class TableGenStubElementType<StubT : StubElement<PsiT>, PsiT : PsiElement>(
    debugName: String
) : IStubElementType<StubT, PsiT>(debugName, TableGenLanguage.INSTANCE) {

    override fun getExternalId(): String {
        return "tablegen." + toString()
    }
}