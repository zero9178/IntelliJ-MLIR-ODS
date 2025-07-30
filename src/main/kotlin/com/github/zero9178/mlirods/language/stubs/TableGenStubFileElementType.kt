package com.github.zero9178.mlirods.language.stubs

import com.github.zero9178.mlirods.language.psi.TableGenFile
import com.github.zero9178.mlirods.language.TableGenLanguage
import com.github.zero9178.mlirods.language.TableGenPreprocessingPsiBuilder
import com.intellij.lang.ASTNode
import com.intellij.lang.LanguageParserDefinitions
import com.intellij.lang.PsiBuilderFactory
import com.intellij.psi.ParsingDiagnostics
import com.intellij.psi.PsiElement
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
        return 7
    }

    companion object {
        val INSTANCE = TableGenStubFileElementType()
    }

    override fun doParseContents(
        chameleon: ASTNode, psi: PsiElement
    ): ASTNode? {
        if (psi !is TableGenFile) return null

        val project = psi.project
        val languageForParser = getLanguageForParser(psi)

        val builder = TableGenPreprocessingPsiBuilder(
            psi.context, PsiBuilderFactory.getInstance().createBuilder(
                    project, chameleon, null, languageForParser, chameleon.chars
                )
        )
        val parser = LanguageParserDefinitions.INSTANCE.forLanguage(languageForParser).createParser(project)
        val startTime = System.nanoTime()
        val node = parser.parse(this, builder)
        ParsingDiagnostics.registerParse(builder, languageForParser, System.nanoTime() - startTime)
        return node.firstChildNode
    }
}