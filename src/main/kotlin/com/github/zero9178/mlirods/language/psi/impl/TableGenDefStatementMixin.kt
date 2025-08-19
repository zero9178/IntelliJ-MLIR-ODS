package com.github.zero9178.mlirods.language.psi.impl

import com.github.zero9178.mlirods.language.generated.psi.TableGenDefStatement
import com.github.zero9178.mlirods.language.generated.psi.TableGenIdentifierValueNode
import com.github.zero9178.mlirods.language.stubs.impl.TableGenIdentifierElementStub
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.IStubElementType
import com.intellij.util.resettableLazy

abstract class TableGenDefStatementMixin : TableGenRecordStatementMixin<TableGenIdentifierElementStub>,
    TableGenDefStatement, PsiElement {

    constructor(node: ASTNode) : super(node)

    constructor(stub: TableGenIdentifierElementStub, stubType: IStubElementType<*, *>) : super(stub, stubType)

    override fun getNameIdentifier(): PsiElement? {
        return valueNode as? TableGenIdentifierValueNode
    }

    private var myDirectIdMap = resettableLazy {
        bodyIdEntries.mapNotNull {
            val name = it.element.name ?: return@mapNotNull null
            name to it
        }.groupBy({
            it.first
        }) {
            it.second
        }
    }

    override val directIdMap by myDirectIdMap

    override fun subtreeChanged() {
        super.subtreeChanged()
        myDirectIdMap.reset()
    }
}
