package com.github.zero9178.mlirods.language.psi.impl

import com.github.zero9178.mlirods.language.generated.psi.TableGenDefStatement
import com.github.zero9178.mlirods.language.generated.psi.TableGenMulticlassStatement
import com.github.zero9178.mlirods.language.psi.TableGenIdentifierElement
import com.github.zero9178.mlirods.language.psi.TableGenIdentifierScopeNode
import com.github.zero9178.mlirods.language.psi.createIdentifier
import com.github.zero9178.mlirods.language.stubs.impl.TableGenMulticlassStatementStub
import com.github.zero9178.mlirods.language.stubs.stubbedChildren
import com.github.zero9178.mlirods.model.TableGenCompilationContext
import com.intellij.extapi.psi.StubBasedPsiElementBase
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.IStubElementType
import com.intellij.util.resettableLazy

abstract class TableGenMulticlassStatementMixin : StubBasedPsiElementBase<TableGenMulticlassStatementStub>,
    TableGenMulticlassStatement {

    constructor(node: ASTNode) : super(node)

    constructor(stub: TableGenMulticlassStatementStub, stubType: IStubElementType<*, *>) : super(stub, stubType)

    override fun toString(): String = TableGenPsiImplUtil.toString(this)

    override fun getName(): String? {
        greenStub?.let { return it.name }

        return identifier?.text
    }

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

    override fun directIdMap(context: TableGenCompilationContext) = myDirectIdMap.value

    override fun subtreeChanged() {
        super.subtreeChanged()
        myDirectIdMap.reset()
    }
}
