package com.github.zero9178.mlirods.language.stubs.impl

import com.github.zero9178.mlirods.language.psi.TableGenDefNameIdentifierOwner
import com.intellij.psi.stubs.StubElement

/**
 * Stub interface for [TableGenDefNameIdentifierOwner] elements.
 */
interface TableGenDefNameIdentifierStub : StubElement<TableGenDefNameIdentifierOwner> {
    val name: String
}
