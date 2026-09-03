package com.github.zero9178.mlirods.language.psi.impl

import com.github.zero9178.mlirods.language.generated.psi.TableGenDefStatement
import com.github.zero9178.mlirods.language.generated.psi.TableGenIdentifierValueNode
import com.github.zero9178.mlirods.language.psi.TableGenRecord
import com.github.zero9178.mlirods.language.stubs.impl.TableGenDefStatementStub
import com.github.zero9178.mlirods.model.getProjectContextDependentCache
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.IStubElementType

abstract class TableGenDefStatementMixin : TableGenRecordStatementMixin<TableGenDefStatementStub>,
    TableGenDefStatement, PsiElement {

    constructor(node: ASTNode) : super(node)

    constructor(stub: TableGenDefStatementStub, stubType: IStubElementType<*, *>) : super(stub, stubType)

    override fun getNameIdentifier(): PsiElement? {
        return valueNode as? TableGenIdentifierValueNode
    }

    // Note: [bodyIdEntries] resolves base classes and must therefore be cached per resolution context, not just per
    // subtree.
    override val directIdMap
        get() = getProjectContextDependentCache(this) { record ->
            record.bodyIdEntries.mapNotNull {
                val name = it.element.name ?: return@mapNotNull null
                name to it
            }.groupBy({
                it.first
            }) {
                it.second
            }
        }

    override val mostDerivedRecords: Sequence<TableGenRecord>
        get() = sequenceOf(this)
}
