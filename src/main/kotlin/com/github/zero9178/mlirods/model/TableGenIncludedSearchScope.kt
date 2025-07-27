package com.github.zero9178.mlirods.model

import com.github.zero9178.mlirods.language.psi.TableGenFile
import com.intellij.openapi.components.service
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope

/**
 * Custom [GlobalSearchScope] which only searches in TableGen files included by the file containing [element].
 */
class TableGenIncludedSearchScope(element: PsiElement, project: Project) : GlobalSearchScope(project) {

    private val mySet: Set<VirtualFile>? = (element.containingFile as? TableGenFile)?.let {
        // TODO: The set of files returned here is an overapproximation as it also considers includes after [element].
        //       Ideally it should ignore those.
        project.service<TableGenContextService>().getIncludedFiles(it)
    }

    override fun isSearchInModuleContent(module: Module): Boolean {
        return true
    }

    override fun isSearchInLibraries(): Boolean {
        return true
    }

    override fun contains(p0: VirtualFile): Boolean {
        return mySet?.contains(p0) ?: true
    }
}