package com.github.zero9178.mlirods.language.psi.impl

import com.github.zero9178.mlirods.language.stubs.impl.TableGenMultiClassRefStub
import com.intellij.psi.PsiElement

/**
 * Interface used to inject methods into [com.github.zero9178.mlirods.language.generated.psi.TableGenMultiClassRef].
 */
interface TableGenMultiClassRefEx : PsiElement {

    val stub: TableGenMultiClassRefStub?

    val identifier: PsiElement

    /**
     * Returns the name of the multiclass or, within a 'defm', the class being referenced.
     */
    val className: String
        get() = stub?.name ?: identifier.text
}
