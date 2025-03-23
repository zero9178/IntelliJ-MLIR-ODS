package com.github.zero9178.mlirods.language.psi.impl

import com.github.zero9178.mlirods.language.generated.psi.TableGenClassRef
import com.github.zero9178.mlirods.language.generated.psi.TableGenFieldBodyItem
import com.github.zero9178.mlirods.language.psi.TableGenFieldScopeNode
import com.intellij.extapi.psi.StubBasedPsiElementBase
import com.intellij.lang.ASTNode
import com.intellij.psi.stubs.IStubElementType
import com.intellij.psi.stubs.StubElement

/**
 * Common base class for record functionality.
 */
abstract class TableGenRecordStatementMixin<StubT : StubElement<*>> : StubBasedPsiElementBase<StubT>,
    TableGenFieldScopeNode {
    private var myDirectFieldsCache: Map<String, TableGenFieldBodyItem>? = null

    protected abstract fun getClassRefList(): List<TableGenClassRef>
    protected abstract fun getFieldBodyItemList(): List<TableGenFieldBodyItem>

    constructor(node: ASTNode) : super(node)

    constructor(stub: StubT, stubType: IStubElementType<*, *>) : super(stub, stubType)


    override val directFields: Map<String, TableGenFieldBodyItem>
        get() {
            myDirectFieldsCache?.let { return it }

            val fields = getFieldBodyItemList().filter { it.name != null }.associateBy {
                it.name!!
            }
            myDirectFieldsCache = fields
            return fields
        }

    override val baseClassRefs: Sequence<TableGenClassRef>
        get() = getClassRefList().asSequence()

    override fun subtreeChanged() {
        super.subtreeChanged()
        myDirectFieldsCache = null
    }
}