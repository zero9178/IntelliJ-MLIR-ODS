package com.github.zero9178.mlirods.language.psi

import com.github.zero9178.mlirods.language.TableGenFileType
import com.github.zero9178.mlirods.language.TableGenLanguage
import com.github.zero9178.mlirods.language.generated.psi.TableGenClassStatement
import com.github.zero9178.mlirods.language.generated.psi.TableGenIncludeDirective
import com.github.zero9178.mlirods.language.stubs.TableGenStubElementTypes
import com.github.zero9178.mlirods.model.TableGenContext
import com.intellij.extapi.psi.PsiFileBase
import com.intellij.openapi.fileTypes.FileType
import com.intellij.psi.FileViewProvider
import com.intellij.psi.stubs.IStubElementType
import com.intellij.util.resettableLazy

class TableGenFile(viewProvider: FileViewProvider, val context: TableGenContext) :
    PsiFileBase(viewProvider, TableGenLanguage.INSTANCE), TableGenIdentifierScopeNode {

    override fun getFileType(): FileType = TableGenFileType.INSTANCE

    /**
     * Returns a sequence of all stub elements that is one of the given [elementTypes].
     */
    inline fun <reified T> stubStream(vararg elementTypes: IStubElementType<*, T>): Sequence<T> {
        val spine = stubbedSpine
        return (0 until spine.stubCount).asSequence().filter {
            elementTypes.contains(spine.getStubType(it))
        }.mapNotNull {
            spine.getStubPsi(it)
        }.filterIsInstance<T>()
    }

    /**
     * Returns a sequence of all include directives in the file.
     */
    val includeDirectives: Sequence<TableGenIncludeDirective>
        get() = stubStream(TableGenStubElementTypes.INCLUDE_DIRECTIVE)

    private val myClassMap = resettableLazy {
        val result = mutableMapOf<String, MutableList<TableGenClassStatement>>()
        stubStream(TableGenStubElementTypes.CLASS_STATEMENT).forEach {
            it.name?.let { name ->
                result.getOrPut(name) {
                    mutableListOf()
                }.add(it)
            }
        }
        result
    }

    /**
     * Returns a map associating every class name to the corresponding class statements.
     * Only class statements that are directly defined within this file are included.
     * The list of class statements are ordered by lexical appearance.
     */
    val classMap: Map<String, List<TableGenClassStatement>> by myClassMap


    override fun subtreeChanged() {
        super.subtreeChanged()
        myClassMap.reset()
    }
}