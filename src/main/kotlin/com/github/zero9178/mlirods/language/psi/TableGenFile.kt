package com.github.zero9178.mlirods.language.psi

import com.github.zero9178.mlirods.language.TableGenFileType
import com.github.zero9178.mlirods.language.TableGenLanguage
import com.github.zero9178.mlirods.language.generated.TableGenTypes
import com.github.zero9178.mlirods.language.generated.psi.TableGenClassStatement
import com.github.zero9178.mlirods.language.generated.psi.TableGenIncludeDirective
import com.github.zero9178.mlirods.language.stubs.TableGenStubElementTypes
import com.github.zero9178.mlirods.model.TableGenContext
import com.github.zero9178.mlirods.model.TableGenContextService
import com.intellij.extapi.psi.PsiFileBase
import com.intellij.openapi.components.serviceOrNull
import com.intellij.openapi.fileTypes.FileType
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.IStubElementType
import com.intellij.psi.tree.TokenSet
import com.intellij.util.ArrayFactory
import com.intellij.util.resettableLazy

class TableGenFile(viewProvider: FileViewProvider) : PsiFileBase(viewProvider, TableGenLanguage.INSTANCE),
    TableGenIdentifierScopeNode {

    val context: TableGenContext
        get() = originalFile.virtualFile?.let {
            // Note: [serviceOrNull] is needed for parser tests which do not have any services.
            project.serviceOrNull<TableGenContextService>()?.getActiveContext(it)
        } ?: TableGenContext()

    override fun getFileType(): FileType = TableGenFileType.INSTANCE

    /**
     * Returns a sequence of all stub elements that is one of the given [elementTypes].
     */
    inline fun <reified T : PsiElement> stubStream(vararg elementTypes: IStubElementType<*, T>): Sequence<T> {
        val spine = stubbedSpine
        return (0 until spine.stubCount).asSequence().filter {
            elementTypes.contains(spine.getStubType(it))
        }.mapNotNull {
            spine.getStubPsi(it)
        }.filterIsInstance<T>()
    }

    private val myIncludeDirectives = resettableLazy {
        stubStream(TableGenStubElementTypes.INCLUDE_DIRECTIVE).toList()
    }

    /**
     * Returns all include directives in the file.
     */
    val includeDirectives: Sequence<TableGenIncludeDirective>
        get() = myIncludeDirectives.value.asSequence()

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


    private var myDirectIdMap = resettableLazy {
        withGreenStubOrAst<(TokenSet, ArrayFactory<TableGenIdentifierElement>) -> Array<TableGenIdentifierElement>>({
            it::getChildrenByType
        }) {
            it::getChildrenAsPsiElements
        }(TokenSet.create(TableGenTypes.DEF_STATEMENT, TableGenTypes.DEFVAR_STATEMENT)) {
            arrayOfNulls<TableGenIdentifierElement>(it)
        }.mapNotNull {
            val name = it.name ?: return@mapNotNull null
            name to it
        }.groupBy({
            it.first
        }) {
            TableGenIdentifierScopeNode.IdMapEntry(it.second)
        }
    }

    override val directIdMap by myDirectIdMap

    private val myUsedMacros = resettableLazy {
        stubStream(TableGenStubElementTypes.IFDEF_IFNDEF_DIRECTIVE).mapNotNullTo(mutableSetOf()) { it.macroName }
    }

    /**
     * Returns the set of macro names tested by '#ifdef'/'#ifndef' directives in the file, i.e. the macros whose
     * defined-state this file's parse result depends on.
     */
    val usedMacros: Set<String> by myUsedMacros

    override fun clearCaches() {
        super.clearCaches()
        myIncludeDirectives.reset()
        myClassMap.reset()
        myDirectIdMap.reset()
        myUsedMacros.reset()
    }
}