package com.github.zero9178.mlirods.index

import com.github.zero9178.mlirods.language.generated.psi.TableGenMulticlassStatement
import com.intellij.psi.stubs.StringStubIndexExtension
import com.intellij.psi.stubs.StubIndexKey

/**
 * Index mapping the name of a multiclass to the multiclass statements defining it.
 */
val MULTICLASS_INDEX = StubIndexKey.createIndexKey<String, TableGenMulticlassStatement>("MULTICLASS_INDEX")

internal class TableGenMulticlassIndex : StringStubIndexExtension<TableGenMulticlassStatement>() {

    override fun getKey(): StubIndexKey<String, TableGenMulticlassStatement> {
        return MULTICLASS_INDEX
    }
}
