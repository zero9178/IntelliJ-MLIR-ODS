package com.github.zero9178.mlirods.language.psi.impl

import com.github.zero9178.mlirods.MyIcons
import com.github.zero9178.mlirods.language.generated.psi.TableGenClassRef
import com.github.zero9178.mlirods.language.generated.psi.TableGenClassStatement
import com.github.zero9178.mlirods.language.generated.psi.TableGenFieldBodyItem
import com.github.zero9178.mlirods.language.psi.computeDirectFields
import com.github.zero9178.mlirods.language.psi.createIdentifier
import com.github.zero9178.mlirods.language.stubs.impl.TableGenClassStatementStub
import com.intellij.extapi.psi.StubBasedPsiElementBase
import com.intellij.lang.ASTNode
import com.intellij.navigation.ItemPresentation
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.IStubElementType

abstract class TableGenClassStatementMixin : StubBasedPsiElementBase<TableGenClassStatementStub>,
    TableGenClassStatement {

    private var myDirectFieldsCache: Map<String, TableGenFieldBodyItem>? = null

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

    override fun getPresentation(): ItemPresentation {
        return object : ItemPresentation {
            override fun getPresentableText() = name

            override fun getIcon(unused: Boolean) = MyIcons.TableGenIcon
        }
    }

    override val directFields: Map<String, TableGenFieldBodyItem>
        get() {
            myDirectFieldsCache?.let { return it }

            val fields = computeDirectFields()
            myDirectFieldsCache = fields
            return fields
        }

    override val baseClassRefs: Sequence<TableGenClassRef>
        get() = classRefList.asSequence()

    override fun subtreeChanged() {
        super.subtreeChanged()
        myDirectFieldsCache = null
    }
}