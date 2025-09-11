package com.github.zero9178.mlirods.language.psi

import com.intellij.psi.PsiNameIdentifierOwner

/**
 * Common base interface for all kinds of records. This includes both 'def' statements and class statements.
 */
interface TableGenRecord : TableGenFieldScopeNode, PsiNameIdentifierOwner {
    /**
     * Returns a list of the most derived records of 'this'. The most derived record is defined as any 'class' or 'def'
     * which isn't further inherited and may include 'this'.
     * Since 'def's cannot be inherited from they're always returned by the method.
     */
    val mostDerivedRecords: Sequence<TableGenRecord>
}
