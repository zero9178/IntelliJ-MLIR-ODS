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
        val LET_STATEMENT = TableGenTypes.LET_STATEMENT!!

        @JvmField
        val IF_STATEMENT = TableGenTypes.IF_STATEMENT!!

        @JvmField
        val IF_BODY = TableGenTypes.IF_BODY!!

        @JvmField
        val FIELD_BODY_ITEM = TableGenTypes.FIELD_BODY_ITEM!!

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
    }
}