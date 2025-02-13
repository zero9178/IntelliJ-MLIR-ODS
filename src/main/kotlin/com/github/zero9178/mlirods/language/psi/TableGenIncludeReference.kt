package com.github.zero9178.mlirods.language.psi

import com.github.zero9178.mlirods.language.generated.psi.TableGenIncludeDirective
import com.github.zero9178.mlirods.model.TableGenIncludeGraph
import com.intellij.openapi.components.service
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiReferenceBase

class TableGenIncludeReference(element: TableGenIncludeDirective) :
    PsiReferenceBase<TableGenIncludeDirective>(element) {
    override fun resolve(): PsiElement? {

        val file = element.containingFile
        val project = file.project

        val vf = file.virtualFile ?: return null
        val includes = project.service<TableGenIncludeGraph>().getIncludePaths(vf)

        for (include in includes) {
            val file = VirtualFileManager.getInstance().findFileByNioPath(include.resolve(element.includeSuffix))
                ?: continue
            return PsiManager.getInstance(project).findFile(file) ?: continue
        }
        return null
    }
}