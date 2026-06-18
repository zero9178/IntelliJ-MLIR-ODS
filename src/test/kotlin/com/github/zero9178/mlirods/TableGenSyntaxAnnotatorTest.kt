package com.github.zero9178.mlirods

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
}
