package com.github.zero9178.mlirods.index

import com.github.zero9178.mlirods.language.psi.TableGenRecord
import com.intellij.psi.stubs.StringStubIndexExtension
import com.intellij.psi.stubs.StubIndexKey

/**
 * Index mapping the name of a class to classes with the same class name in their derived list.
 * Requires further class reference resolution to determine whether the class list occurrence constitutes a derived
 * relation.
 */
val MAY_DERIVE_CLASS_INDEX = StubIndexKey.createIndexKey<String, TableGenRecord>("MAY_DERIVE_CLASS_INDEX")

private class TableGenMayDeriveClassIndex : StringStubIndexExtension<TableGenRecord>() {

    override fun getKey(): StubIndexKey<String, TableGenRecord> {
        return MAY_DERIVE_CLASS_INDEX
    }
}
