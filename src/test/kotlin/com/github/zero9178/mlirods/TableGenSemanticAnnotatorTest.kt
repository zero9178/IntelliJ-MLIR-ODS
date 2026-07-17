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
     * Highlighting test that additionally installs compile commands so that class references resolve.
     */
    private fun doResolvingTest(source: String) {
        val file = myFixture.configureByText("test.td", source)
        installCompileCommands(
            project, mapOf(
                file.virtualFile to IncludePaths(emptyList())
            )
        )
        myFixture.checkHighlighting()
    }
}
