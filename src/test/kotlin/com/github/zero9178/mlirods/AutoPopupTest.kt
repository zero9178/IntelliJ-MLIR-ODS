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
}
