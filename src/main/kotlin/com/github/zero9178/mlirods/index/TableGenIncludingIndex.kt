package com.github.zero9178.mlirods.index

import com.github.zero9178.mlirods.language.generated.psi.TableGenIncludeDirective
import com.intellij.psi.stubs.StringStubIndexExtension
import com.intellij.psi.stubs.StubIndexKey

val INCLUDED_INDEX = StubIndexKey.createIndexKey<String, TableGenIncludeDirective>("INCLUDED_INDEX")

/**
 * Maps a filename (not path!) to every include directive that ends with that file name.
 */
private class TableGenIncludingIndex : StringStubIndexExtension<TableGenIncludeDirective>() {

    override fun getKey(): StubIndexKey<String, TableGenIncludeDirective> {
        return INCLUDED_INDEX
    }
}