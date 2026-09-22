package com.github.zero9178.mlirods.language.psi.impl

import com.github.zero9178.mlirods.index.MAY_DERIVE_CLASS_INDEX
import com.github.zero9178.mlirods.index.getElements
import com.github.zero9178.mlirods.language.generated.psi.TableGenClassStatement
import com.github.zero9178.mlirods.language.psi.TableGenClassReference
import com.github.zero9178.mlirods.language.psi.TableGenRecord
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
     * Returns all statements of this class preceding this one be they declarations or definitions.
     * The statements are in the order they have within that text, making the last one the
     * closest to this statement.
     */
    val previousStatements: List<TableGenClassStatement>
        get() = getProjectContextDependentCache(this) { self ->
            val name = self.name ?: return@getProjectContextDependentCache emptyList()
            TableGenClassReference.findVisibleClasses(name, self)
        }

    /**
     * Returns true if this statement and [other] denote the same class.
     * A class may be declared any number of times before it is defined, leaving several statements of one and the same
     * class. Which of them a reference resolves to depends on what is visible from it, so class identity must never be
     * decided by comparing statements. Two statements rather denote the same class if they are of the same name: a
     * compilation has exactly one class per name, and the two statements only ever get compared where both are pasted
     * in, e.g. because a record deriving from one is used where the other is expected.
     *
     * Note that the statements need not see each other: a file derives its context from a single root, while the
     * compilation the statements are compared for may be another one that pastes the file in from elsewhere. Even
     * statements of files neither of which includes the other are one and the same class within a compilation
     * including both.
     */
    fun isSameClassAs(other: TableGenClassStatement): Boolean = this === other || name != null && name == other.name

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