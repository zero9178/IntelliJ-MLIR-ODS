package com.github.zero9178.mlirods.language

import com.github.zero9178.mlirods.language.generated.TableGenParser
import com.github.zero9178.mlirods.language.generated.TableGenTypes
import com.github.zero9178.mlirods.language.psi.TableGenFile
import com.github.zero9178.mlirods.language.stubs.TableGenStubFileElementType
import com.github.zero9178.mlirods.model.TableGenContext
import com.github.zero9178.mlirods.model.TableGenContextService
import com.intellij.lang.ASTNode
import com.intellij.lang.ParserDefinition
import com.intellij.lexer.Lexer
import com.intellij.openapi.components.serviceOrNull
import com.intellij.openapi.project.Project
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.IFileElementType
import com.intellij.psi.tree.TokenSet

internal class TableGenParserDefinition : ParserDefinition {
    override fun createLexer(project: Project?): Lexer = TableGenLexerAdapter()

    override fun createParser(project: Project?) = TableGenParser()

    override fun getFileNodeType(): IFileElementType = TableGenStubFileElementType.INSTANCE

    override fun getCommentTokens(): TokenSet = COMMENTS

    override fun getStringLiteralElements(): TokenSet = STRING_LITERALS

    override fun createElement(node: ASTNode?): PsiElement = TableGenTypes.Factory.createElement(node)

    override fun createFile(viewProvider: FileViewProvider) = TableGenFile(
        viewProvider,
        // Note: [serviceOrNull] is needed for parser tests which do not have any services.
        viewProvider.manager.project.serviceOrNull<TableGenContextService>()
            ?.getActiveContext(viewProvider.virtualFile) ?: TableGenContext(),
    )
}