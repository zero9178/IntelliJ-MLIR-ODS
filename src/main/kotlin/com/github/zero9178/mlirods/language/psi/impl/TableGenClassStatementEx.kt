package com.github.zero9178.mlirods.language.psi.impl

import com.github.zero9178.mlirods.index.CLASS_INDEX
import com.github.zero9178.mlirods.index.MAY_DERIVE_CLASS_INDEX
import com.github.zero9178.mlirods.index.getElements
import com.github.zero9178.mlirods.language.generated.psi.TableGenClassStatement
import com.github.zero9178.mlirods.language.psi.TableGenRecord
import com.github.zero9178.mlirods.model.TableGenIncluderSearchScope
import com.github.zero9178.mlirods.model.getProjectContextDependentCache
import com.intellij.navigation.NavigationItem
import com.intellij.openapi.util.RecursionManager
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.psi.search.GlobalSearchScope

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
     * Returns all class statements that may be the definition of this class.
     * Returns just 'this' if this statement is itself a definition and the empty list if the class is never defined.
     */
    val definitions: List<TableGenClassStatement>
        get() = getProjectContextDependentCache(this as TableGenClassStatement) { self ->
            if (!self.isDeclaration) return@getProjectContextDependentCache listOf(self)

            val name = self.name ?: return@getProjectContextDependentCache emptyList()
            CLASS_INDEX.getElements(
                name, self.project, TableGenIncluderSearchScope(self, self.project)
            ).filter { !it.isDeclaration }
        }

    /**
     * Returns the class statement defining this class.
     * As a class may first be declared and only later be defined, a reference is not guaranteed to resolve to the
     * statement carrying the template arguments, base classes and body. All statements of a class denote the same
     * class, making this the statement that should be used whenever class identity matters.
     *
     * Returns 'this' if this statement is itself a definition and null if the definition cannot be determined, either
     * because the class is never defined or because it is ambiguous which of several [definitions] is meant.
     */
    val definition: TableGenClassStatement?
        get() = definitions.singleOrNull()

    /**
     * Returns a list of all records that directly derive from this class.
     *
     * Note that this currently doesn't include inline class instantiation values.
     */
    val directivelyDerivedRecords: Sequence<TableGenRecord>
        get() = getProjectContextDependentCache(this) {
            MAY_DERIVE_CLASS_INDEX.getElements(
                name ?: return@getProjectContextDependentCache emptyList(),
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
        get() = getProjectContextDependentCache(this) {
            RecursionManager.doPreventingRecursion(this, true) {
                directivelyDerivedRecords + directivelyDerivedRecords.flatMap {
                    if (it is TableGenClassStatement) it.allDerivedRecords else emptySequence()
                }
            }?.toList() ?: emptyList()
        }.asSequence()

    override val mostDerivedRecords: Sequence<TableGenRecord>
        get() = getProjectContextDependentCache(this) {
            (allDerivedRecords + sequenceOf(this)).filter {
                when (it) {
                    is TableGenClassStatement -> it.directivelyDerivedRecords.firstOrNull() == null
                    else -> true
                }
            }.toList()
        }.asSequence()
}