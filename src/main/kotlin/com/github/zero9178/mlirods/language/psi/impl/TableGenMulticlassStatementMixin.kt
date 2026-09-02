package com.github.zero9178.mlirods.language.psi.impl

import com.github.zero9178.mlirods.language.generated.psi.TableGenDefStatement
import com.github.zero9178.mlirods.language.generated.psi.TableGenMulticlassStatement
import com.github.zero9178.mlirods.language.psi.TableGenIdentifierElement
import com.github.zero9178.mlirods.language.psi.TableGenIdentifierScopeNode
import com.github.zero9178.mlirods.language.psi.createIdentifier
import com.github.zero9178.mlirods.language.stubs.impl.TableGenStatementStub
import com.github.zero9178.mlirods.language.stubs.stubbedChildren
import com.intellij.extapi.psi.StubBasedPsiElementBase
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.IStubElementType
import com.intellij.util.resettableLazy

abstract class TableGenMulticlassStatementMixin : StubBasedPsiElementBase<TableGenStatementStub>,
    TableGenMulticlassStatement {

    constructor(node: ASTNode) : super(node)

    constructor(stub: TableGenStatementStub, stubType: IStubElementType<*, *>) : super(stub, stubType)

    override fun toString(): String = TableGenPsiImplUtil.toString(this)

    /**
     * The name of a multiclass is not stubbed as multiclasses live in a namespace of their own that is not part of
     * any id map. It is therefore never required during resolution, only for presentation.
     */
    override fun getName(): String? = identifier?.text

    override fun getNameIdentifier(): PsiElement? = identifier

    override fun setName(name: String): PsiElement {
        nameIdentifier?.replace(createIdentifier(project, name))
        return this
    }

    override fun getTextOffset(): Int = nameIdentifier?.textOffset ?: super.getTextOffset()

    private var myDirectIdMap = resettableLazy {
        // 'def's within a multiclass are only instantiated by 'defm's with 'NAME' prepended to their names.
        // They can therefore never be found by identifier lookup, only via '!cast'.
        stubbedChildren<TableGenIdentifierElement>().filter { it !is TableGenDefStatement }.mapNotNull {
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
