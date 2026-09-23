package com.github.zero9178.mlirods.language

import com.github.zero9178.mlirods.language.generated.TableGenTypes
import com.github.zero9178.mlirods.language.generated.psi.*
import com.intellij.lang.parameterInfo.CreateParameterInfoContext
import com.intellij.lang.parameterInfo.ParameterInfoContext
import com.intellij.lang.parameterInfo.ParameterInfoHandlerWithTabActionSupport
import com.intellij.lang.parameterInfo.ParameterInfoUIContext
import com.intellij.lang.parameterInfo.ParameterInfoUtils
import com.intellij.lang.parameterInfo.UpdateParameterInfoContext
import com.intellij.psi.tree.IElementType
import com.intellij.psi.util.parentOfType
import com.intellij.psi.util.startOffset

internal class TableGenParameterInfoHandler : ParameterInfoHandlerWithTabActionSupport<
        TableGenAbstractRef, TableGenAbstractClassStatement, TableGenArgValueItem> {

    /**
     * Finds a class or multiclass reference if the current offset is within its argument list.
     */
    private fun findElementForParameterInfoImpl(context: ParameterInfoContext): TableGenAbstractRef? {
        val file = context.file
        val ref = file.findElementAt(context.offset)?.parentOfType<TableGenAbstractRef>() ?: return null
        val parametersStart = ref.lAngle ?: return null
        if (context.offset <= parametersStart.startOffset) return null

        return ref
    }

    override fun findElementForParameterInfo(context: CreateParameterInfoContext): TableGenAbstractRef? {
        val ref = findElementForParameterInfoImpl(context) ?: return null

        // A multiclass reference within a 'defm' may refer to a class instead.
        context.itemsToShow = ref.references.mapNotNull {
            it.resolve() as? TableGenAbstractClassStatement
        }.toTypedArray()
        return ref
    }

    override fun showParameterInfo(
        element: TableGenAbstractRef, context: CreateParameterInfoContext
    ) {
        context.showHint(element, context.offset, this)
    }

    override fun findElementForUpdatingParameterInfo(context: UpdateParameterInfoContext): TableGenAbstractRef? {
        val ref = findElementForParameterInfoImpl(context) ?: return null
        if (ref != context.parameterOwner) return null

        val index = ParameterInfoUtils.getCurrentParameterIndex(
            ref.node, context.offset, actualParameterDelimiterType
        )
        context.setCurrentParameter(index)
        return ref
    }

    override fun updateParameterInfo(
        parameterOwner: TableGenAbstractRef, context: UpdateParameterInfoContext
    ) {
        if (context.parameterOwner != parameterOwner) context.removeHint()

        val index = ParameterInfoUtils.getCurrentParameterIndex(
            parameterOwner.node, context.offset, actualParameterDelimiterType
        )
        context.setCurrentParameter(index)
    }

    override fun updateUI(
        p: TableGenAbstractClassStatement?, context: ParameterInfoUIContext
    ) {
        if (p == null) return

        context.isUIComponentEnabled = true

        var highlight = 0 to 0
        val reprs = p.templateArgDeclList.map {
            it.typeNode.text + " " + it.name
        }
        if (context.currentParameterIndex == 0)
            highlight = 0 to (reprs.firstOrNull()?.length ?: 0)

        val s = reprs.reduceIndexed { i, acc, iter ->
            val tmp = "$acc, "
            if (i == context.currentParameterIndex) highlight = tmp.length to tmp.length + iter.length
            tmp + iter
        }


        context.setupUIComponentPresentation(
            s, highlight.first, highlight.second, false, false, false, context.defaultParameterColor
        )
    }

    override fun getActualParameters(o: TableGenAbstractRef): Array<out TableGenArgValueItem> {
        return o.argValueItemList.toTypedArray()
    }

    override fun getActualParameterDelimiterType(): IElementType {
        return TableGenTypes.COMMA
    }

    override fun getActualParametersRBraceType(): IElementType {
        return TableGenTypes.RANGLE
    }

    override fun getArgumentListAllowedParentClasses(): Set<Class<*>?> {
        // A class ref might occur in a record body (class or def statement) or anywhere a value may occur, a multiclass
        // ref in a 'defm' or 'multiclass' statement. Those are basically all the scope items and other values.
        return setOf(TableGenScopeItem::class.java, TableGenValueNode::class.java)
    }

    override fun getArgListStopSearchClasses(): Set<Class<*>?> {
        return setOf(TableGenScopeItem::class.java)
    }

    override fun getArgumentListClass(): Class<TableGenAbstractRef> {
        return TableGenAbstractRef::class.java
    }
}