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
            #define <text_attr textAttributesKey="TABLEGEN_PREPROCESSOR_MACRO_NAME">FOO</text_attr>
            #ifdef <text_attr textAttributesKey="TABLEGEN_PREPROCESSOR_MACRO_NAME">FOO</text_attr>
            #ifndef <text_attr textAttributesKey="TABLEGEN_PREPROCESSOR_MACRO_NAME">FOO</text_attr>
            
            #endif
            #endif
        """.trimIndent()
        )
        myFixture.checkHighlighting(false, true, false)
    }
}