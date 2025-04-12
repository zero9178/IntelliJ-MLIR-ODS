package com.github.zero9178.mlirods.index

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.stubs.StubIndex
import com.intellij.psi.stubs.StubIndexKey
import com.intellij.util.Processor
import com.intellij.util.indexing.IdFilter

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

/**
 * Invokes [processor] with all elements mapped to [key] included in [scope].
 */
inline fun <Key : Any, reified Element : PsiElement> StubIndexKey<Key, Element>.processElements(
    key: Key,
    project: Project,
    scope: GlobalSearchScope,
    processor: Processor<in Element>
) {
    StubIndex.getInstance().processElements(
        this,
        key,
        project,
        scope,
        Element::class.java,
        processor
    )
}

/**
 * Invokes [processor] with all keys of the index that are included in [scope] and [idFilter].
 */
fun <Key : Any, Element : PsiElement> StubIndexKey<Key, Element>.processAllKeys(
    processor: Processor<in Key>,
    scope: GlobalSearchScope,
    idFilter: IdFilter? = null
) {
    StubIndex.getInstance().processAllKeys(this, processor, scope, idFilter)
}