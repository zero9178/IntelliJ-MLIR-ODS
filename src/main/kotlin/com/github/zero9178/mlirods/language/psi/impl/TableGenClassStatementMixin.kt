package com.github.zero9178.mlirods.language.psi.impl

import com.github.zero9178.mlirods.language.generated.psi.TableGenClassStatement
import com.github.zero9178.mlirods.language.psi.TableGenIdentifierScopeNode.IdMapEntry
import com.github.zero9178.mlirods.language.psi.createIdentifier
import com.github.zero9178.mlirods.language.stubs.impl.TableGenClassStatementStub
import com.github.zero9178.mlirods.language.stubs.stubbedChildren
import com.github.zero9178.mlirods.model.getProjectContextDependentCache
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.IStubElementType
import com.intellij.util.takeWhileInclusive

abstract class TableGenClassStatementMixin : TableGenRecordStatementMixin<TableGenClassStatementStub>,
    TableGenClassStatement, PsiElement {

    constructor(node: ASTNode) : super(node)

    constructor(stub: TableGenClassStatementStub, stubType: IStubElementType<*, *>) : super(stub, stubType)

    override fun getName(): String? {
        greenStub?.let { return it.name }

        return nameIdentifier?.text
    }

    override fun setName(name: String): PsiElement {
        nameIdentifier?.replace(createIdentifier(project, name))
        return this
    }

    override fun getNameIdentifier(): PsiElement? {
        return identifier
    }

    override fun getTextOffset(): Int {
        return nameIdentifier?.textOffset ?: super.getTextOffset()
    }

    override fun classStatementsBefore(withSelf: Boolean): Sequence<TableGenClassStatement> {
        greenStub?.let { stub ->
            return stub.parentStub.stubbedChildren<TableGenClassStatement>().takeWhileInclusive {
                it !== this
            }.toList().asReversed().asSequence().drop(if (withSelf) 0 else 1)
        }

        return super.classStatementsBefore(withSelf)
    }

    // Note: [bodyIdEntries] resolves base classes and must therefore be cached per resolution context, not just per
    // subtree.
    override val directIdMap
        get() = getProjectContextDependentCache(this) { klass ->
            (klass.templateArgDeclList.asSequence().map(::IdMapEntry) + klass.bodyIdEntries).mapNotNull {
                val name = it.element.name ?: return@mapNotNull null
                name to it
            }.groupBy({
                it.first
            }) {
                it.second
            }
        }
}