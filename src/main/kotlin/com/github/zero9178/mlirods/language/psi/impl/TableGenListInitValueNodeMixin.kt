package com.github.zero9178.mlirods.language.psi.impl

import com.github.zero9178.mlirods.language.generated.psi.TableGenListInitValueNode
import com.github.zero9178.mlirods.language.stubs.impl.TableGenValueNodeStub
import com.github.zero9178.mlirods.language.types.commonType
import com.github.zero9178.mlirods.language.values.TableGenListValue
import com.github.zero9178.mlirods.language.values.TableGenValue
import com.intellij.extapi.psi.StubBasedPsiElementBase
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.IStubElementType
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

abstract class TableGenListInitValueNodeMixin : StubBasedPsiElementBase<TableGenValueNodeStub>,
    TableGenListInitValueNode, PsiElement {

    constructor(node: ASTNode) : super(node)

    constructor(
        stub: TableGenValueNodeStub, stubType: IStubElementType<*, *>
    ) : super(stub, stubType)

    override suspend fun evaluateInner(context: TableGenEvaluationContext): TableGenValue {
        // An element that cannot be evaluated remains unknown within the list, keeping the others and the length known.
        val elements = coroutineScope { valueNodeList.map { async { it.evaluate(context) } }.awaitAll() }
        // Without an explicit element type, the element type is the one all elements can be used as.
        return TableGenListValue(elements, typeNode?.toType() ?: elements.map { it.type }.commonType())
    }
}
