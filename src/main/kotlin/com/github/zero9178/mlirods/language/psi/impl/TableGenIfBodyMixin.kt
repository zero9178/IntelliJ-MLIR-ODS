package com.github.zero9178.mlirods.language.psi.impl

import com.github.zero9178.mlirods.language.generated.psi.TableGenIfBody
import com.github.zero9178.mlirods.language.psi.TableGenIdentifierElement
import com.github.zero9178.mlirods.language.psi.TableGenIdentifierScopeNode
import com.github.zero9178.mlirods.language.stubs.impl.TableGenStatementStub
import com.github.zero9178.mlirods.language.stubs.stubbedChildren
import com.intellij.extapi.psi.StubBasedPsiElementBase
import com.intellij.lang.ASTNode
import com.intellij.psi.stubs.IStubElementType
import com.intellij.util.resettableLazy

abstract class TableGenIfBodyMixin : StubBasedPsiElementBase<TableGenStatementStub>,
    TableGenIfBody {

    constructor(node: ASTNode) : super(node)

    constructor(stub: TableGenStatementStub, stubType: IStubElementType<*, *>) : super(stub, stubType)

    private var myDirectIdMap = resettableLazy {
        stubbedChildren<TableGenIdentifierElement>().mapNotNull {
            val name = it.name ?: return@mapNotNull null
            name to it
        }.groupBy({
            it.first
        }) {
            TableGenIdentifierScopeNode.IdMapEntry(it.second)
        }
    }

    override val directIdMap by myDirectIdMap

    override fun subtreeChanged() {
        super.subtreeChanged()
        myDirectIdMap.reset()
    }
}