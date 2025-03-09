package com.github.zero9178.mlirods.index

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.stubs.StubIndex
import com.intellij.psi.stubs.StubIndexKey

/**
 * Returns all elements that are mapped to [key] and within files included in [scope].
 */
inline fun <Key : Any, reified Element : PsiElement> StubIndexKey<Key, Element>.getElements(
    key: Key,
    project: Project,
    scope: GlobalSearchScope
): Collection<Element> {
    return StubIndex.getElements<Key, Element>(
        this,
        key,
        project,
        scope,
        Element::class.java
    )
}
