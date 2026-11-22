package com.github.zero9178.mlirods.language.psi.impl

import com.github.zero9178.mlirods.language.generated.TableGenTypes
import com.github.zero9178.mlirods.language.stubs.impl.TableGenForeachIteratorStub
import com.intellij.psi.PsiElement

/**
 * Interface used to inject methods into [com.github.zero9178.mlirods.language.generated.psi.TableGenForeachIterator].
 */
interface TableGenForeachIteratorEx : PsiElement {

    val stub: TableGenForeachIteratorStub?

    /**
     * Returns whether this iterator iterates over a range list in braces, e.g. `foreach i = {0-3, 5} in`, rather than
     * over a single range or a value.
     */
    val isRangeList: Boolean
        get() = stub?.isRangeList ?: (node.findChildByType(TableGenTypes.LBRACE) != null)
}
