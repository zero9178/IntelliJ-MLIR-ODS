package com.github.zero9178.mlirods.index

import com.github.zero9178.mlirods.language.psi.TableGenIdentifierElement
import com.intellij.psi.stubs.IntStubIndexExtension
import com.intellij.psi.stubs.StringStubIndexExtension
import com.intellij.psi.stubs.StubIndexKey

/**
 * Index mapping global identifiers to the corresponding psi element.
 */
val IDENTIFIER_INDEX = StubIndexKey.createIndexKey<String, TableGenIdentifierElement>("IDENTIFIER_INDEX")

/**
 * Index listing every global identifier element under the key 0.
 */
val ALL_IDENTIFIERS_INDEX = StubIndexKey.createIndexKey<Int, TableGenIdentifierElement>("ALL_IDENTIFIERS_INDEX")

private class TableGenIdentifierIndex : StringStubIndexExtension<TableGenIdentifierElement>() {

    override fun getKey(): StubIndexKey<String, TableGenIdentifierElement> {
        return IDENTIFIER_INDEX
    }
}

private class TableGenAllIdentifiersIndex : IntStubIndexExtension<TableGenIdentifierElement>() {

    override fun getKey(): StubIndexKey<Int, TableGenIdentifierElement> {
        return ALL_IDENTIFIERS_INDEX
    }
}
