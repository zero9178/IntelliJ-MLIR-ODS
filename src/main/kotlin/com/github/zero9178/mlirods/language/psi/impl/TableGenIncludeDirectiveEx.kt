package com.github.zero9178.mlirods.language.psi.impl

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.util.concurrency.annotations.RequiresReadLock

/**
 * Interface used to add extra methods to [com.github.zero9178.mlirods.language.generated.psi.TableGenIncludeDirective].
 */
interface TableGenIncludeDirectiveEx : PsiElement {
    val includeSuffix: String

    val includedFile: VirtualFile?
        @RequiresReadLock
        get
}

