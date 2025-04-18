package com.github.zero9178.mlirods

import com.github.zero9178.mlirods.model.IncludePaths
import com.intellij.testFramework.DumbModeTestUtils
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

    fun `test dumb field lookup`() {
        myFixture.configureByText(
            "test.td", """
            defvar v = 0;
            
            class A : B {
                int i = <caret>;
            }
        """.trimIndent()
        )

        DumbModeTestUtils.computeInDumbModeSynchronously(project) {
            myFixture.completeBasic()
            assertSameElements(requireNotNull(myFixture.lookupElementStrings), "v")
        }
    }

    fun `test field access lookup`() {
        myFixture.configureByText(
            "test.td", """
            defvar v = 0;
            
            class B {
                int j = 0;
            }
            
            defvar l = B<>.<caret>
        """.trimIndent()
        )

        myFixture.completeBasic()
        assertSameElements(
            requireNotNull(myFixture.lookupElementStrings),
            "j"
        )
    }

    fun `test field cross file access lookup`() {
        val otherTD = myFixture.configureByText(
            "other.td", """   
            class B {
                int j = 0;
            }
            """.trimIndent()
        )

        val testTD = myFixture.configureByText(
            "test.td", """
            include "other.td"
                
            defvar l = B<>.<caret>
        """.trimIndent()
        )
        installCompileCommands(
            project, mapOf(
                testTD.virtualFile to IncludePaths(listOf(otherTD.virtualFile.parent))
            )
        )

        myFixture.completeBasic()
        assertSameElements(
            requireNotNull(myFixture.lookupElementStrings),
            "j"
        )
    }
}