package com.github.zero9178.mlirods.language.psi.impl

import com.github.zero9178.mlirods.language.stubs.impl.TableGenClassTypeNodeStub
import com.github.zero9178.mlirods.language.stubs.impl.TableGenTypeNodeStub
import com.intellij.psi.StubBasedPsiElement

interface TableGenClassTypeNodeEx : StubBasedPsiElement<TableGenTypeNodeStub> {
    override fun getStub(): TableGenClassTypeNodeStub?
}