package com.github.zero9178.mlirods.language.psi.impl

import com.github.zero9178.mlirods.getCachedValue
import com.github.zero9178.mlirods.language.generated.psi.*
import com.github.zero9178.mlirods.language.psi.TableGenFieldScopeNode
import com.github.zero9178.mlirods.language.stubs.impl.TableGenIntegerValueNodeStub
import com.github.zero9178.mlirods.language.stubs.impl.TableGenStringValueNodeStub
import com.github.zero9178.mlirods.language.types.TableGenType
import com.github.zero9178.mlirods.language.types.TableGenUnknownType
import com.github.zero9178.mlirods.language.values.TableGenIntegerValue
import com.github.zero9178.mlirods.language.values.TableGenStringValue
import com.github.zero9178.mlirods.language.values.TableGenUnknownValue
import com.github.zero9178.mlirods.language.values.TableGenValue
import com.github.zero9178.mlirods.merge
import com.intellij.psi.PsiElement
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValue

/**
 * Class passed around as context during a constant evaluation.
 * This is used to keep track of things such as template parameter to argument mapping or field mappings.
 */
class TableGenEvaluationContext(
    calcTemplateArgs: () -> Map<TableGenTemplateArgDecl, TableGenValueNode> = { emptyMap() },
    private val calcFieldArg: (String, TableGenEvaluationContext) -> TableGenValue? = { _, _ -> null },
) {

    companion object {

        private fun matchArgumentsToTemplateArgs(classRef: TableGenAbstractClassRef, klass: TableGenClassStatement) =
            getCachedValue(classRef) {
                val positional = classRef.argValueItemList.takeWhile {
                    it.identifier == null
                }
                val named = classRef.argValueItemList.fold(mutableMapOf<String, TableGenValueNode>()) { acc, iter ->
                    iter.identifier?.text?.let {
                        acc[it] = iter.valueNode
                    }
                    acc
                }

                val result = mutableMapOf<TableGenTemplateArgDecl, TableGenValueNode>()
                for ((i, iter) in klass.templateArgDeclList.withIndex()) {
                    positional[i]?.let {
                        it.valueNode?.let { value ->
                            result[iter] = value
                        }
                        continue
                    }

                    iter.name?.let { name ->
                        named[name]?.let {
                            result[iter] = it
                        }
                    }
                }
                CachedValueProvider.Result.create(result.toMap(), classRef, klass)
            }

        private fun TableGenFieldScopeNode.getDirectBaseclassArgumentMap(): CachedValue<Map<TableGenTemplateArgDecl, TableGenValueNode>> =
            baseClassRefs.flatMap { classRef ->
                val ref = classRef.reference?.resolve() as? TableGenClassStatement ?: return@flatMap emptyList()
                listOf(matchArgumentsToTemplateArgs(classRef, ref), ref.getDirectBaseclassArgumentMap())
            }.toList().merge(this) {
                val result = mutableMapOf<TableGenTemplateArgDecl, TableGenValueNode>()
                it.forEach(result::putAll)
                result
            }

    }

    constructor(defStatement: TableGenDefStatement) : this({
        defStatement.getDirectBaseclassArgumentMap().value
    }, { name, context ->
        when (val assignment = defStatement.allFieldAssignments[name]?.lastOrNull()) {
            is TableGenFieldBodyItem -> assignment.valueNode?.evaluate(context)
            else -> null
        }
    })

    val templateArgDeclValues by lazy(calcTemplateArgs)

    fun evaluateFieldInContext(name: String): TableGenValue? = calcFieldArg.invoke(name, this)
}

interface TableGenValueNodeEx : PsiElement {
    /**
     * Returns the type of this TableGen expression.
     */
    val type: TableGenType
        get() = TableGenUnknownType

    /**
     * Performs constant evaluation of this value within the given context.
     */
    fun evaluate(context: TableGenEvaluationContext): TableGenValue = TableGenUnknownValue
}

/**
 * Specialization interface for [TableGenValueNodeEx.evaluate] implementations which are atomic.
 * The result of these are never dependent on a context, but rather purely on their Psi subtree.
 */
interface TableGenAtomicValue : TableGenValueNodeEx {
    fun evaluateAtomic(): TableGenValue?
}

interface TableGenIntegerValueNodeEx : TableGenAtomicValue {
    override fun evaluateAtomic(): TableGenIntegerValue?

    val stub: TableGenIntegerValueNodeStub?
}

interface TableGenIdentifierValueNodeEx : TableGenValueNodeEx {
    val identifierText: String
}

interface TableGenStringValueNodeEx : TableGenAtomicValue {
    override fun evaluateAtomic(): TableGenStringValue?

    val stub: TableGenStringValueNodeStub?
}