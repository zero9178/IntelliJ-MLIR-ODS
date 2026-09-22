package com.github.zero9178.mlirods.language.psi.impl

import com.github.zero9178.mlirods.language.generated.psi.TableGenDefStatement
import com.github.zero9178.mlirods.language.generated.psi.TableGenDefvarStatement
import com.github.zero9178.mlirods.language.generated.psi.TableGenFieldBodyItem
import com.github.zero9178.mlirods.language.generated.psi.TableGenIdentifierValueNode
import com.github.zero9178.mlirods.language.generated.psi.TableGenTemplateArgDecl
import com.github.zero9178.mlirods.language.stubs.impl.TableGenIdentifierValueNodeStub
import com.github.zero9178.mlirods.language.stubs.impl.TableGenValueNodeStub
import com.github.zero9178.mlirods.language.values.TableGenRecordValue
import com.github.zero9178.mlirods.language.values.TableGenUnknownValue
import com.github.zero9178.mlirods.language.values.TableGenValue
import com.intellij.extapi.psi.StubBasedPsiElementBase
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.IStubElementType
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

abstract class TableGenIdentifierValueNodeMixin : StubBasedPsiElementBase<TableGenIdentifierValueNodeStub>,
    TableGenIdentifierValueNode, PsiElement {

    constructor(node: ASTNode) : super(node)

    constructor(
        stub: TableGenValueNodeStub,
        stubType: IStubElementType<*, *>
    ) : super(stub as TableGenIdentifierValueNodeStub, stubType)

    override val identifierText: String
        get() = stub?.identifier ?: identifier.text

    override suspend fun evaluateInner(context: TableGenEvaluationContext): TableGenValue {
        val ref = reference?.resolve() ?: return TableGenUnknownValue
        if (ref is TableGenDefStatement) return TableGenRecordValue(ref)

        // The value referred to may refer to another, and so on, to any length no matter how deep the AST is. Launched,
        // it is computed from the bottom of the stack of some thread instead of on top of ours.
        return coroutineScope {
            async {
                when (ref) {
                    is TableGenDefvarStatement -> ref.valueNode?.evaluate(context)
                    is TableGenTemplateArgDecl -> context.evaluateTemplateArgDeclInContext(context, ref)
                    is TableGenFieldBodyItem -> ref.fieldName?.let { context.evaluateFieldInContext(context, it) }
                    else -> null
                }
            }.await() ?: TableGenUnknownValue
        }
    }
}

