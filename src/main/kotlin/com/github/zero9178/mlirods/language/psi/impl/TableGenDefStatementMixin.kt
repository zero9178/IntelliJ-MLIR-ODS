package com.github.zero9178.mlirods.language.psi.impl

import com.github.zero9178.mlirods.language.generated.psi.TableGenDefStatement
import com.github.zero9178.mlirods.language.generated.psi.TableGenFieldBodyItem
import com.github.zero9178.mlirods.language.generated.psi.TableGenIdentifierValue
import com.github.zero9178.mlirods.language.psi.TableGenFieldScopeNode
import com.github.zero9178.mlirods.language.psi.computeDirectFields
import com.github.zero9178.mlirods.language.stubs.impl.TableGenDefNameIdentifierStub
import com.intellij.extapi.psi.StubBasedPsiElementBase
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.IStubElementType

abstract class TableGenDefStatementMixin : StubBasedPsiElementBase<TableGenDefNameIdentifierStub>,
    TableGenDefStatement {

    private var myDirectFieldsCache: Map<String, TableGenFieldBodyItem>? = null

    constructor(node: ASTNode) : super(node)

    constructor(stub: TableGenDefNameIdentifierStub, stubType: IStubElementType<*, *>) : super(stub, stubType)

    override fun getNameIdentifier(): PsiElement? {
        return value as? TableGenIdentifierValue
    }

    override val directFields: Map<String, TableGenFieldBodyItem>
        get() {
            myDirectFieldsCache?.let { return it }

            val fields = computeDirectFields()
            myDirectFieldsCache = fields
            return fields
        }

    override val baseClasses: Sequence<TableGenFieldScopeNode>
        get() = classRefList.asSequence().mapNotNull {
            it.reference?.resolve() as? TableGenFieldScopeNode
        }

    override fun subtreeChanged() {
        super.subtreeChanged()
        myDirectFieldsCache = null
    }
}
