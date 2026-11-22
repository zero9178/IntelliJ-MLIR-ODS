package com.github.zero9178.mlirods.language.psi.impl

import com.github.zero9178.mlirods.language.generated.psi.TableGenFieldBodyItem
import com.github.zero9178.mlirods.language.psi.TableGenFieldRedefinitionReference
import com.github.zero9178.mlirods.language.psi.createIdentifier
import com.github.zero9178.mlirods.language.stubs.impl.TableGenFieldBodyItemStub
import com.github.zero9178.mlirods.language.types.TableGenType
import com.github.zero9178.mlirods.model.TableGenCompilationContext
import com.intellij.extapi.psi.StubBasedPsiElementBase
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.IStubElementType

abstract class TableGenFieldBodyItemMixin : StubBasedPsiElementBase<TableGenFieldBodyItemStub>,
    TableGenFieldBodyItem {

    constructor(node: ASTNode) : super(node)

    constructor(stub: TableGenFieldBodyItemStub, stubType: IStubElementType<*, *>) : super(stub, stubType)

    override fun getName(): String? {
        greenStub?.let { return it.name }

        return nameIdentifier?.text
    }

    override fun setName(name: String): PsiElement {
        fieldIdentifier?.replace(createIdentifier(project, name))
        return this
    }

    override fun getNameIdentifier(): PsiElement? {
        return fieldIdentifier
    }

    override val fieldName: String?
        get() = name

    override val assignedValueNode get() = valueNode

    /**
     * Returns the declared type.
     */
    override suspend fun type(context: TableGenCompilationContext) = typeBlocking(context)

    override fun typeBlocking(context: TableGenCompilationContext): TableGenType = typeNode.toType()

    override fun getTextOffset(): Int {
        return nameIdentifier?.textOffset ?: super.getTextOffset()
    }

    override fun getReference() = TableGenFieldRedefinitionReference(this)

    override fun referencedDefinitionBlocking(context: TableGenCompilationContext) =
        TableGenFieldRedefinitionReference.findRedefinedFieldBlocking(this, context)
}