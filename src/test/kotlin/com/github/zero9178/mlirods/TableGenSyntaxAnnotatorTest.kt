package com.github.zero9178.mlirods

import com.github.zero9178.mlirods.model.IncludePaths
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class TableGenSyntaxAnnotatorTest : BasePlatformTestCase() {

    fun `test valid let mode`() {
        myFixture.configureByText(
            "test.td", """
            class F {
                list<int> i = [];
                let append i = [10];
                let prepend i = [10];
            }
        """.trimIndent()
        )
        myFixture.checkHighlighting()
    }

    fun `test invalid let mode`() {
        myFixture.configureByText(
            "test.td", """
            class F {
                list<int> i = [];
                let <error descr="Expected one of 'append' or 'prepend' instead of 'apend'">apend</error> i = [10];
            }
        """.trimIndent()
        )
        myFixture.checkHighlighting()
    }

    fun `test valid switch`() {
        myFixture.configureByText(
            "test.td", """
            defvar v = !switch(0, 1: "one", 2: "two", "default");
        """.trimIndent()
        )
        myFixture.checkHighlighting()
    }

    fun `test valid switch with single case`() {
        myFixture.configureByText(
            "test.td", """
            defvar v = !switch(0, 1: "one", "default");
        """.trimIndent()
        )
        myFixture.checkHighlighting()
    }

    fun `test switch missing default`() {
        myFixture.configureByText(
            "test.td", """
            defvar v = !switch(0, 1: "one", <error descr="'!switch' requires a default value as its last argument">2: "two"</error>);
        """.trimIndent()
        )
        myFixture.checkHighlighting()
    }

    fun `test switch misplaced default`() {
        myFixture.configureByText(
            "test.td", """
            defvar v = !switch(0, <error descr="Only the last '!switch' argument may be the default; expected ':' followed by a value">"oops"</error>, 1: "one", "default");
        """.trimIndent()
        )
        myFixture.checkHighlighting()
    }

    fun `test switch multiple misplaced defaults`() {
        myFixture.configureByText(
            "test.td", """
            defvar v = !switch(0, <error descr="Only the last '!switch' argument may be the default; expected ':' followed by a value">1</error>, <error descr="Only the last '!switch' argument may be the default; expected ':' followed by a value">2</error>, 3: "three", "default");
        """.trimIndent()
        )
        myFixture.checkHighlighting()
    }

    fun `test switch missing case`() {
        myFixture.configureByText(
            "test.td", """
            defvar v = <error descr="'!switch' requires at least one 'case : value' pair">!switch(0, "default")</error>;
        """.trimIndent()
        )
        myFixture.checkHighlighting()
    }

    fun `test positional only arguments`() {
        myFixture.configureByText(
            "test.td", """
            class C<int a, int b>;
            defvar v = C<1, 2>;
        """.trimIndent()
        )
        myFixture.checkHighlighting()
    }

    fun `test named only arguments`() {
        myFixture.configureByText(
            "test.td", """
            class C<int a, int b>;
            defvar v = C<a = 1, b = 2>;
        """.trimIndent()
        )
        myFixture.checkHighlighting()
    }

    fun `test positional before named arguments`() {
        myFixture.configureByText(
            "test.td", """
            class C<int a, int b>;
            defvar v = C<1, b = 2>;
        """.trimIndent()
        )
        myFixture.checkHighlighting()
    }

    fun `test positional after named argument`() {
        myFixture.configureByText(
            "test.td", """
            class C<int a, int b>;
            defvar v = C<a = 1, <error descr="Positional argument is not allowed after a named argument">2</error>>;
        """.trimIndent()
        )
        myFixture.checkHighlighting()
    }

    fun `test class ref positional before named arguments`() {
        myFixture.configureByText(
            "test.td", """
            class C<int a, int b>;
            def D : C<1, b = 2>;
        """.trimIndent()
        )
        myFixture.checkHighlighting()
    }

    fun `test class ref positional after named argument`() {
        myFixture.configureByText(
            "test.td", """
            class C<int a, int b>;
            def D : C<a = 1, <error descr="Positional argument is not allowed after a named argument">2</error>>;
        """.trimIndent()
        )
        myFixture.checkHighlighting()
    }

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
