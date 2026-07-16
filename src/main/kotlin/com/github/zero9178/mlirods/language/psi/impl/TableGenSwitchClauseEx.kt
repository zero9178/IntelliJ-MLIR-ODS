package com.github.zero9178.mlirods.language.psi.impl

import com.github.zero9178.mlirods.language.generated.TableGenTypes
import com.github.zero9178.mlirods.language.stubs.impl.TableGenSwitchClauseStub
import com.intellij.psi.PsiElement

/**
 * Interface used to inject methods into [com.github.zero9178.mlirods.language.generated.psi.TableGenSwitchClause].
 */
interface TableGenSwitchClauseEx : PsiElement {

    val stub: TableGenSwitchClauseStub?

    /**
     * Returns whether this clause is a `case : value` pair rather than the trailing default value.
     * A clause with a `:` carries a case value, while the default clause consists of a single value only.
     */
    val hasColon: Boolean
        get() = stub?.hasColon ?: (node.findChildByType(TableGenTypes.COLON) != null)
}
