package com.github.zero9178.mlirods

import com.github.zero9178.mlirods.model.IncludePaths
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class TableGenSemanticAnnotatorTest : BasePlatformTestCase() {

    fun `test all required arguments provided`() {
        doResolvingTest(
            """
            class C<int a, int b>;
            def D : C<1, 2>;
        """.trimIndent()
        )
    }

    fun `test default arguments may be omitted`() {
        doResolvingTest(
            """
            class C<int a, int b = 0>;
            def D : C<1>;
        """.trimIndent()
        )
    }

    fun `test duplicate named argument`() {
        doResolvingTest(
            """
            class C<int a, int b = 0>;
            def D : C<a = 1, <error descr="Template argument 'a' is assigned more than once">a = 2</error>>;
        """.trimIndent()
        )
    }

    fun `test duplicate positional and named argument`() {
        doResolvingTest(
            """
            class C<int a, int b = 0>;
            def D : C<1, <error descr="Template argument 'a' is assigned more than once">a = 2</error>>;
        """.trimIndent()
        )
    }

    fun `test unknown named argument`() {
        doResolvingTest(
            """
            class C<int a = 0>;
            def D : C<<error descr="Class 'C' has no template argument named 'b'">b = 1</error>>;
        """.trimIndent()
        )
    }

    fun `test too many positional arguments`() {
        doResolvingTest(
            """
            class C<int a, int b>;
            def D : C<1, 2, <error descr="Too many arguments for class 'C'; expected at most 2">3</error>>;
        """.trimIndent()
        )
    }

    fun `test missing required argument`() {
        doResolvingTest(
            """
            class C<int a>;
            def D : <error descr="Missing value for required template argument 'a'">C</error>;
        """.trimIndent()
        )
    }

    fun `test class used as type is not validated`() {
        // A class referenced as a field type does not pass template arguments and must not be flagged.
        doResolvingTest(
            """
            class C<int a>;
            class D { C f; }
        """.trimIndent()
        )
    }

    fun `test class with decl`() {
        doResolvingTest(
            """
            class C;

            class C<int a>;

            class D : C<0>;
        """.trimIndent()
        )
    }

    fun `test argument of matching type is not flagged`() {
        doResolvingTest(
            """
            class C<string a>;
            def D : C<"hello">;
        """.trimIndent()
        )
    }

    fun `test argument of a convertible type is not flagged`() {
        // A 'bit' is convertible to an 'int'.
        doResolvingTest(
            """
            class B { bit x = 1; }
            class C<int a>;
            def D : C<B<>.x>;
        """.trimIndent()
        )
    }

    fun `test argument of a mismatching type is flagged`() {
        doResolvingTest(
            """
            class C<string a>;
            def D : C<<error descr="Value of type 'int' cannot be assigned to template argument 'a' of type 'string'">1</error>>;
        """.trimIndent()
        )
    }

    fun `test named argument of a mismatching type is flagged`() {
        doResolvingTest(
            """
            class C<int a = 0>;
            def D : C<a = <error descr="Value of type 'string' cannot be assigned to template argument 'a' of type 'int'">"oops"</error>>;
        """.trimIndent()
        )
    }

    fun `test undef argument is not flagged`() {
        doResolvingTest(
            """
            class C<int a>;
            def D : C<?>;
        """.trimIndent()
        )
    }

    fun `test argument of a subclass record type is not flagged`() {
        doResolvingTest(
            """
            class Base;
            class Derived : Base;
            class C<Base b>;
            def D : C<Derived<>>;
        """.trimIndent()
        )
    }

    fun `test argument of an unrelated record type is flagged`() {
        doResolvingTest(
            """
            class Base;
            class Other;
            class C<Base b>;
            def D : C<<error descr="Value of type 'Other' cannot be assigned to template argument 'b' of type 'Base'">Other<></error>>;
        """.trimIndent()
        )
    }

    /**
     * Creates the files the forward-declaration tests share, mirroring LLVM: 'Target.td' includes
     * 'TargetInstrPredicate.td', which forward declares 'Instruction' and already uses it as a template argument type,
     * and only then defines 'Instruction' itself. 'unrelated.td' stands for the files defining a class of the same name
     * without taking part in the compilation, as those in LLVM's 'test/TableGen' do.
     */
    private fun addForwardDeclaredInstruction() {
        myFixture.addFileToProject("unrelated.td", "class Instruction { int unrelated = 1; }")
        myFixture.addFileToProject(
            "predicate.td", """
            class Instruction;
            class CheckOpcode<list<Instruction> opcodes>;
        """.trimIndent()
        )
        myFixture.addFileToProject(
            "target.td", """
            include "predicate.td"
            class Instruction { int size = 0; }
        """.trimIndent()
        )
    }

    fun `test argument deriving from a forward declared class is not flagged`() {
        // Both statements of 'Instruction' denote the same class, making a record deriving from the definition
        // assignable to a template argument typed with the declaration.
        addForwardDeclaredInstruction()
        doResolvingTest(
            """
            include "target.td"
            def BLR : Instruction;
            def D : CheckOpcode<[BLR]>;
        """.trimIndent()
        )
    }

    fun `test an unrelated definition of the same name does not make a declaration ambiguous`() {
        // A record that genuinely does not derive from 'Instruction'. The mismatch may only be reported because
        // 'target.td' is the sole candidate for the definition of the declaration; if 'unrelated.td' counted as one as
        // well it would be unknown which class the declaration denotes and the derivation could not be ruled out.
        addForwardDeclaredInstruction()
        doResolvingTest(
            """
            include "target.td"
            def NotAnInstruction;
            def D : CheckOpcode<<error descr="Value of type 'list<NotAnInstruction>' cannot be assigned to template argument 'opcodes' of type 'list<Instruction>'">[NotAnInstruction]</error>>;
        """.trimIndent()
        )
    }

    fun `test a declaration defined by several compilations is ambiguous`() {
        // 'predicate.td' now takes part in two compilations that each define 'Instruction' themselves, as LLVM's
        // per-target 'Target.td' files do. Which class the declaration denotes depends on the compilation, so the
        // mismatch the test above reports may no longer be.
        addForwardDeclaredInstruction()
        val otherTarget = myFixture.addFileToProject(
            "other_target.td", """
            include "predicate.td"
            class Instruction { int width = 0; }
        """.trimIndent()
        )
        val main = myFixture.configureByText(
            "test.td", """
            include "target.td"
            def NotAnInstruction;
            def D : CheckOpcode<[NotAnInstruction]>;
        """.trimIndent()
        )
        val dir = main.virtualFile.parent
        installCompileCommands(
            project, mapOf(
                main.virtualFile to IncludePaths(listOf(dir)),
                otherTarget.virtualFile to IncludePaths(listOf(dir)),
            )
        )
        myFixture.checkHighlighting()
    }

    fun `test base class resolving to a forward declaration is not flagged`() {
        // 'derived.td' only sees the forward declaration of 'Base', making 'Derived' derive from the declaration,
        // while 'Base' used as a type in this file resolves to the definition. Both denote the same class, so this
        // must not be flagged even though the two statements cannot be matched up from the base class side.
        myFixture.addFileToProject("decl.td", "class Base;")
        myFixture.addFileToProject(
            "derived.td", """
            include "decl.td"
            class Derived : Base;
        """.trimIndent()
        )
        myFixture.addFileToProject(
            "def.td", """
            include "decl.td"
            class Base { int x = 0; }
        """.trimIndent()
        )
        doResolvingTest(
            """
            include "derived.td"
            include "def.td"
            class Sink<Base b>;
            def D : Derived;
            def U : Sink<D>;
        """.trimIndent()
        )
    }

    fun `test list argument with a mismatching element type is flagged`() {
        doResolvingTest(
            """
            class C<list<int> a>;
            def D : C<<error descr="Value of type 'list<string>' cannot be assigned to template argument 'a' of type 'list<int>'">["a"]</error>>;
        """.trimIndent()
        )
    }

    fun `test empty list argument is not flagged`() {
        // An empty list has an unknown element type, so its convertibility is indeterminate.
        doResolvingTest(
            """
            class C<list<int> a>;
            def D : C<[]>;
        """.trimIndent()
        )
    }

    fun `test iteration variable of a foreach over a list has the element type`() {
        doResolvingTest(
            """
            class C<int x>;
            defvar v = !foreach(a, [5], C<a>);
        """.trimIndent()
        )
    }

    fun `test iteration variable of a foreach over a dag is a dag`() {
        doResolvingTest(
            """
            class C<int x>;
            def ins;
            defvar d = (ins 5);
            defvar v = !foreach(a, d, C<<error descr="Value of type 'dag' cannot be assigned to template argument 'x' of type 'int'">a</error>>);
        """.trimIndent()
        )
    }

    /**
     * Highlighting test that additionally installs compile commands so that class references resolve. [source] becomes
     * the sole compile-command root and may include any other file previously added to the fixture.
     */
    private fun doResolvingTest(source: String) {
        val file = myFixture.configureByText("test.td", source)
        installCompileCommands(
            project, mapOf(
                file.virtualFile to IncludePaths(listOf(file.virtualFile.parent))
            )
        )
        myFixture.checkHighlighting()
    }
}
