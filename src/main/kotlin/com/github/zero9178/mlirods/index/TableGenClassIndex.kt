package com.github.zero9178.mlirods.index

import com.github.zero9178.mlirods.language.generated.psi.TableGenClassStatement
import com.github.zero9178.mlirods.model.TableGenIncludedSearchScope
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.stubs.StringStubIndexExtension
import com.intellij.psi.stubs.StubIndex
import com.intellij.psi.stubs.StubIndexKey

/**
 * Index mapping global identifiers, that refer to any kind of 'class', to the corresponding psi element.
 */
val CLASS_INDEX = StubIndexKey.createIndexKey<String, TableGenClassStatement>("CLASS_INDEX")

private class TableGenClassIndex : StringStubIndexExtension<TableGenClassStatement>() {

    override fun getKey(): StubIndexKey<String, TableGenClassStatement> {
        return CLASS_INDEX
    }
}
