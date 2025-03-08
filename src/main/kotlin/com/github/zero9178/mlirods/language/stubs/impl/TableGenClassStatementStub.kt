package com.github.zero9178.mlirods.language.stubs.impl

import com.github.zero9178.mlirods.language.generated.psi.TableGenClassStatement
import com.github.zero9178.mlirods.language.psi.TableGenDefNameIdentifierOwner
import com.intellij.psi.stubs.StubElement

/**
 * Stub interface for [TableGenClassStatement].
 */
interface TableGenClassStatementStub : StubElement<TableGenClassStatement> {
    val name: String
}
