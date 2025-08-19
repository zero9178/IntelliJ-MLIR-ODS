package com.github.zero9178.mlirods.language.psi

import com.intellij.extapi.psi.StubBasedPsiElementBase
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.StubBase
import com.intellij.psi.util.startOffset

/**
 * Performs a lexicographical comparison of 'this' with 'other'.
 * Returns null if the elements are in different files.
 */
fun PsiElement.compareTo(other: PsiElement): Int? {
    if (this is StubBasedPsiElementBase<*> && other is StubBasedPsiElementBase<*>) {
        val stub = this.stub as? StubBase
        if (stub != null) {
            val otherStub = other.stub as? StubBase ?: return null
            if (stub.containingFileStub != otherStub.containingFileStub)
                return null
            return stub.compareByOrderWith(otherStub)
        }
    }
    if (containingFile != other.containingFile)
        return null

    return startOffset.compareTo(other.startOffset)
}