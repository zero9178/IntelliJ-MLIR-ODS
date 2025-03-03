package com.github.zero9178.mlirods.language

import com.github.zero9178.mlirods.MyIcons
import com.github.zero9178.mlirods.language.generated.psi.TableGenIncludeDirective
import com.github.zero9178.mlirods.language.psi.TableGenIdentifierScopeNode
import com.github.zero9178.mlirods.language.stubs.TableGenStubElementTypes
import com.github.zero9178.mlirods.model.TableGenContext
import com.intellij.extapi.psi.PsiFileBase
import com.intellij.lang.Language
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.fileTypes.LanguageFileType
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.IStubElementType
import javax.swing.Icon

class TableGenLanguage private constructor() : Language("TableGen") {
    companion object {
        val INSTANCE = TableGenLanguage()
    }
}

class TableGenFile(viewProvider: FileViewProvider, var context: TableGenContext) :
    PsiFileBase(viewProvider, TableGenLanguage.INSTANCE), TableGenIdentifierScopeNode {
    override fun getFileType(): FileType = TableGenFileType.INSTANCE

    /**
     * Returns a sequence of all stub elements that is one of the given [elementTypes].
     */
    fun stubStream(vararg elementTypes: IStubElementType<*, *>): Sequence<PsiElement> {
        val spine = stubbedSpine
        return (0 until spine.stubCount).asSequence().filter {
            elementTypes.contains(spine.getStubType(it))
        }.mapNotNull {
            spine.getStubPsi(it)
        }
    }

    /**
     * Returns a sequence of all include directives in the file.
     */
    val includeDirectives: Sequence<TableGenIncludeDirective>
        get() = stubStream(TableGenStubElementTypes.INCLUDE_DIRECTIVE).filterIsInstance<TableGenIncludeDirective>()
}

class TableGenFileType : LanguageFileType(TableGenLanguage.INSTANCE) {

    @Suppress("CompanionObjectInExtension")
    companion object {
        @JvmField
        val INSTANCE = TableGenFileType()
    }

    override fun getName(): String {
        return "TableGen"
    }

    override fun getDescription(): String {
        return "LLVM TableGen langauge"
    }

    override fun getDefaultExtension(): String {
        return "td";
    }

    override fun getIcon(): Icon {
        return MyIcons.TableGenIcon
    }
}
