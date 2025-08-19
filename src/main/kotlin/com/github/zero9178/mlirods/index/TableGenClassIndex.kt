package com.github.zero9178.mlirods.index

import com.github.zero9178.mlirods.language.generated.psi.TableGenClassStatement
import com.intellij.psi.stubs.IntStubIndexExtension
import com.intellij.psi.stubs.StringStubIndexExtension
import com.intellij.psi.stubs.StubIndexKey

/**
 * Index mapping global identifiers, that refer to any kind of 'class', to the corresponding psi element.
 */
val CLASS_INDEX = StubIndexKey.createIndexKey<String, TableGenClassStatement>("CLASS_INDEX")

/**
 * Index listing every class statement under the key 0.
 */
val ALL_CLASSES_INDEX = StubIndexKey.createIndexKey<Int, TableGenClassStatement>("ALL_CLASSES_INDEX")

private class TableGenClassIndex : StringStubIndexExtension<TableGenClassStatement>() {

    override fun getKey(): StubIndexKey<String, TableGenClassStatement> {
        return CLASS_INDEX
    }
}

private class TableGenAllClassesIndex : IntStubIndexExtension<TableGenClassStatement>() {

    override fun getKey(): StubIndexKey<Int, TableGenClassStatement> {
        return ALL_CLASSES_INDEX
    }
}
