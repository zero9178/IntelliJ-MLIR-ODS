package com.github.zero9178.mlirods.index

import com.github.zero9178.mlirods.language.generated.psi.TableGenDefineDirective
import com.intellij.psi.stubs.StringStubIndexExtension
import com.intellij.psi.stubs.StubIndexKey

/**
 * Index mapping the name of a macro to the '#define' directives defining it.
 */
val DEFINE_INDEX = StubIndexKey.createIndexKey<String, TableGenDefineDirective>("DEFINE_INDEX")

internal class TableGenDefineIndex : StringStubIndexExtension<TableGenDefineDirective>() {

    override fun getKey(): StubIndexKey<String, TableGenDefineDirective> {
        return DEFINE_INDEX
    }
}
