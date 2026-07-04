package com.github.zero9178.mlirods.language.psi.impl

import com.github.zero9178.mlirods.language.generated.TableGenTypes
import com.github.zero9178.mlirods.language.stubs.impl.TableGenMacroDirectiveStub
import com.intellij.extapi.psi.StubBasedPsiElementBase
import com.intellij.lang.ASTNode
import com.intellij.psi.stubs.IStubElementType

/**
 * Base mixin shared by the preprocessor directives carrying a macro name ('#define', '#ifdef' and '#ifndef').
 * The macro name is stored on the stub so that it can be queried without reparsing the file.
 */
abstract class TableGenMacroDirectiveMixin : StubBasedPsiElementBase<TableGenMacroDirectiveStub>,
    TableGenMacroDirectiveEx {

    override val macroName: String?
        get() = greenStub?.macroName ?: node.findChildByType(TableGenTypes.IDENTIFIER)?.text

    constructor(node: ASTNode) : super(node)

    constructor(stub: TableGenMacroDirectiveStub, stubType: IStubElementType<*, *>) : super(stub, stubType)
}
