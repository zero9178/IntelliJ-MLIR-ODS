package com.github.zero9178.mlirods.language.psi.impl

import com.github.zero9178.mlirods.language.generated.psi.TableGenLetItem
import com.github.zero9178.mlirods.language.stubs.impl.TableGenLetItemStub
import com.intellij.extapi.psi.StubBasedPsiElementBase
import com.intellij.lang.ASTNode
import com.intellij.psi.stubs.IStubElementType

abstract class TableGenLetItemMixin : StubBasedPsiElementBase<TableGenLetItemStub>, TableGenLetItem {

    constructor(node: ASTNode) : super(node)

    constructor(stub: TableGenLetItemStub, stubType: IStubElementType<*, *>) : super(stub, stubType)

    override val fieldName: String?
        get() {
            greenStub?.let { return it.name }

            return fieldIdentifier?.text
        }

    override val letMode: LetMode?
        get() {
            greenStub?.let { return it.letMode }

            return super.letMode
        }
}
