package com.github.zero9178.mlirods.language.psi.impl

import com.github.zero9178.mlirods.language.generated.psi.TableGenTemplateArgDecl
import com.github.zero9178.mlirods.language.stubs.impl.TableGenTemplateArgDeclStub
import com.intellij.extapi.psi.StubBasedPsiElementBase
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.IStubElementType

abstract class TableGenTemplateArgDeclMixin : StubBasedPsiElementBase<TableGenTemplateArgDeclStub>,
    TableGenTemplateArgDecl, PsiElement {

    constructor(node: ASTNode) : super(node)

    constructor(stub: TableGenTemplateArgDeclStub, stubType: IStubElementType<*, *>) : super(stub, stubType)

    override fun getName(): String? {
        stub?.let {
            return it.name
        }
        return nameIdentifier?.text
    }
}