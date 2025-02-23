package com.github.zero9178.mlirods.index

import com.github.zero9178.mlirods.language.psi.TableGenDefNameIdentifierOwner
import com.intellij.psi.stubs.StringStubIndexExtension
import com.intellij.psi.stubs.StubIndexKey

/**
 * Index mapping global identifiers, that refer to any kind of 'defs', to the corresponding psi element.
 */
val DEF_INDEX = StubIndexKey.createIndexKey<String, TableGenDefNameIdentifierOwner>("DEF_INDEX")

private class TableGenDefIndex : StringStubIndexExtension<TableGenDefNameIdentifierOwner>() {

    override fun getKey(): StubIndexKey<String, TableGenDefNameIdentifierOwner> {
        return DEF_INDEX
    }
}
