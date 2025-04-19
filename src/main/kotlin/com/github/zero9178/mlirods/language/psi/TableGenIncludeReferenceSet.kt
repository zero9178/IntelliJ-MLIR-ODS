package com.github.zero9178.mlirods.language.psi

import com.github.zero9178.mlirods.language.TableGenFile
import com.github.zero9178.mlirods.language.generated.psi.TableGenIncludeDirective
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.util.Condition
import com.intellij.openapi.util.TextRange
import com.intellij.psi.*
import com.intellij.psi.impl.source.resolve.reference.impl.providers.FileInfoManager
import com.intellij.psi.impl.source.resolve.reference.impl.providers.FileReference
import com.intellij.psi.impl.source.resolve.reference.impl.providers.FileReferenceSet
import java.io.File

class TableGenIncludeReferenceSet(
    private val element: TableGenIncludeDirective,
    delegateSet: FileReferenceSet = createSet(
        element,
        false,
        true,
        false
    )
) :
    FileReferenceSet(
        delegateSet.pathString,
        delegateSet.element,
        delegateSet.startInElement,
        null,
        delegateSet.isCaseSensitive,
        delegateSet.isEndingSlashNotAllowed
    ) {

    override fun createFileReference(
        range: TextRange?,
        index: Int,
        text: String?
    ): FileReference {
        // Special file reference that highlights each directory and file in the path individually, but nevertheless
        // renames and highlights only the filename.
        // The behaviour here explicitly matches CLion's behaviour for C++ include paths.
        return TableGenIncludeFileReference(this, range, index, text)
    }

    private class TableGenIncludeFileReference(
        fileReferenceSet: TableGenIncludeReferenceSet, range: TextRange?, index: Int, text: String?
    ) : FileReference(fileReferenceSet, range, index, text) {

        private val element: TableGenIncludeDirective
            get() = fileReferenceSet.element

        override fun getFileReferenceSet() = super.getFileReferenceSet() as TableGenIncludeReferenceSet

        override fun rename(newName: String?): PsiElement? {
            // Only the filename can be renamed.
            val includeSuffix = element.includeSuffix
            // Find the filename and replace it.
            val index = includeSuffix.lastIndexOf(File(includeSuffix).name)
            if (index == -1) return super.rename(includeSuffix)
            return super.rename(includeSuffix.substring(0, index) + newName)
        }

        override fun innerResolve(
            caseSensitive: Boolean, containingFile: PsiFile
        ): Array<out ResolveResult> {
            val project = containingFile.project

            return element.includedFile?.let { file ->
                PsiManager.getInstance(project).findFile(file)?.let {
                    arrayOf(PsiElementResolveResult(it))
                }
            } ?: emptyArray()
        }

        /**
         * Returns a list of directories in which the file or directory that this reference resolves to may be
         * contained.
         */
        override fun getContexts(): Collection<PsiDirectory> {
            if (index == 0) {
                val file = fileReferenceSet.containingFile as? TableGenFile ?: return emptyList()
                return file.context.includePaths.mapNotNull {
                    PsiManager.getInstance(file.project).findDirectory(it)
                }
            }

            val reference =
                fileReferenceSet.myReferences[index - 1] as? TableGenIncludeFileReference ?: return emptyList()
            return reference.contexts.mapNotNull {
                it.findSubdirectory(reference.text)
            }
        }

        override fun createLookupItem(candidate: PsiElement?): Any? {
            return when (candidate) {
                is PsiDirectory -> {
                    LookupElementBuilder.createWithIcon(candidate).withInsertHandler { c, e ->
                        // Insert a slash afterwards if we are inserting a directory.
                        c.document.insertString(c.tailOffset, "/")
                        c.editor.caretModel.moveCaretRelatively(1, 0, false, false, false)
                    }
                }

                else -> FileInfoManager.getFileLookupItem(candidate)
            }
        }
    }

    override fun isCaseSensitive(): Boolean {
        return element.containingFile?.virtualFile?.isCaseSensitive ?: super.isCaseSensitive()
    }

    override fun couldBeConvertedTo(relative: Boolean): Boolean {
        return false
    }

    override fun getReferenceCompletionFilter(): Condition<PsiFileSystemItem> {
        // Only suggest directories and tablegen files.
        return Condition<PsiFileSystemItem> {
            it.isDirectory || it is TableGenFile
        }
    }
}