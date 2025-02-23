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
    }
}