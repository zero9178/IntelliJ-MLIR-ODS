package com.github.zero9178.mlirods.language.psi.impl

import com.github.zero9178.mlirods.language.generated.psi.TableGenFieldAccessValueNode
import com.github.zero9178.mlirods.language.stubs.impl.TableGenValueNodeStub
import com.github.zero9178.mlirods.language.values.TableGenRecordValue
import com.github.zero9178.mlirods.language.values.TableGenUnknownValue
import com.github.zero9178.mlirods.language.values.TableGenValue
import com.intellij.extapi.psi.StubBasedPsiElementBase
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.IStubElementType
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

abstract class TableGenFieldAccessValueNodeMixin : StubBasedPsiElementBase<TableGenValueNodeStub>,
    TableGenFieldAccessValueNode, PsiElement {

    constructor(node: ASTNode) : super(node)

    constructor(
        stub: TableGenValueNodeStub,
        stubType: IStubElementType<*, *>
    ) : super(stub, stubType)

    override suspend fun evaluateInner(context: TableGenEvaluationContext): TableGenValue {
        val record = valueNode.evaluate(context) as? TableGenRecordValue ?: return TableGenUnknownValue
        val fieldName = fieldName ?: return TableGenUnknownValue
        // The field is a value of another record, whose value may be a field of yet another record, and so on, to any
        // length. Launched, it is computed from the bottom of the stack of some thread instead of on top of ours.
        return coroutineScope { async { record.fields[fieldName] }.await() }
    }
}
