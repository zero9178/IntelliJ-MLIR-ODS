package com.github.zero9178.mlirods.language.stubs

import com.github.zero9178.mlirods.language.TableGenLanguage
import com.github.zero9178.mlirods.language.TableGenPreprocessingPsiBuilder
import com.github.zero9178.mlirods.language.psi.TableGenFile
import com.intellij.lang.ASTNode
import com.intellij.lang.LanguageParserDefinitions
import com.intellij.lang.PsiBuilderFactory
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.psi.ParsingDiagnostics
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.stubs.DefaultStubBuilder
import com.intellij.psi.stubs.PsiFileStubImpl
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.tree.IStubFileElementType
import org.jetbrains.annotations.NonNls

class TableGenFileStub(tableGenFile: TableGenFile?) : PsiFileStubImpl<TableGenFile>(tableGenFile) {
    override fun getType(): IStubFileElementType<TableGenFileStub> {
        return TableGenStubFileElementType.INSTANCE
    }
}

class TableGenStubFileElementType :
    IStubFileElementType<TableGenFileStub>("FILE", TableGenLanguage.INSTANCE) {
    override fun getExternalId(): @NonNls String {
        return "tablegen." + toString()
    }

    override fun getStubVersion(): Int {
        return 30
    }

    override fun deserialize(
        dataStream: StubInputStream,
        parentStub: StubElement<*>?
    ): TableGenFileStub {
        return TableGenFileStub(null)
    }

    override fun getBuilder() = object : DefaultStubBuilder() {
        override fun createStubForFile(file: PsiFile): StubElement<*> {
            return TableGenFileStub(file as? TableGenFile)
        }
    }

    companion object {
        val INSTANCE = TableGenStubFileElementType()
    }

    override fun doParseContents(
        chameleon: ASTNode, psi: PsiElement
    ): ASTNode? {
        if (psi !is TableGenFile) {
            // This element type is only registered for 'TableGenLanguage', whose parser definition only ever creates
            // 'TableGenFile's. Returning null leaves the chameleon unparsed, i.e. the file without any PSI at all,
            // which is indistinguishable from an empty file.
            thisLogger().error("Parsing the contents of a ${psi.javaClass.name} as TableGen")
            return null
        }

        val project = psi.project
        val languageForParser = getLanguageForParser(psi)

        val builder = TableGenPreprocessingPsiBuilder(
            PsiBuilderFactory.getInstance().createBuilder(
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