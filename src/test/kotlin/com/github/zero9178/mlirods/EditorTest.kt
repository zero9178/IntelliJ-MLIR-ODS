package com.github.zero9178.mlirods

import com.intellij.testFramework.fixtures.BasePlatformTestCase


class EditorTest : BasePlatformTestCase() {

    fun `test brace matching`() {
        for (pair in arrayOf('{' to '}', '[' to ']', '(' to ')')) {
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
    }
}