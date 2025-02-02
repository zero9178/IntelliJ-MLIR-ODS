package com.github.zero9178.mlirods

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class SemanticTokensAnnotatorTest : BasePlatformTestCase() {

    fun `test let identifier`() {
        myFixture.configureByText(
            "test.td", """
            let <text_attr textAttributesKey="TABLEGEN_FIELD">s</text_attr> = 5 in {
                class Foo {
                    string <text_attr textAttributesKey="TABLEGEN_FIELD">s</text_attr> = ?;
                }
            }
            def Foo : Foo {
                let <text_attr textAttributesKey="TABLEGEN_FIELD">s</text_attr> = "";
            }
            defvar test = Foo.<text_attr textAttributesKey="TABLEGEN_FIELD">s</text_attr>;
        """.trimIndent()
        )
        myFixture.checkHighlighting(false, true, false)
    }
}