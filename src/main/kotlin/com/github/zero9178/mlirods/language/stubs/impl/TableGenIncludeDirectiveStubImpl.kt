package com.github.zero9178.mlirods.language.stubs.impl

import com.github.zero9178.mlirods.language.generated.psi.TableGenIncludeDirective
import com.github.zero9178.mlirods.language.stubs.TableGenStubElementTypes
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.StubBase
import com.intellij.psi.stubs.StubElement

class TableGenIncludeDirectiveStubImpl(override val includeSuffix: String, parent: StubElement<out PsiElement>?) :
    StubBase<TableGenIncludeDirective>(
    parent, TableGenStubElementTypes.INCLUDE_DIRECTIVE
    ), TableGenIncludeDirectiveStub
