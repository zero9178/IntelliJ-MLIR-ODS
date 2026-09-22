package com.github.zero9178.mlirods

import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.testFramework.fixtures.BasePlatformTestCase


class EditorTest : BasePlatformTestCase() {

    fun `test brace matching`() {
        for (pair in arrayOf('{' to '}', '[' to ']', '(' to ')', '<' to '>')) {
            val (lhs, rhs) = pair
            myFixture.configureByText(
                "test.td", """
                <caret>
            """.trimIndent()
            )

            myFixture.type(lhs)
            myFixture.checkResult(
                """
                ${lhs}<caret>${rhs}
            """.trimIndent()
            )
        }
        myFixture.configureByText(
            "test.td", """
                [<caret>]
            """.trimIndent()
        )

        myFixture.type('{')
        myFixture.checkResult(
            """
                [{<caret>}]
            """.trimIndent()
        )

        myFixture.configureByText(
            "test.td", """
                [{   [<caret>]  }]
            """.trimIndent()
        )

        myFixture.type('{')
        myFixture.checkResult(
            """
                [{   [{<caret>]  }]
            """.trimIndent()
        )
    }

    fun `test angle bracket is paired`() = doTestTyping(
        "class Foo<caret>", '<', "class Foo<<caret>>"
    )

    fun `test angle bracket is not paired inside string`() = doTestTyping(
        """def x = "<caret>";""", '<', """def x = "<<caret>";"""
    )

    fun `test angle bracket is not paired inside comment`() = doTestTyping(
        "// <caret>", '<', "// <<caret>"
    )

    fun `test angle bracket is not paired if already closed`() = doTestTyping(
        "def x : Foo<caret>>;", '<', "def x : Foo<<caret>>;"
    )

    fun `test closing angle bracket is stepped over`() = doTestTyping(
        "def x : Foo<<caret>>;", '>', "def x : Foo<><caret>;"
    )

    fun `test closing angle bracket is not stepped over if unbalanced`() = doTestTyping(
        "list<list<int<caret>>;", '>', "list<list<int><caret>>;"
    )

    fun `test paired angle bracket can be tabbed out of`() {
        myFixture.configureByText("test.td", "class Foo<caret>")
        myFixture.type('<')
        myFixture.performEditorAction(IdeActions.ACTION_BRACE_OR_QUOTE_OUT)
        myFixture.checkResult("class Foo<><caret>")
    }

    fun `test deleting angle bracket deletes its pair`() = doTestBackspace(
        "class Foo<<caret>>", "class Foo<caret>"
    )

    fun `test deleting angle bracket keeps closing bracket of other pair`() = doTestBackspace(
        "class Foo<A<<caret>>", "class Foo<A<caret>>"
    )

    fun `test deleting angle bracket keeps closing bracket inside string`() = doTestBackspace(
        """def x = "<<caret>>";""", """def x = "<caret>>";"""
    )

    private fun doTestTyping(source: String, char: Char, expected: String) {
        myFixture.configureByText("test.td", source)
        myFixture.type(char)
        myFixture.checkResult(expected)
    }

    private fun doTestBackspace(source: String, expected: String) {
        myFixture.configureByText("test.td", source)
        myFixture.performEditorAction(IdeActions.ACTION_EDITOR_BACKSPACE)
        myFixture.checkResult(expected)
    }
}
