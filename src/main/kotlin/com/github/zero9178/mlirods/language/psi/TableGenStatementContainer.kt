package com.github.zero9178.mlirods.language.psi

import com.github.zero9178.mlirods.language.stubs.stubbedChildren
import com.intellij.extapi.psi.StubBasedPsiElementBase
import com.intellij.psi.PsiElement

/**
 * Interface implemented by every [PsiElement] whose children are statements rather than body items, e.g. files and
 * the bodies of 'let', 'foreach' or 'multiclass'.
 */
interface TableGenStatementContainer : PsiElement {

    /**
     * The children of this, taken from the stub if there is one. Besides the statements, these may contain the other
     * parts of this, such as the items of a 'let'.
     */
    val statements: Sequence<PsiElement>
        get() = (this as? StubBasedPsiElementBase<*>)?.stubbedChildren<PsiElement>() ?: children.asSequence()
}

/**
 * Returns all statements nested within this in pre-order, i.e. in the order they appear in the source, including the
 * statements of nested [TableGenStatementContainer]s, which directly follow the container itself. Nested containers
 * for which [descendInto] returns false are returned without their statements.
 */
fun TableGenStatementContainer.nestedStatements(
    descendInto: (TableGenStatementContainer) -> Boolean,
): Sequence<PsiElement> = sequence {
    val stack = ArrayDeque<Iterator<PsiElement>>()
    stack.addLast(statements.iterator())
    while (stack.isNotEmpty()) {
        val top = stack.last()
        if (!top.hasNext()) {
            stack.removeLast()
            continue
        }
        val next = top.next()
        yield(next)
        if (next is TableGenStatementContainer && descendInto(next)) stack.addLast(next.statements.iterator())
    }
}
