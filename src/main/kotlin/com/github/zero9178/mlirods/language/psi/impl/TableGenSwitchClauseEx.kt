package com.github.zero9178.mlirods.language.psi.impl

import com.github.zero9178.mlirods.language.generated.TableGenTypes
import com.intellij.psi.PsiElement

/**
 * Interface used to inject methods into [com.github.zero9178.mlirods.language.generated.psi.TableGenSwitchClause].
 */
interface TableGenSwitchClauseEx : PsiElement {

    /**
     * Returns whether this clause is a `case : value` pair rather than the trailing default value.
     * A clause with a `:` carries a case value, while the default clause consists of a single value only.
     */
    val hasColon: Boolean
        get() = node.findChildByType(TableGenTypes.COLON) != null
}
