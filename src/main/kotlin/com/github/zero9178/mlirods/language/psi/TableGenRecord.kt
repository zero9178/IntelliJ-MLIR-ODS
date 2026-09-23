package com.github.zero9178.mlirods.language.psi

import com.github.zero9178.mlirods.model.TableGenCompilationContext
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.util.concurrency.annotations.RequiresReadLock

/**
 * Common base interface for all kinds of records. This includes both 'def' statements and class statements.
 */
interface TableGenRecord : TableGenFieldScopeNode, PsiNameIdentifierOwner {
    /**
     * Returns a list of the most derived records of 'this' within [context]. The most derived record is defined as
     * any 'class' or 'def' which isn't further inherited and may include 'this'.
     * Since 'def's cannot be inherited from they're always returned by the method.
     */
    @RequiresReadLock
    fun mostDerivedRecords(context: TableGenCompilationContext): Sequence<TableGenRecord>
}
