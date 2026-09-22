package com.github.zero9178.mlirods.language.psi.impl

import com.github.zero9178.mlirods.language.generated.psi.TableGenBlockStringValue
import com.github.zero9178.mlirods.language.generated.psi.impl.TableGenStringValueNodeImpl
import com.github.zero9178.mlirods.language.stubs.impl.TableGenValueNodeStub
import com.intellij.lang.ASTNode
import com.intellij.psi.LiteralTextEscaper
import com.intellij.psi.PsiLanguageInjectionHost
import com.intellij.psi.impl.source.tree.LeafElement
import com.intellij.psi.stubs.IStubElementType

abstract class TableGenBlockStringValueMixin : TableGenStringValueNodeImpl, TableGenBlockStringValue {

    constructor(node: ASTNode) : super(node)

    constructor(stub: TableGenValueNodeStub, stubType: IStubElementType<*, *>) : super(stub, stubType)

    override fun isValidHost() = true

    /**
     * Replaces the text of the whole element, delimiters included, as the platform passes the complete host text when
     * writing changes made to the injected file back into the host.
     */
    override fun updateText(text: String): PsiLanguageInjectionHost {
        (node.firstChildNode as LeafElement).replaceWithText(text)
        return this
    }

    /**
     * Block strings have no escape sequences and the indentation shared by all lines is left out by the injection places
     * rather than by decoding, so the text of the injected file is always a verbatim copy of the host text.
     */
    override fun createLiteralTextEscaper(): LiteralTextEscaper<TableGenBlockStringValue> =
        LiteralTextEscaper.createSimple(this, false)
}
