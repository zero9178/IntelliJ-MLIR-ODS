package com.github.zero9178.mlirods.language.psi.impl

import com.github.zero9178.mlirods.language.generated.psi.TableGenFieldBodyItem
import com.github.zero9178.mlirods.language.psi.TableGenFieldAssignmentNode
import com.github.zero9178.mlirods.model.TableGenCompilationContext
import com.intellij.util.concurrency.annotations.RequiresReadLock

interface TableGenFieldBodyItemEx : TableGenFieldAssignmentNode {

    /**
     * Returns the defining [TableGenFieldBodyItem] of this field within [context].
     * The defining [TableGenFieldBodyItem] is defined as the one which first defined the field within the record
     * and therefore also determined its type.
     * Returns `this` if `this` is the defining [TableGenFieldBodyItem].
     */
    @RequiresReadLock
    fun definingFieldBodyItem(context: TableGenCompilationContext): TableGenFieldBodyItem
}
