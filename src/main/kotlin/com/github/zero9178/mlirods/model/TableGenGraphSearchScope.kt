package com.github.zero9178.mlirods.model

import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
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
