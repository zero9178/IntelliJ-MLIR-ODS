package com.github.zero9178.mlirods.language.stubs.impl

import com.github.zero9178.mlirods.language.stubs.TableGenStubElementType
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.EmptyStubSerializer
import com.intellij.psi.stubs.IStubElementType
import com.intellij.psi.stubs.StubBase
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream

/**
 * Abstract base class of stub file types which do not contain any data.
 */
abstract class TableGenSingletonStubElementType<StubT : StubElement<*>, PsiT : PsiElement>(
    debugName: String,
    psiConstructor: (StubT, TableGenStubElementType<StubT, PsiT>) -> PsiT,
    private val myStubConstructor: (StubElement<*>?, IStubElementType<*, *>) -> StubT
) : TableGenStubElementType<StubT, PsiT>(
    debugName, psiConstructor
), EmptyStubSerializer<StubT> {
    final override fun createStub(
        psi: PsiT, parentStub: StubElement<out PsiElement?>?
    ): StubT {
        return myStubConstructor.invoke(parentStub, this)
    }

    override fun instantiate(parentStub: StubElement<*>?): StubT {
        return myStubConstructor.invoke(parentStub, this)
    }

    override fun isAlwaysLeaf(root: StubBase<*>) = true
}