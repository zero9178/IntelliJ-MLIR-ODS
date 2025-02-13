package com.github.zero9178.mlirods.language.stubs.impl

import com.github.zero9178.mlirods.language.generated.psi.TableGenIncludeDirective
import com.intellij.psi.stubs.StubElement

/**
 * Interface used for include related methods implemented by both the Psi element and the backing stub class.
 */
interface TableGenIncludeDirectiveStubInterface {
    val includeSuffix: String
}

/**
 * Interface for include stubs.
 */
interface TableGenIncludeDirectiveStub : TableGenIncludeDirectiveStubInterface, StubElement<TableGenIncludeDirective>
