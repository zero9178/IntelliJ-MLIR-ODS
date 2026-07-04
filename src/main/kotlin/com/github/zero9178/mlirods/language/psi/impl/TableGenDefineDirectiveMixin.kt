package com.github.zero9178.mlirods.language.psi.impl

import com.github.zero9178.mlirods.language.generated.psi.TableGenDefineDirective
import com.github.zero9178.mlirods.language.psi.createIdentifier
import com.github.zero9178.mlirods.language.stubs.impl.TableGenMacroDirectiveStub
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.IStubElementType

/**
 * Mixin for the '#define' directive.
 */
abstract class TableGenDefineDirectiveMixin : TableGenMacroDirectiveMixin, TableGenDefineDirective {

    constructor(node: ASTNode) : super(node)

    constructor(stub: TableGenMacroDirectiveStub, stubType: IStubElementType<*, *>) : super(stub, stubType)

    // Backed by the stub so that the name can be queried without loading the AST.
    override fun getName(): String? = macroName

    override fun getNameIdentifier(): PsiElement? = identifier

    override fun setName(name: String): PsiElement {
        identifier?.replace(createIdentifier(project, name))
        return this
    }

    override fun getTextOffset(): Int = identifier?.textOffset ?: super.getTextOffset()
}
