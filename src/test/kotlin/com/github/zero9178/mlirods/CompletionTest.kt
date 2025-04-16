package com.github.zero9178.mlirods

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class CompletionTest : BasePlatformTestCase() {
    fun `test foldl completion`() {
        myFixture.configureByText(
            "test.td", """
            defvar values = [1];
            defvar test = !foldl(0, <caret>, acc, i, i);
        """.trimIndent()
        )
        myFixture.completeBasic()
        val list = requireNotNull(myFixture.lookupElementStrings)
        assertSameElements(list, "values")
    }

    fun `test foreach completion`() {
        myFixture.configureByText(
            "test.td", """
            defvar values = [1];
            defvar test = !foreach(i, <caret>, i);
        """.trimIndent()
        )
        myFixture.completeBasic()
        val list = requireNotNull(myFixture.lookupElementStrings)
        assertSameElements(list, "values")
    }
}