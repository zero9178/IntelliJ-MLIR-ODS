package com.github.zero9178.mlirods.language.psi.impl

import com.github.zero9178.mlirods.language.generated.TableGenTypes
import com.github.zero9178.mlirods.language.generated.psi.TableGenClassRef
import com.github.zero9178.mlirods.language.generated.psi.TableGenFieldBodyItem
import com.github.zero9178.mlirods.language.generated.psi.TableGenLetBodyItem
import com.github.zero9178.mlirods.language.generated.psi.TableGenValueNode
import com.github.zero9178.mlirods.language.psi.TableGenFieldIdentifierNode
import com.github.zero9178.mlirods.language.psi.TableGenFieldScopeNode
import com.intellij.extapi.psi.StubBasedPsiElementBase
import com.intellij.lang.ASTNode
import com.intellij.psi.stubs.IStubElementType
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.tree.TokenSet
import com.intellij.util.resettableLazy

/**
 * Common base class for record functionality.
 */
abstract class TableGenRecordStatementMixin<StubT : StubElement<*>> : StubBasedPsiElementBase<StubT>,
    TableGenFieldScopeNode {

    protected abstract fun getClassRefList(): List<TableGenClassRef>
    protected abstract fun getFieldBodyItemList(): List<TableGenFieldBodyItem>

    constructor(node: ASTNode) : super(node)

    constructor(stub: StubT, stubType: IStubElementType<*, *>) : super(stub, stubType)

    private val myDirectFields = resettableLazy {
        getFieldBodyItemList().filter { it.name != null }.associateBy {
            it.name!!
        }
    }

    override val directFields by myDirectFields

    override val baseClassRefs: Sequence<TableGenClassRef>
        get() = getClassRefList().asSequence()

    private val myDirectFieldExpressions = resettableLazy {
        val result = mutableMapOf<String, TableGenValueNode>()
        getStubOrPsiChildren(
            TokenSet.create(
                TableGenTypes.FIELD_BODY_ITEM,
                TableGenTypes.LET_BODY_ITEM
            )
        ) {
            arrayOfNulls<TableGenFieldIdentifierNode>(it)
        }.toList().asReversed().asSequence().filterNotNull().forEach {
            val name = it.fieldName ?: return@forEach
            val value = when (it) {
                is TableGenFieldBodyItem -> it.valueNode ?: return@forEach
                // TODO: This completely ignores the bitlist and whatnot.
                is TableGenLetBodyItem -> it.valueNode ?: return@forEach
                else -> return@forEach
            }
            result.putIfAbsent(name, value)
        }
        result
    }

    override val directFieldExpressions: Map<String, TableGenValueNode> by myDirectFieldExpressions

    override fun subtreeChanged() {
        super.subtreeChanged()
        myDirectFields.reset()
        myDirectFieldExpressions.reset()
    }
}