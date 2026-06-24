package com.github.zero9178.mlirods.language.psi.impl

import com.github.zero9178.mlirods.language.generated.psi.TableGenFieldBodyItem
import com.github.zero9178.mlirods.language.psi.TableGenFieldAssignmentNode

interface TableGenFieldBodyItemEx : TableGenFieldAssignmentNode {

    /**
     * Returns the defining [TableGenFieldBodyItem] of this field.
     * The defining [TableGenFieldBodyItem] is defined as the one which first defined the field within the record
     * and therefore also determined its type.
     * Returns `this` if `this` is the defining [TableGenFieldBodyItem].
     */
    val definingFieldBodyItem: TableGenFieldBodyItem
}
