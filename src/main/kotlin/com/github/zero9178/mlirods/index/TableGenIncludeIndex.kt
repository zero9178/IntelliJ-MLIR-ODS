package com.github.zero9178.mlirods.index

import com.github.zero9178.mlirods.language.COMMENTS
import com.github.zero9178.mlirods.language.TableGenFileType
import com.github.zero9178.mlirods.language.TableGenLexerAdapter
import com.github.zero9178.mlirods.language.generated.TableGenTypes
import com.github.zero9178.mlirods.language.generated.psi.TableGenIncludeDirective
import com.github.zero9178.mlirods.language.psi.TableGenFile
import com.github.zero9178.mlirods.language.psi.impl.TableGenPsiImplUtil.Companion.getStringValue
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.impl.LoadTextUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.ThrowableComputable
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.psi.TokenType
import com.intellij.psi.util.parentOfType
import com.intellij.util.concurrency.annotations.RequiresReadLock
import com.intellij.util.indexing.DefaultFileTypeSpecificInputFilter
import com.intellij.util.indexing.DumbModeAccessType
import com.intellij.util.indexing.FileBasedIndex
import com.intellij.util.indexing.FileContent
import com.intellij.util.indexing.ID
import com.intellij.util.indexing.SingleEntryFileBasedIndexExtension
import com.intellij.util.indexing.SingleEntryIndexer
import com.intellij.util.io.DataExternalizer
import com.intellij.util.io.DataInputOutputUtil
import com.intellij.util.io.IOUtil
import java.io.DataInput
import java.io.DataOutput

/**
 * A single 'include' directive of a file as recorded by [INCLUDE_INDEX].
 */
data class TableGenIncludeEntry(
    /**
     * Value of the string literal of the directive, i.e. the path that is looked up in the include paths.
     */
    val suffix: String,
    /**
     * Offset of the 'include' keyword within the file.
     */
    val offset: Int,
)

/**
 * Index mapping a file to all 'include' directives it contains, in lexical order.
 *
 * The point of this index is to make the include graph's edge computation independent of a file's psi: reading the
 * suffix of every include directive through the psi requires the file's entire stub tree to be deserialized – or worse,
 * the file to be parsed – which dwarfs the cost of the graph update itself.
 */
val INCLUDE_INDEX = ID.create<Int, List<TableGenIncludeEntry>>("TableGenIncludeIndex")

internal class TableGenIncludeIndex : SingleEntryFileBasedIndexExtension<List<TableGenIncludeEntry>>() {

    override fun getName() = INCLUDE_INDEX

    override fun getVersion() = 0

    override fun getInputFilter() = DefaultFileTypeSpecificInputFilter(TableGenFileType.INSTANCE)

    override fun getValueExternalizer(): DataExternalizer<List<TableGenIncludeEntry>> = Externalizer

    override fun getIndexer() = object : SingleEntryIndexer<List<TableGenIncludeEntry>>(false) {
        override fun computeValue(inputData: FileContent) = computeIncludeEntries(inputData.contentAsText)
    }

    private object Externalizer : DataExternalizer<List<TableGenIncludeEntry>> {
        override fun save(out: DataOutput, value: List<TableGenIncludeEntry>) {
            DataInputOutputUtil.writeINT(out, value.size)
            value.forEach {
                IOUtil.writeUTF(out, it.suffix)
                DataInputOutputUtil.writeINT(out, it.offset)
            }
        }

        override fun read(input: DataInput): List<TableGenIncludeEntry> {
            val size = DataInputOutputUtil.readINT(input)
            return List(size) {
                TableGenIncludeEntry(IOUtil.readUTF(input), DataInputOutputUtil.readINT(input))
            }
        }
    }
}

/**
 * Returns all 'include' directives contained in [text] in lexical order.
 *
 * This deliberately only runs the lexer rather than the parser, making it roughly as expensive as reading the file.
 *
 * TODO: The preprocessor is currently not interpreted, causing include directives within inactive '#ifdef' blocks to be
 *       reported as well. Fixing this requires the preprocessor state machine of 'TableGenPreprocessingPsiBuilder' to
 *       be shared with this function. Note that the index must stay a pure function of [text]: once defines propagate
 *       from a file to the files it includes, the *result* of preprocessing is no longer a property of the file alone.
 *       The index should then record the preprocessor directives as well and leave interpreting them to whoever knows
 *       the defines that are active.
 */
private fun computeIncludeEntries(text: CharSequence): List<TableGenIncludeEntry> {
    val lexer = TableGenLexerAdapter()
    lexer.start(text)

    val result = mutableListOf<TableGenIncludeEntry>()
    while (lexer.tokenType != null) {
        if (lexer.tokenType != TableGenTypes.INCLUDE) {
            lexer.advance()
            continue
        }

        val offset = lexer.tokenStart
        lexer.advance()
        // Mimic the parser, which never sees whitespace or comments.
        while (lexer.tokenType.let { it == TokenType.WHITE_SPACE || COMMENTS.contains(it) }) lexer.advance()

        // Not an include directive after all. Do not consume the current token as it may start one.
        if (lexer.tokenType != TableGenTypes.LINE_STRING_LITERAL) continue

        result.add(TableGenIncludeEntry(getStringValue(lexer.tokenText), offset))
        lexer.advance()
    }
    return result
}

/**
 * Returns all 'include' directives of [file] in lexical order.
 */
@RequiresReadLock
fun getIncludeEntries(project: Project, file: VirtualFile): List<TableGenIncludeEntry> {
    if (!file.isValid) return emptyList()

    // A file that is being edited is only reflected in the index once it has been saved, making its entries have to be
    // computed from the document instead. This only ever applies to the handful of files that are open and is still
    // cheaper than going through the file's psi.
    val documentManager = FileDocumentManager.getInstance()
    documentManager.getCachedDocument(file)?.let {
        if (documentManager.isDocumentUnsaved(it)) return computeIncludeEntries(it.immutableCharSequence)
    }

    // Query the index even during dumb mode rather than waiting for indexing to finish: the include graph is what
    // contributes the content roots that indexing works off, so waiting on indexing is prone to deadlock. Only files
    // that are queued for re-indexing are withheld from us, every other file still answers from the index.
    DumbModeAccessType.RELIABLE_DATA_ONLY.ignoreDumbMode(ThrowableComputable {
        FileBasedIndex.getInstance().getSingleEntryIndexData(INCLUDE_INDEX, file, project)
    })?.let { return it }

    // Either [file] is queued for re-indexing or it is not part of the index at all, the latter being the case for
    // files outside of the project's content roots. Lexing the file directly remains cheaper than going through its
    // psi, which would have to be parsed from scratch as no stubs exist for it either.
    return computeIncludeEntries(LoadTextUtil.loadText(file))
}
