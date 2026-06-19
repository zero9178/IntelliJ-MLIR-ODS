package com.github.zero9178.mlirods.language.psi.impl

import com.github.zero9178.mlirods.language.generated.psi.TableGenArgValueItem
import com.github.zero9178.mlirods.language.generated.psi.TableGenTemplateArgDecl
import com.github.zero9178.mlirods.language.generated.psi.TableGenValueNode
import com.github.zero9178.mlirods.language.psi.TableGenArgValueItemReference
import com.github.zero9178.mlirods.language.stubs.impl.TableGenArgValueItemStub
import com.intellij.extapi.psi.StubBasedPsiElementBase
import com.intellij.lang.ASTNode
import com.intellij.psi.stubs.IStubElementType

abstract class TableGenArgValueItemMixin : StubBasedPsiElementBase<TableGenArgValueItemStub>, TableGenArgValueItem {

    constructor(node: ASTNode) : super(node)

    constructor(stub: TableGenArgValueItemStub, stubType: IStubElementType<*, *>) : super(stub, stubType)

    override fun getReference() =
        // Only allow navigation in the UI if it is a named argument.
        if (isNamedArgument) TableGenArgValueItemReference(this) else null

    override val isNamedArgument: Boolean
        get() = greenStub?.isNamedArgument ?: (equalsSign != null)

    override val nameNode: TableGenValueNode?
        get() = if (isNamedArgument) valueNodeList.firstOrNull() else null

    override val valueNode: TableGenValueNode?
        get() = if (isNamedArgument) valueNodeList.getOrNull(1) else valueNodeList.firstOrNull()

    override val referencedTemplateArgDecl: TableGenTemplateArgDecl?
        get() = TableGenArgValueItemReference(this).resolve() as? TableGenTemplateArgDecl

    override val identifierName: String?
        get() {
            greenStub?.let {
                return it.identifier
            }

            return identifier?.text
        }
}
