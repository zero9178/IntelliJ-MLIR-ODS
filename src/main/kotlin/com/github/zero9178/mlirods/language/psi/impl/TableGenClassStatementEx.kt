package com.github.zero9178.mlirods.language.psi.impl

import com.github.zero9178.mlirods.index.MAY_DERIVE_CLASS_INDEX
import com.github.zero9178.mlirods.index.getElements
import com.github.zero9178.mlirods.language.generated.psi.TableGenClassStatement
import com.github.zero9178.mlirods.language.psi.TableGenRecord
import com.intellij.navigation.NavigationItem
import com.intellij.openapi.util.RecursionManager
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.CachedValuesManager

interface TableGenClassStatementEx : PsiNameIdentifierOwner, NavigationItem, TableGenRecord {
    /**
     * Is true if this class is a declaration.
     * A declaration is a class statement that defines no template arguments, parent class list nor has a body.
     * Declarations can be re-redefined once or declared multiple times.
     */
    val isDeclaration: Boolean

    /**
     * Is true if this class has a body.
     */
    val hasBody: Boolean

    /**
     * Returns a list of all records that directly derive from this class.
     *
     * Note that this currently doesn't include inline class instantiation values.
     */
    val directivelyDerivedRecords: Sequence<TableGenRecord>
        get() = CachedValuesManager.getProjectPsiDependentCache(this) {
            MAY_DERIVE_CLASS_INDEX.getElements(
                name ?: return@getProjectPsiDependentCache emptyList(),
                project,
                GlobalSearchScope.allScope(project)
            ).asSequence().filter {
                it.baseClassRefs.any { ref ->
                    ref.referencedClass == this
                }
            }.toList()
        }.asSequence()

    /**
     * Returns a list of all records that directly or indirectly derive from this class.
     *
     * Note that this currently doesn't include inline class instantiation values.
     */
    val allDerivedRecords: Sequence<TableGenRecord>
        get() = CachedValuesManager.getProjectPsiDependentCache(this) {
            RecursionManager.doPreventingRecursion(this, true) {
                directivelyDerivedRecords + directivelyDerivedRecords.flatMap {
                    if (it is TableGenClassStatement) it.allDerivedRecords else emptySequence()
                }
            }?.toList() ?: emptyList()
        }.asSequence()
}