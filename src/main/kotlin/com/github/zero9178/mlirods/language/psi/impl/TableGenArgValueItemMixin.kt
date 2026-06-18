package com.github.zero9178.mlirods.language.psi.impl

import com.github.zero9178.mlirods.language.generated.psi.TableGenArgValueItem
import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode

abstract class TableGenArgValueItemMixin : ASTWrapperPsiElement, TableGenArgValueItem {

    constructor(node: ASTNode) : super(node)

    override val isNamedArgument: Boolean
        get() = equalsSign != null
}
