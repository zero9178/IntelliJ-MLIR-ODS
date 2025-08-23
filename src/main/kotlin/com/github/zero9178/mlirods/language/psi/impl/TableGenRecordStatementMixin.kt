package com.github.zero9178.mlirods.language.psi.impl

import com.github.zero9178.mlirods.language.generated.psi.TableGenClassRef
import com.github.zero9178.mlirods.language.generated.psi.TableGenDefvarStatement
import com.github.zero9178.mlirods.language.generated.psi.TableGenFieldBodyItem
import com.github.zero9178.mlirods.language.generated.psi.TableGenLetBodyItem
import com.github.zero9178.mlirods.language.psi.TableGenFieldIdentifierNode
import com.github.zero9178.mlirods.language.psi.TableGenFieldScopeNode
import com.github.zero9178.mlirods.language.psi.TableGenIdentifierElement
import com.github.zero9178.mlirods.language.psi.TableGenIdentifierScopeNode.IdMapEntry
import com.github.zero9178.mlirods.language.stubs.stubbedChildren
import com.intellij.extapi.psi.StubBasedPsiElementBase
import com.intellij.lang.ASTNode
import com.intellij.psi.stubs.IStubElementType
import com.intellij.psi.stubs.StubElement
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

    /**
     * Returns all id entries originating from the record body.
     */
    protected val bodyIdEntries: Sequence<IdMapEntry>
        get() = baseClassRefs.mapNotNull {
            it.referencedClass?.let { klass ->
                klass to it
            }
        }.flatMap { (klass, ref) ->
            klass.allFields.map {
                IdMapEntry(it, ref)
            }
        } + stubbedChildren<TableGenIdentifierElement>(
            TableGenDefvarStatement::class.java,
            TableGenFieldBodyItem::class.java,
        ).map(::IdMapEntry)

    private var myDirectFields = resettableLazy {
        directFieldAssignments.mapNotNull { (k, v) ->
            val field = v.asReversed().firstNotNullOfOrNull {
                it as? TableGenFieldBodyItem
            } ?: return@mapNotNull null
            k to field
        }.toMap()
    }

    override val directFields by myDirectFields

    private var myDirectFieldAssignments = resettableLazy {
        stubbedChildren<TableGenFieldIdentifierNode>(
            TableGenFieldBodyItem::class.java,
            TableGenLetBodyItem::class.java
        ).mapNotNull {
            val name = it.fieldName ?: return@mapNotNull null
            name to it
        }.groupBy({ it.first }) { it.second }
    }

    override val directFieldAssignments by myDirectFieldAssignments

    override val baseClassRefs: Sequence<TableGenClassRef>
        get() = getClassRefList().asSequence()

    override fun subtreeChanged() {
        super.subtreeChanged()
        myDirectFields.reset()
        myDirectFieldAssignments.reset()
    }
}