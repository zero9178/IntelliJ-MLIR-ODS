package com.github.zero9178.mlirods.language.psi

import com.github.zero9178.mlirods.language.TableGenFile
import com.github.zero9178.mlirods.language.generated.psi.TableGenIncludeDirective
import com.intellij.openapi.util.TextRange
import com.intellij.psi.*
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
        return object : FileReference(this, range, index, text) {

            private val element: TableGenIncludeDirective
                get() = this@TableGenIncludeReferenceSet.element

            override fun rename(newName: String?): PsiElement? {
                // Only the filename can be renamed.
                val includeSuffix = element.includeSuffix
                // Find the filename and replace it.
                val index = includeSuffix.lastIndexOf(File(includeSuffix).name)
                if (index == -1) return super.rename(includeSuffix)
                return super.rename(includeSuffix.substring(0, index) + newName)
            }

            override fun innerResolve(
                caseSensitive: Boolean,
                containingFile: PsiFile
            ): Array<out ResolveResult> {
                val project = containingFile.project

                return element.includedFile?.let { file ->
                    PsiManager.getInstance(project).findFile(file)?.let {
                        arrayOf(PsiElementResolveResult(it))
                    }
                } ?: emptyArray()
            }
        }
    }

    override fun isCaseSensitive(): Boolean {
        return element.containingFile.virtualFile.isCaseSensitive
    }

    override fun couldBeConvertedTo(relative: Boolean): Boolean {
        return false
    }
}