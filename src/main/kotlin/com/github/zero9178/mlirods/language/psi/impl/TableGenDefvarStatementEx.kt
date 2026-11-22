package com.github.zero9178.mlirods.language.psi.impl

import com.github.zero9178.mlirods.language.generated.psi.TableGenDefvarStatement
import com.github.zero9178.mlirods.language.psi.TableGenTypedElement
import com.github.zero9178.mlirods.language.types.TableGenType
import com.github.zero9178.mlirods.language.types.TableGenUnknownType
import com.github.zero9178.mlirods.model.TableGenCompilationContext
import com.intellij.psi.PsiElement
import com.intellij.util.concurrency.annotations.RequiresBlockingContext
import com.intellij.util.concurrency.annotations.RequiresReadLock
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

/**
 * Interface used to inject methods into [TableGenDefvarStatement].
 */
interface TableGenDefvarStatementEx : PsiElement, TableGenTypedElement {

    /**
     * Returns the type of the value assigned to the 'defvar'.
     */
    override suspend fun type(context: TableGenCompilationContext): TableGenType {
        val value = (this as TableGenDefvarStatement).valueNode ?: return TableGenUnknownType
        // The value may again be an identifier referring to another 'defvar', and so on, to any length no matter how
        // deep the AST is. Launched, its type is computed from the bottom of the stack of some thread instead of on top
        // of ours.
        return coroutineScope { async { value.type(context) }.await() }
    }

    @RequiresReadLock
    @RequiresBlockingContext
    override fun typeBlocking(context: TableGenCompilationContext): TableGenType =
        (this as TableGenDefvarStatement).valueNode?.typeBlocking(context) ?: TableGenUnknownType
}
