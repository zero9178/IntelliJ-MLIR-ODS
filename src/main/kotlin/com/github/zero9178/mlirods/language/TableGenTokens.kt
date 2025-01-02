package com.github.zero9178.mlirods.language

import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet
import org.jetbrains.annotations.NonNls
import com.github.zero9178.mlirods.language.generated.TableGenTypes

class TableGenTokenType(@NonNls debugName: String) : IElementType(debugName, TableGenLanguage.INSTANCE)

class TableGenElementType(@NonNls debugName: String) : IElementType(debugName, TableGenLanguage.INSTANCE)
