package com.github.zero9178.mlirods.language.psi.impl

import com.github.zero9178.mlirods.language.generated.psi.TableGenIfdefIfndefDirective
import com.github.zero9178.mlirods.language.psi.TableGenMacroReference
import com.github.zero9178.mlirods.language.stubs.impl.TableGenMacroDirectiveStub
import com.intellij.lang.ASTNode
import com.intellij.psi.stubs.IStubElementType

/**
 * Mixin for the '#ifdef'/'#ifndef' directives, whose macro name references the corresponding '#define' directive.
 */
abstract class TableGenIfdefIfndefDirectiveMixin : TableGenMacroDirectiveMixin, TableGenIfdefIfndefDirective {

    constructor(node: ASTNode) : super(node)

    constructor(stub: TableGenMacroDirectiveStub, stubType: IStubElementType<*, *>) : super(stub, stubType)

    override fun getReference() = TableGenMacroReference(this)
}
