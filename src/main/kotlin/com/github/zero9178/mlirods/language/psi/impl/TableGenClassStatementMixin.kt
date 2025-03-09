package com.github.zero9178.mlirods.language.psi.impl

import com.github.zero9178.mlirods.MyIcons
import com.github.zero9178.mlirods.language.generated.psi.TableGenClassStatement
import com.github.zero9178.mlirods.language.stubs.impl.TableGenClassStatementStub
import com.intellij.extapi.psi.StubBasedPsiElementBase
import com.intellij.lang.ASTNode
import com.intellij.navigation.ItemPresentation
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.IStubElementType

abstract class TableGenClassStatementMixin : StubBasedPsiElementBase<TableGenClassStatementStub>,
    TableGenClassStatement {

    constructor(node: ASTNode) : super(node)

    constructor(stub: TableGenClassStatementStub, stubType: IStubElementType<*, *>) : super(stub, stubType)

    override fun getName(): String? {
        greenStub?.let { return it.name }

        return nameIdentifier?.text
    }

    override fun setName(name: String): PsiElement {
        TODO("not implemented")
    }

    override fun getNameIdentifier(): PsiElement? {
        return identifier
    }

    override fun getTextOffset(): Int {
        return nameIdentifier?.textOffset ?: super.getTextOffset()
    }

    override fun getPresentation(): ItemPresentation {
        return object : ItemPresentation {
            override fun getPresentableText() = name

            override fun getIcon(unused: Boolean) = MyIcons.TableGenIcon
        }
    }
}