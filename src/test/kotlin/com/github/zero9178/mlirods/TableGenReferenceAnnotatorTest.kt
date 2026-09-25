package com.github.zero9178.mlirods

import com.github.zero9178.mlirods.model.IncludePaths
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class TableGenReferenceAnnotatorTest : BasePlatformTestCase() {

    fun `test resolving include is not flagged`() {
        myFixture.addFileToProject("sub/target.td", "")
        val main = myFixture.configureByText(
            "test.td", """
            include "sub/target.td"
        """.trimIndent()
        )
        installCompileCommands(
            project, mapOf(main.virtualFile to IncludePaths(listOf(main.virtualFile.parent)))
        )
        myFixture.checkHighlighting()
    }

    fun `test include with a missing directory is flagged`() {
        val main = myFixture.configureByText(
            "test.td", """
            include <error descr="Cannot resolve file 'missing/target.td'">"missing/target.td"</error>
        """.trimIndent()
        )
        installCompileCommands(
            project, mapOf(main.virtualFile to IncludePaths(listOf(main.virtualFile.parent)))
        )
        myFixture.checkHighlighting()
    }

    fun `test include with a missing file is flagged`() {
        myFixture.addFileToProject("sub/keep.td", "")
        val main = myFixture.configureByText(
            "test.td", """
            include <error descr="Cannot resolve file 'sub/missing.td'">"sub/missing.td"</error>
        """.trimIndent()
        )
        installCompileCommands(
            project, mapOf(main.virtualFile to IncludePaths(listOf(main.virtualFile.parent)))
        )
        myFixture.checkHighlighting()
    }

    fun `test unresolved include without a context is suppressed`() {
        // A file without an active context cannot resolve any include, so the annotator does not run there instead of
        // drowning the file in false positives.
        myFixture.configureByText(
            "test.td", """
            include "missing/target.td"
        """.trimIndent()
        )
        myFixture.checkHighlighting()
    }

    fun `test unresolved class is flagged`() {
        val main = myFixture.configureByText(
            "test.td", """
            def D : <error descr="Cannot resolve class 'Missing'">Missing</error>;
        """.trimIndent()
        )
        installCompileCommands(
            project, mapOf(main.virtualFile to IncludePaths(emptyList()))
        )
        myFixture.checkHighlighting()
    }

    fun `test resolved class is not flagged`() {
        val main = myFixture.configureByText(
            "test.td", """
            class Base;
            def D : Base;
        """.trimIndent()
        )
        installCompileCommands(
            project, mapOf(main.virtualFile to IncludePaths(emptyList()))
        )
        myFixture.checkHighlighting()
    }

    fun `test unresolved class without a context is suppressed`() {
        // Without a context, references cannot be resolved, so the annotator does not run there.
        myFixture.configureByText(
            "test.td", """
            def D : Missing;
        """.trimIndent()
        )
        myFixture.checkHighlighting()
    }

    fun `test unresolved multiclass is flagged`() {
        val main = myFixture.configureByText(
            "test.td", """
            defm d : <error descr="Cannot resolve multiclass 'Missing'">Missing</error>;
            multiclass N : <error descr="Cannot resolve multiclass 'Missing'">Missing</error>;
        """.trimIndent()
        )
        installCompileCommands(
            project, mapOf(main.virtualFile to IncludePaths(emptyList()))
        )
        myFixture.checkHighlighting()
    }

    fun `test resolved multiclass and class are not flagged`() {
        val main = myFixture.configureByText(
            "test.td", """
            multiclass M { def a; }
            class C;
            defm d : M, C;
            multiclass N : M;
        """.trimIndent()
        )
        installCompileCommands(
            project, mapOf(main.virtualFile to IncludePaths(emptyList()))
        )
        myFixture.checkHighlighting()
    }

    fun `test class deriving from itself is flagged`() {
        // TableGen crashes on either. A forward declaration is the same class as its definition.
        val main = myFixture.configureByText(
            "test.td", """
            class A<int a> : <error descr="Class 'A' cannot derive from itself">A</error><a> { int f = a; }
            class B;
            class B : <error descr="Class 'B' cannot derive from itself">B</error> { int g = 1; }
            def D : A<1>, B;
            defvar v = !add(D.f, D.g);
        """.trimIndent()
        )
        installCompileCommands(
            project, mapOf(main.virtualFile to IncludePaths(emptyList()))
        )
        myFixture.checkHighlighting()
    }

    fun `test unresolved name of a defm is flagged as multiclass or class depending on its position`() {
        // The names of a 'defm' refer to classes starting with the first name after the first one that names a class.
        val main = myFixture.configureByText(
            "test.td", """
            multiclass M { def a; }
            class C;
            defm d : M, <error descr="Cannot resolve multiclass 'Missing'">Missing</error>, C, <error descr="Cannot resolve class 'Missing'">Missing</error>;
        """.trimIndent()
        )
        installCompileCommands(
            project, mapOf(main.virtualFile to IncludePaths(emptyList()))
        )
        myFixture.checkHighlighting()
    }

    fun `test class as first name of a defm is flagged`() {
        // The first name of a 'defm' always refers to a multiclass.
        val main = myFixture.configureByText(
            "test.td", """
            class C;
            defm d : <error descr="Cannot resolve multiclass 'C'">C</error>;
        """.trimIndent()
        )
        installCompileCommands(
            project, mapOf(main.virtualFile to IncludePaths(emptyList()))
        )
        myFixture.checkHighlighting()
    }

    fun `test multiclass following the reference is flagged`() {
        val main = myFixture.configureByText(
            "test.td", """
            defm d : <error descr="Cannot resolve multiclass 'M'">M</error>;
            multiclass M { def a; }
        """.trimIndent()
        )
        installCompileCommands(
            project, mapOf(main.virtualFile to IncludePaths(emptyList()))
        )
        myFixture.checkHighlighting()
    }

    fun `test unresolved multiclass without a context is suppressed`() {
        // Without a context, references cannot be resolved, so the annotator does not run there.
        myFixture.configureByText(
            "test.td", """
            defm d : Missing;
        """.trimIndent()
        )
        myFixture.checkHighlighting()
    }

    fun `test access to a non-existent field is flagged`() {
        val main = myFixture.configureByText(
            "test.td", """
            class Base {
                int x = 0;
            }
            defvar v = Base<>.<error descr="Record 'Base' has no field named 'nope'">nope</error>;
        """.trimIndent()
        )
        installCompileCommands(
            project, mapOf(main.virtualFile to IncludePaths(emptyList()))
        )
        myFixture.checkHighlighting()
    }

    fun `test access to an existing field is not flagged`() {
        val main = myFixture.configureByText(
            "test.td", """
            class Base {
                int x = 0;
            }
            defvar v = Base<>.x;
        """.trimIndent()
        )
        installCompileCommands(
            project, mapOf(main.virtualFile to IncludePaths(emptyList()))
        )
        myFixture.checkHighlighting()
    }

    fun `test access to an inherited field is not flagged`() {
        val main = myFixture.configureByText(
            "test.td", """
            class Base {
                int x = 0;
            }
            class Derived : Base;
            defvar v = Derived<>.x;
        """.trimIndent()
        )
        installCompileCommands(
            project, mapOf(main.virtualFile to IncludePaths(emptyList()))
        )
        myFixture.checkHighlighting()
    }

    fun `test field access on an unresolved class is not double-flagged`() {
        // The unresolved class is already reported; the field access on it should stay quiet.
        val main = myFixture.configureByText(
            "test.td", """
            defvar v = <error descr="Cannot resolve class 'Missing'">Missing</error><>.nope;
        """.trimIndent()
        )
        installCompileCommands(
            project, mapOf(main.virtualFile to IncludePaths(emptyList()))
        )
        myFixture.checkHighlighting()
    }

    fun `test unknown field without a context is suppressed`() {
        // Without a context, field resolution cannot run, so the annotator does not fire.
        myFixture.configureByText(
            "test.td", """
            class Base {
                int x = 0;
            }
            defvar v = Base<>.nope;
        """.trimIndent()
        )
        myFixture.checkHighlighting()
    }
}
