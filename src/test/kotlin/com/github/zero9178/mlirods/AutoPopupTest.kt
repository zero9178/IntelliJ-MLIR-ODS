package com.github.zero9178.mlirods

import com.intellij.testFramework.fixtures.CompletionAutoPopupTestCase

class AutoPopupTest : CompletionAutoPopupTestCase() {
    fun `test bang operator autopopup`() {
        myFixture.configureByText(
            "test.td", """
            defvar test = <caret>;
        """.trimIndent()
        )

        type("!")

        val lookup = requireNotNull(lookup) { "typing '!' did not pop up completion" }
        assertContainsElements(lookup.items.map { it.lookupString }, "!add", "!cond")
    }

    fun `test no autopopup while typing an integer`() = doIntegerTest("123", "a")

    // The continuation has to be a character that the literal being typed cannot contain.
    fun `test no autopopup while typing a hexadecimal integer`() = doIntegerTest("0xF", "z")

    fun `test no autopopup while typing a binary integer`() = doIntegerTest("0b1", "a")

    /**
     * Types [integer] one character at a time, none of which may pop up completion, and asserts that the [continuation]
     * turning it into an identifier does.
     */
    private fun doIntegerTest(integer: String, continuation: String) {
        val identifiers = listOf("${integer}${continuation}One", "${integer}${continuation}Two")
        myFixture.configureByText(
            "test.td", """
            def ${identifiers[0]};
            def ${identifiers[1]};
            defvar test = <caret>;
        """.trimIndent()
        )

        integer.forEachIndexed { index, char ->
            type(char.toString())
            assertNull("typing '${integer.substring(0, index + 1)}' popped up completion", lookup)
        }

        type(continuation)

        val lookup = requireNotNull(lookup) { "typing '$integer$continuation' did not pop up completion" }
        assertContainsElements(lookup.items.map { it.lookupString }, identifiers)
    }
}
