package com.github.zero9178.mlirods.language.psi.impl

import com.github.zero9178.mlirods.index.MAY_DERIVE_CLASS_INDEX
import com.github.zero9178.mlirods.index.getElements
import com.github.zero9178.mlirods.language.generated.psi.TableGenClassStatement
import com.github.zero9178.mlirods.language.psi.TableGenClassReference
import com.github.zero9178.mlirods.language.psi.TableGenRecord
import com.github.zero9178.mlirods.model.TableGenCompilationContext
import com.github.zero9178.mlirods.model.getProjectContextDependentCache
import com.intellij.navigation.NavigationItem
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.concurrency.annotations.RequiresReadLock

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
     * Returns all statements of this class preceding this one within [context], be they declarations or
     * definitions. The statements are in the order they have within that text, making the last one the closest to
     * this statement.
     */
    @RequiresReadLock
    fun previousStatements(context: TableGenCompilationContext): List<TableGenClassStatement> =
        name?.let { TableGenClassReference.findVisibleClasses(it, this, context) } ?: emptyList()

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
     * Returns a list of all records that directly derive from this class, i.e. whose base class reference resolves to
     * this statement within [context].
     *
     * Note that this currently doesn't include inline class instantiation values.
     */
    @RequiresReadLock
    fun directivelyDerivedRecords(context: TableGenCompilationContext): Sequence<TableGenRecord> =
        getProjectContextDependentCache(this, context) {
            MAY_DERIVE_CLASS_INDEX.getElements(
                name ?: return@getProjectContextDependentCache emptyList(),
                project,
                GlobalSearchScope.allScope(project)
            ).asSequence().filter {
                it.baseClassRefs.any { ref ->
                    ref.referencedDefinitionBlocking(context) == this
                }
            }.toList()
        }.asSequence()

    /**
     * Returns a list of all records that directly or indirectly derive from this class within [context].
     *
     * Note that this currently doesn't include inline class instantiation values.
     */
    @RequiresReadLock
    fun allDerivedRecords(context: TableGenCompilationContext): Sequence<TableGenRecord> =
        getProjectContextDependentCache(this, context) {
            (directivelyDerivedRecords(context) + directivelyDerivedRecords(context).flatMap {
                if (it is TableGenClassStatement) it.allDerivedRecords(context) else emptySequence()
            }).toList()
        }.asSequence()

    @RequiresReadLock
    override fun mostDerivedRecords(context: TableGenCompilationContext): Sequence<TableGenRecord> =
        getProjectContextDependentCache(this, context) {
            (allDerivedRecords(context) + sequenceOf(this)).filter {
                when (it) {
                    is TableGenClassStatement -> it.directivelyDerivedRecords(context).firstOrNull() == null
                    else -> true
                }
            }.toList()
        }.asSequence()
}
