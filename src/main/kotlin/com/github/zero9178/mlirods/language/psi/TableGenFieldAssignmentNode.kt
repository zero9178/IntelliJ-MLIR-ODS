package com.github.zero9178.mlirods.language.psi

import com.github.zero9178.mlirods.language.generated.psi.TableGenValueNode

/**
 * A [TableGenFieldIdentifierNode] that assigns a value to a field, as opposed to merely referencing one. Examples are a
 * field body item (`int x = 5;`) or a `let` item (`let x = 5`).
 */
interface TableGenFieldAssignmentNode : TableGenFieldIdentifierNode {
    /**
     * Returns the value node assigned to the field, or null if it has none (e.g. due to an error in the AST).
     */
    val assignedValueNode: TableGenValueNode?
}
