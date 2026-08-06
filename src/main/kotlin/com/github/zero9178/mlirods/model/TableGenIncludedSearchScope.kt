package com.github.zero9178.mlirods.model

import com.github.zero9178.mlirods.language.psi.TableGenFile
import com.intellij.openapi.components.service
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope

/**
 * [GlobalSearchScope] restricted to a set of TableGen files taken from the include graph.
 *
 * Both module content and libraries are searched: whether a file that is part of the compilation happens to live inside
 * the project or is only reachable through the include paths does not change whether it belongs to the scope.
 */
abstract class TableGenGraphSearchScope(project: Project) : GlobalSearchScope(project) {

    /**
     * Files this scope is restricted to, or `null` if it is not restricted at all.
     */
    protected abstract val files: Set<VirtualFile>?

    override fun isSearchInModuleContent(module: Module): Boolean {
        return true
    }

    override fun isSearchInLibraries(): Boolean {
        return true
    }

    override fun contains(p0: VirtualFile): Boolean {
        return files?.contains(p0) ?: true
    }
}

/**
 * Custom [GlobalSearchScope] which only searches in TableGen files included by the file containing [element].
 */
class TableGenIncludedSearchScope(element: PsiElement, project: Project) : TableGenGraphSearchScope(project) {

    override val files: Set<VirtualFile>? = (element.containingFile as? TableGenFile)?.let {
        // TODO: The set of files returned here is an overapproximation as it also considers includes after [element].
        //       Ideally it should ignore those.
        project.service<TableGenIncludeGraphService>().getIncludedFiles(it)
    }
}

/**
 * Custom [GlobalSearchScope] which only searches in the file containing [element] and the TableGen files that include
 * it, directly or transitively.
 *
 * These are the files that can complete a declaration [element] is part of: a file including it gets to see the
 * declaration and then adds whatever follows the 'include' directive. The scope approximates that in both directions.
 * It is too large because it is the union over every expansion the file takes part in rather than a description of one
 * of them: a declaration in 'Predicate.td' that both 'X86.td' and 'AArch64.td' include, each defining the declared
 * class itself, yields two definitions even though either compilation only ever sees one of them. It is too small
 * because following [element] does not require including it: a root that includes 'Predicate.td' and then, in a
 * separate branch, the file defining the declared class leaves the declaration looking undefined. Either way a lookup
 * ends up with more candidates than it should have or with none at all, which every caller has to be able to deal with
 * anyway; never with a wrong one.
 *
 * TODO: Both would be fixed by describing a single expansion instead of the union over all of them. The ordering that
 *       requires is cheap to obtain, see the TODO on [TableGenIncludeGraphService.getTransitiveIncludersOf]. Which
 *       expansion to describe is the actual problem: the Psi this scope is built for is shared by every compilation
 *       that pastes the file in, so deriving the expansion from the file's active context alone would hand every
 *       consumer the definition of whichever root won the labeling and flag the records of the others. Either the union
 *       over all roots reaching the file has to be taken – correct, but it expands every one of them, which for a
 *       widely included declaration means expanding the whole project – or the caller has to state which context it is
 *       asking about, which in turn requires that it has one to state; nothing cached per Psi element does.
 */
class TableGenIncluderSearchScope(element: PsiElement, project: Project) : TableGenGraphSearchScope(project) {

    override val files: Set<VirtualFile>? =
        (element.containingFile as? TableGenFile)?.originalFile?.virtualFile?.let {
            project.service<TableGenIncludeGraphService>().getTransitiveIncludersOf(it) + it
        }
}
