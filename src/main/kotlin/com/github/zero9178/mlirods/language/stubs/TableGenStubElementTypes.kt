package com.github.zero9178.mlirods.language.stubs

import com.github.zero9178.mlirods.language.generated.TableGenTypes

/**
 * Interface used to collect stub element types in the companion object.
 */
interface TableGenStubElementTypes {
    companion object {
        @JvmField
        val INCLUDE_DIRECTIVE = TableGenTypes.INCLUDE_DIRECTIVE!!

        @JvmField
        val DEFVAR_STATEMENT = TableGenTypes.DEFVAR_STATEMENT!!

        @JvmField
        val DEF_STATEMENT = TableGenTypes.DEF_STATEMENT!!

        @JvmField
        val TEMPLATE_ARG_DECL = TableGenTypes.TEMPLATE_ARG_DECL!!

        @JvmField
        val CLASS_STATEMENT = TableGenTypes.CLASS_STATEMENT!!

        @JvmField
        val FOREACH_STATEMENT = TableGenTypes.FOREACH_STATEMENT!!

        @JvmField
        val FOREACH_ITERATOR = TableGenTypes.FOREACH_ITERATOR!!

        @JvmField
        val LET_STATEMENT = TableGenTypes.LET_STATEMENT!!

        @JvmField
        val IF_STATEMENT = TableGenTypes.IF_STATEMENT!!

        @JvmField
        val IF_BODY = TableGenTypes.IF_BODY!!

        @JvmField
        val FIELD_BODY_ITEM = TableGenTypes.FIELD_BODY_ITEM!!

        @JvmField
        val LET_BODY_ITEM = TableGenTypes.LET_BODY_ITEM!!

        @JvmField
        val CLASS_REF = TableGenTypes.CLASS_REF!!

        @JvmField
        val BIT_TYPE_NODE = TableGenTypes.BIT_TYPE_NODE!!

        @JvmField
        val INT_TYPE_NODE = TableGenTypes.INT_TYPE_NODE!!

        @JvmField
        val STRING_TYPE_NODE = TableGenTypes.STRING_TYPE_NODE!!

        @JvmField
        val DAG_TYPE_NODE = TableGenTypes.DAG_TYPE_NODE!!

        @JvmField
        val CODE_TYPE_NODE = TableGenTypes.CODE_TYPE_NODE!!

        @JvmField
        val BITS_TYPE_NODE = TableGenTypes.BITS_TYPE_NODE!!

        @JvmField
        val LIST_TYPE_NODE = TableGenTypes.LIST_TYPE_NODE!!

        @JvmField
        val CLASS_TYPE_NODE = TableGenTypes.CLASS_TYPE_NODE!!

        @JvmField
        val CONCAT_VALUE_NODE = TableGenTypes.CONCAT_VALUE_NODE!!

        @JvmField
        val FIELD_ACCESS_VALUE_NODE = TableGenTypes.FIELD_ACCESS_VALUE_NODE!!

        @JvmField
        val BIT_ACCESS_VALUE_NODE = TableGenTypes.BIT_ACCESS_VALUE_NODE!!

        @JvmField
        val SLICE_ACCESS_VALUE_NODE = TableGenTypes.SLICE_ACCESS_VALUE_NODE!!

        @JvmField
        val INTEGER_VALUE_NODE = TableGenTypes.INTEGER_VALUE_NODE!!

        @JvmField
        val STRING_VALUE_NODE = TableGenTypes.STRING_VALUE_NODE!!

        @JvmField
        val BOOL_VALUE_NODE = TableGenTypes.BOOL_VALUE_NODE!!

        @JvmField
        val UNDEF_VALUE_NODE = TableGenTypes.UNDEF_VALUE_NODE!!

        @JvmField
        val BITS_INIT_VALUE_NODE = TableGenTypes.BITS_INIT_VALUE_NODE!!

        @JvmField
        val LIST_INIT_VALUE_NODE = TableGenTypes.LIST_INIT_VALUE_NODE!!

        @JvmField
        val DAG_INIT_VALUE_NODE = TableGenTypes.DAG_INIT_VALUE_NODE!!

        @JvmField
        val IDENTIFIER_VALUE_NODE = TableGenTypes.IDENTIFIER_VALUE_NODE!!

        @JvmField
        val CLASS_INSTANTIATION_VALUE_NODE = TableGenTypes.CLASS_INSTANTIATION_VALUE_NODE!!

        @JvmField
        val FOREACH_OPERATOR_VALUE_NODE = TableGenTypes.FOREACH_OPERATOR_VALUE_NODE!!

        @JvmField
        val FOLDL_OPERATOR_VALUE_NODE = TableGenTypes.FOLDL_OPERATOR_VALUE_NODE!!

        @JvmField
        val BANG_OPERATOR_VALUE_NODE = TableGenTypes.BANG_OPERATOR_VALUE_NODE!!

        @JvmField
        val COND_OPERATOR_VALUE_NODE = TableGenTypes.COND_OPERATOR_VALUE_NODE!!

        @JvmField
        val BLOCK_STRING_VALUE = TableGenTypes.BLOCK_STRING_VALUE!!

        @JvmField
        val CAST_OPERATOR_VALUE_NODE = TableGenTypes.CAST_OPERATOR_VALUE_NODE!!

        @JvmField
        val SORT_OPERATOR_VALUE_NODE = TableGenTypes.SORT_OPERATOR_VALUE_NODE!!

        @JvmField
        val SWITCH_OPERATOR_VALUE_NODE = TableGenTypes.SWITCH_OPERATOR_VALUE_NODE!!

        @JvmField
        val ARG_VALUE_ITEM = TableGenTypes.ARG_VALUE_ITEM!!
    }
}