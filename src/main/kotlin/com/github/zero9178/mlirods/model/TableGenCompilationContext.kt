package com.github.zero9178.mlirods.model

import com.intellij.openapi.components.service
import com.intellij.openapi.components.serviceOrNull
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.util.concurrency.annotations.RequiresReadLock

/**
 * The compilation context every cross-file lookup happens in: the expansion of one root of the include graph, i.e. the
 * order in which that root pastes every file it transitively includes into itself. What a reference resolves to, which
 * fields a record has, what a value evaluates to – all of it may differ between two roots pasting the same file in from
 * different places, which is why everything depending on it takes the context as a parameter and is cached per
 * context.
 *
 * Instances are values: two of them are equal if they stand for the same root, no matter where or when they were
 * obtained, so that results cached under one are found again under the other. They hold nothing derived from the graph
 * and may therefore be kept for as long as one likes; a lookup always consults the current graph. Which root a file is
 * resolved in by default is decided in one place only, [TableGenIncludeGraphService.compilationContextOf], and every
 * feature the IDE calls into obtains its context from there – through [activeFor] – before passing it down.
 */
sealed class TableGenCompilationContext(val project: Project) {

    /**
     * The expansion of [root], a file the compile commands name.
     */
    class Rooted internal constructor(project: Project, val root: VirtualFile) : TableGenCompilationContext(project) {

        @RequiresReadLock
        override fun positionOf(file: VirtualFile): TableGenIncludePosition? {
            val expansion = project.service<TableGenIncludeGraphService>().expansionOf(root) ?: return null
            return expansion.positionOf(file)?.let { TableGenIncludePosition(expansion, it) }
        }

        override fun equals(other: Any?): Boolean = this === other || (other is Rooted && root == other.root)

        override fun hashCode(): Int = root.hashCode()

        override fun toString(): String = "Rooted(${root.name})"
    }

    /**
     * The context of a file that no root reaches: it sees nothing but itself. [file] is `null` for a file that is not
     * backed by a [VirtualFile] at all.
     */
    class SelfOnly internal constructor(
        project: Project,
        val file: VirtualFile?,
    ) : TableGenCompilationContext(project) {

        override fun positionOf(file: VirtualFile): TableGenIncludePosition? = null

        override fun equals(other: Any?): Boolean = this === other || (other is SelfOnly && file == other.file)

        override fun hashCode(): Int = file.hashCode()

        override fun toString(): String = "SelfOnly(${file?.name})"
    }

    /**
     * Returns where [file] sits within this context, or `null` if the context does not paste [file] in at all. This is
     * what any lookup that has to respect the order in which files are pasted into each other is built on, see
     * [TableGenIncludePosition].
     */
    @RequiresReadLock
    abstract fun positionOf(file: VirtualFile): TableGenIncludePosition?

    /**
     * Returns what is visible from the position of [element] within this context, see [TableGenVisibility].
     */
    @RequiresReadLock
    fun at(element: PsiElement): TableGenVisibility = TableGenVisibility(this, element)

    companion object {
        /**
         * Returns the context [element] is resolved in by default, i.e. the one its file derives from the include
         * graph. This is what a feature the IDE calls into without a context of its own – an annotator, a completion
         * contributor, a reference resolved by the platform – starts from.
         */
        @RequiresReadLock
        fun activeFor(element: PsiElement): TableGenCompilationContext {
            val project = element.project
            val virtualFile = element.containingFile?.originalFile?.virtualFile
            // Note: [serviceOrNull] is needed for parser tests which do not have any services.
            return project.serviceOrNull<TableGenIncludeGraphService>()?.compilationContextOf(virtualFile)
                ?: SelfOnly(project, virtualFile)
        }
    }
}
