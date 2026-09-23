package com.github.zero9178.mlirods.model

import com.github.zero9178.mlirods.language.generated.psi.TableGenIncludeDirective
import com.github.zero9178.mlirods.language.psi.TableGenFile
import com.github.zero9178.mlirods.language.psi.compareTo
import com.github.zero9178.mlirods.language.psi.isBefore
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.startOffset
import com.intellij.util.concurrency.annotations.RequiresReadLock
import java.util.concurrent.ConcurrentHashMap

/**
 * Where a file sits within the expansion of a root, i.e. within the order in which that root pastes every file it
 * transitively includes into itself.
 *
 * Neither this class nor the graph knows about anything smaller than a file: wherever an answer depends on where within
 * a file something is, it is given in terms of the 'include' directive the position has to be compared with, see
 * [TableGenVisibility] for who does so.
 */
class TableGenIncludePosition internal constructor(
    private val myRootExpansion: TableGenIncludeGraphService.Expansion,
    private val myPosition: Int,
) {

    /**
     * Answers "which files may contain something visible from here?" for every place within the file at once.
     *
     * The only thing that changes the answer while reading through the file is an 'include' directive, as it is what
     * pastes further files in. A file with 'n' directives therefore has just 'n + 1' answers, one per gap between them,
     * and a place within the file picks its answer by nothing but the number of directives preceding it. Given the
     * files the directives resolve to in lexical order as [included], element 'i' of the result is the answer after the
     * first 'i' of them, making the first element the answer at the very first token of the file:
     * ```
     *                     // [0]: whatever was pasted in before this file, plus this file
     * include "X.td"
     *                     // [1]: additionally 'X.td' and everything it pastes in
     * include "Seen.td"
     *                     // [2]: same as [1] if 'Seen.td' was pasted in before already, by 'X.td' for example
     * ```
     * Files pasted in after this file by whoever includes it are in none of the sets.
     *
     * A file being part of a set does not mean that all of it is visible: the file itself and the files in the middle
     * of pasting it in, see [pastedThrough], are part of every set although only their beginning is part of the text by
     * then. The sets are views and cost nothing to create.
     */
    fun visibleFilesAfterEach(included: List<VirtualFile>): List<Set<VirtualFile>> {
        // Everything preceding the file and the file itself.
        var end = myPosition + 1
        return listOf(myRootExpansion.filesBefore(end)) + included.map { file ->
            val position = myRootExpansion.positionOf(file)
            // Only a file following this one is pasted in by the directive, and everything it pastes in with it.
            // Anything else was either pasted in before the file already or is one of the files including it. The
            // maximum accounts for a file pasted in by an earlier directive having pasted in whatever it includes.
            if (position != null && position > myPosition) end = maxOf(end, myRootExpansion.endOf(position))
            myRootExpansion.filesBefore(end)
        }
    }

    /**
     * Maps every file that is in the middle of pasting the file in to the file it directly includes to do so. The map
     * has one entry per level of nesting and is therefore computed once per file rather than searched for per query.
     */
    private val myPastedThrough: Map<VirtualFile, VirtualFile> by lazy(LazyThreadSafetyMode.PUBLICATION) {
        val result = HashMap<VirtualFile, VirtualFile>()
        var child = myPosition
        while (true) {
            val parent = myRootExpansion.parentOf(child)
            if (parent < 0) break

            result[myRootExpansion.fileAt(parent)] = myRootExpansion.fileAt(child)
            child = parent
        }
        result
    }

    /**
     * Returns the file [includer] directly includes to get the file pasted in, or `null` if [includer] is not in the
     * middle of pasting the file in. It is the first directive of [includer] including the returned file that separates
     * what precedes the file from what follows it.
     */
    fun pastedThrough(includer: VirtualFile): VirtualFile? = myPastedThrough[includer]

    /**
     * Compares [first] and [second], both of which must be part of the expansion, by when the expansion begins pasting
     * them in. A file beginning earlier does not precede the other one entirely if it is what pastes the other one in,
     * see [pastedThrough].
     */
    fun compareBeginOf(first: VirtualFile, second: VirtualFile): Int {
        val firstPosition = requireNotNull(myRootExpansion.positionOf(first)) { "file must be part of the expansion" }
        val secondPosition = requireNotNull(myRootExpansion.positionOf(second)) { "file must be part of the expansion" }
        return firstPosition.compareTo(secondPosition)
    }

    /**
     * Returns the file [includer] directly includes to get [included] pasted in, or `null` if [includer] is not what
     * pastes [included] in. It is the first directive of [includer] including the returned file that separates what
     * precedes [included] from what follows it.
     */
    fun pastedThrough(includer: VirtualFile, included: VirtualFile): VirtualFile? {
        val ancestor = myRootExpansion.positionOf(includer) ?: return null
        var child = myRootExpansion.positionOf(included) ?: return null
        if (child <= ancestor || child >= myRootExpansion.endOf(ancestor)) return null

        while (myRootExpansion.parentOf(child) != ancestor) child = myRootExpansion.parentOf(child)
        return myRootExpansion.fileAt(child)
    }
}

/**
 * Everything about the visibility of declarations from within a file that does not depend on where in the file one is
 * looking from. Cached per file and compilation context, turning what is left to do per lookup into a binary search.
 */
private class FileVisibility(
    val position: TableGenIncludePosition,
    /**
     * 'include' directives of the file in lexical order. Directives that do not resolve are left out as they do not
     * paste anything in.
     */
    val directives: List<TableGenIncludeDirective>,
    /**
     * The files having something visible after the first 'i' of [directives], see
     * [TableGenIncludePosition.visibleFilesAfterEach].
     */
    val files: List<Set<VirtualFile>>,
)

@RequiresReadLock
private fun computeFileVisibility(file: TableGenFile, context: TableGenCompilationContext): FileVisibility? {
    val position = file.originalFile.virtualFile?.let { context.positionOf(it) } ?: return null
    // What a directive resolves to is a function of the file and the graph, not of the context: a file's 'include'
    // directives are resolved once, against the include paths of the root it derives its context from, no matter which
    // root is looking at it.
    val included = file.includeDirectives.mapNotNull { directive ->
        directive.includedFile?.let { directive to it }
    }.toList()
    return FileVisibility(
        position, included.map { it.first }, position.visibleFilesAfterEach(included.map { it.second }),
    )
}

@RequiresReadLock
private fun getFileVisibility(file: TableGenFile, context: TableGenCompilationContext): FileVisibility? {
    // Unlike most of what is cached per context, this depends on nothing but the file and the graph, and therefore
    // survives edits of other files.
    val byContext = CachedValuesManager.getCachedValue(file) {
        CachedValueProvider.Result.create(
            ConcurrentHashMap<TableGenCompilationContext, FileVisibility>(),
            file,
            file.project.service<TableGenIncludeGraphService>().graphChangedModificationTracker,
        )
    }
    byContext[context]?.let { return it }
    // A file the context does not paste in is not remembered, as finding that out is a lookup in the graph.
    val computed = computeFileVisibility(file, context) ?: return null
    return byContext.putIfAbsent(context, computed) ?: computed
}

/**
 * Decides which declarations are visible from the position of a given element in the way TableGen does: a file is
 * nothing but the text that results from pasting every file it includes into it, and a declaration is visible if it
 * precedes the position in that text. A declaration of another file is therefore only visible if the 'include'
 * directive making it part of the text precedes the position – or, if it is a file including the one looked from, if
 * the declaration precedes the directive doing so.
 *
 * A lookup consists of two steps: querying an index restricted to [scope], which rules out every file no part of which
 * is visible, followed by filtering the result by [isVisible], which rules out whatever is declared too late in the
 * files that are left. Creating an instance is a binary search over the 'include' directives of the file and every
 * [isVisible] query a hash lookup, neither of which requires the syntax tree of any file to be loaded.
 *
 * Unlike the [TableGenCompilationContext] it is derived from, an instance is bound to the element and to the graph as
 * it was when the instance was created: it must not outlive the read action it was created in, is never passed around
 * and never serves as a cache key. Obtain one through [TableGenCompilationContext.at] right where an index is
 * queried.
 */
class TableGenVisibility @RequiresReadLock internal constructor(
    private val myContext: TableGenCompilationContext,
    private val myElement: PsiElement,
) {

    val project: Project
        get() = myContext.project

    private val myFile = myElement.containingFile

    private val myVirtualFile: VirtualFile? = myFile?.originalFile?.virtualFile

    private val myPosition: TableGenIncludePosition?

    /**
     * The files that may contain something visible from the element: the file of the element itself, the files pasted
     * in before it and the files pasted in by the 'include' directives of the file preceding the element. Not
     * everything within these files is necessarily visible, see [isVisible]. A file the context does not paste in sees
     * nothing but itself.
     */
    private val myVisibleFiles: Set<VirtualFile>

    init {
        val fileVisibility = (myFile as? TableGenFile)?.let { getFileVisibility(it, myContext) }
        myPosition = fileVisibility?.position
        myVisibleFiles = if (fileVisibility == null) setOfNotNull(myVirtualFile) else {
            // Never reporting a match makes the search return the inverted insertion point, which is the number of
            // directives preceding the element.
            val preceding = fileVisibility.directives.binarySearch {
                val isBefore = requireNotNull(it.isBefore(myElement)) { "directives should have been in the same file" }
                if (isBefore) -1 else 1
            }.inv()
            fileVisibility.files[preceding]
        }
    }

    /**
     * Scope containing exactly the files that have something visible from the element, including the file of the
     * element itself. A file the context does not paste in sees nothing but itself.
     */
    val scope: GlobalSearchScope
        get() {
            val visible = myVisibleFiles
            return object : TableGenGraphSearchScope(project) {
                override val files: Set<VirtualFile> = visible
            }
        }

    /**
     * Orders declarations visible from the element by where they are within the text that results from pasting every
     * file in, making the last of them the one closest to the element. Two declarations of the same file are in the
     * order they have within it. The file of any other two either precedes the other file entirely, or is what pastes
     * the other file in, where the 'include' directive doing so stands in for everything it pastes in.
     */
    val textOrder: Comparator<PsiElement> = Comparator(::compareTextOrder)

    private fun compareTextOrder(first: PsiElement, second: PsiElement): Int {
        first.compareTo(second)?.let { return it }

        val firstFile = first.containingFile as TableGenFile
        val firstVirtualFile = firstFile.originalFile.virtualFile
        val secondVirtualFile = second.containingFile.originalFile.virtualFile
        val position = requireNotNull(myPosition) { "a file the context does not paste in sees nothing but itself" }
        if (position.compareBeginOf(firstVirtualFile, secondVirtualFile) > 0) return -compareTextOrder(second, first)

        val directive = position.pastedThrough(firstVirtualFile, secondVirtualFile)?.let {
            firstFile.findIncludeDirectiveOf(it)
        } ?: return -1
        val isBefore = requireNotNull(first.isBefore(directive)) { "directive should have been in the same file" }
        return if (isBefore) -1 else 1
    }

    /**
     * Returns true if [declaration] is part of the file of the element.
     */
    fun isInSameFile(declaration: PsiElement): Boolean =
        myVirtualFile != null && declaration.containingFile?.originalFile?.virtualFile == myVirtualFile

    /**
     * Returns true if [declaration], which must be part of a file of [scope], is visible from the element, i.e. if it
     * precedes it.
     */
    fun isVisible(declaration: PsiElement): Boolean {
        val file = declaration.containingFile as? TableGenFile ?: return false
        val virtualFile = file.originalFile.virtualFile
        // The element is part of a copy of the file during completion while a declaration found through an index is
        // part of the original. Offsets remain comparable as the text preceding the completion position is identical in
        // both.
        // TODO: The offset of the declaration requires the syntax tree of the original, which a caller resolving a
        //       reference within a copy may have disallowed loading. The comparison is also only exact for the
        //       completion position: whatever follows it is shifted by the text inserted into the copy.
        if (virtualFile == myVirtualFile)
            return declaration.isBefore(myElement) ?: (declaration.startOffset < myElement.startOffset)

        assert(virtualFile in myVisibleFiles) { "declaration must be part of a file of the scope" }

        // The only files of the scope not pasted in entirely are the ones in the middle of pasting this file in. What
        // they declare is only visible if it precedes the directive doing so.
        val directive = virtualFile?.let { myPosition?.pastedThrough(it) }?.let { file.findIncludeDirectiveOf(it) }
            ?: return true
        return requireNotNull(declaration.isBefore(directive)) { "directive should have been in the same file" }
    }
}
