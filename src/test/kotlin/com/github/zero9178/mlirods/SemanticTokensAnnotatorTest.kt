package com.github.zero9178.mlirods

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class SemanticTokensAnnotatorTest : BasePlatformTestCase() {

    fun `test let identifier`() {
        myFixture.configureByText(
            "test.td", """
            class Foo {
                string s = ?;
            }
            def Foo : Foo {
                let <text_attr textAttributesKey="TABLEGEN_FIELD">s</text_attr> = "";
            }
        """.trimIndent()
        )
        myFixture.checkHighlighting(false, true, false)
    }
}