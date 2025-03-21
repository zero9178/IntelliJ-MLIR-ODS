package com.github.zero9178.mlirods.language.stubs

import com.github.zero9178.mlirods.language.TableGenLanguage
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.*

/**
 * Base class of all stub element types used within TableGen.
 */
abstract class TableGenStubElementType<StubT : StubElement<*>, PsiT : PsiElement>(
    debugName: String, private val constructor: (StubT, TableGenStubElementType<StubT, PsiT>) -> PsiT
) : IStubElementType<StubT, PsiT>(debugName, TableGenLanguage.INSTANCE) {

    final override fun createPsi(stub: StubT): PsiT? {
        return constructor.invoke(stub, this)
    }

    override fun getExternalId(): String {
        return "tablegen." + toString()
    }

    override fun indexStub(stub: StubT, sink: IndexSink) {}
}