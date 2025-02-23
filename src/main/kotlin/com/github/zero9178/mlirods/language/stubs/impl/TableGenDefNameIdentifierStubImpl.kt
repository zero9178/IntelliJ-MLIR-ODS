package com.github.zero9178.mlirods.language.stubs.impl

import com.github.zero9178.mlirods.language.psi.TableGenDefNameIdentifierOwner
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.StubBase
import com.intellij.psi.stubs.StubElement

internal class TableGenDefNameIdentifierStubImpl(
    override val name: String,
    parent: StubElement<out PsiElement>?,
    elementType: TableGenDefNameIdentifierStubElementType,
) : StubBase<TableGenDefNameIdentifierOwner>(
    parent, elementType
), TableGenDefNameIdentifierStub
