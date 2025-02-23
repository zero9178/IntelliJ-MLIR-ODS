package com.github.zero9178.mlirods.language.stubs

import com.github.zero9178.mlirods.language.TableGenFile
import com.github.zero9178.mlirods.language.TableGenLanguage
import com.intellij.psi.stubs.PsiFileStubImpl
import com.intellij.psi.tree.IStubFileElementType
import org.jetbrains.annotations.NonNls

class TableGenFileStub(tableGenFile: TableGenFile) : PsiFileStubImpl<TableGenFile>(tableGenFile) {
    override fun getType(): IStubFileElementType<TableGenFileStub> {
        return TableGenStubFileElementType.INSTANCE
    }
}

class TableGenStubFileElementType :
    IStubFileElementType<TableGenFileStub>("FILE", TableGenLanguage.Companion.INSTANCE) {
    override fun getExternalId(): @NonNls String {
        return "tablegen." + toString()
    }

    override fun getStubVersion(): Int {
        return 1
    }

    companion object {
        val INSTANCE = TableGenStubFileElementType()
    }
}