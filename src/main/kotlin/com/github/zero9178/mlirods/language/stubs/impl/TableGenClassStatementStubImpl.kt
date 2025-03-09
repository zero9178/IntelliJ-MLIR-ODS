package com.github.zero9178.mlirods.language.stubs.impl

import com.github.zero9178.mlirods.language.generated.psi.TableGenClassStatement
import com.github.zero9178.mlirods.language.psi.TableGenDefNameIdentifierOwner
import com.github.zero9178.mlirods.language.stubs.TableGenStubElementTypes
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.StubBase
import com.intellij.psi.stubs.StubElement

internal class TableGenClassStatementStubImpl(
    override val name: String,
    parent: StubElement<out PsiElement>?
) : StubBase<TableGenClassStatement>(
    parent, TableGenStubElementTypes.CLASS_STATEMENT
), TableGenClassStatementStub
