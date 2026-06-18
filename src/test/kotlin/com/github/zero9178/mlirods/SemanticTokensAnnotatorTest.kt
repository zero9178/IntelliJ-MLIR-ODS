package com.github.zero9178.mlirods

import com.intellij.testFramework.IndexingTestUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class SemanticTokensAnnotatorTest : BasePlatformTestCase() {

    fun `test let identifier`() {
        IndexingTestUtil.waitUntilIndexesAreReady(project)
        
        myFixture.configureByText(
            "test.td", """
            let <text_attr textAttributesKey="TABLEGEN_KEYWORD">prepend</text_attr> <text_attr textAttributesKey="TABLEGEN_FIELD">s</text_attr> = 5 in {
                class Foo {
                    string <text_attr textAttributesKey="TABLEGEN_FIELD">s</text_attr> = ?;
                }
            }
            def Foo : Foo {
                let <text_attr textAttributesKey="TABLEGEN_KEYWORD">append</text_attr> <text_attr textAttributesKey="TABLEGEN_FIELD">s</text_attr> = "";
            }
            defvar test = Foo.<text_attr textAttributesKey="TABLEGEN_FIELD">s</text_attr>;
            #define <text_attr textAttributesKey="TABLEGEN_PREPROCESSOR_MACRO_NAME">FOO</text_attr>
            #ifdef <text_attr textAttributesKey="TABLEGEN_PREPROCESSOR_MACRO_NAME">FOO</text_attr>
            #ifndef <text_attr textAttributesKey="TABLEGEN_PREPROCESSOR_MACRO_NAME">FOO</text_attr>
            
            <text_attr textAttributesKey="TABLEGEN_SKIPPED_CODE"></text_attr>#endif
            #endif
            class Bar {
                int <text_attr textAttributesKey="TABLEGEN_FIELD">i</text_attr> = 0;
                defvar j = <text_attr textAttributesKey="TABLEGEN_FIELD">i</text_attr>;
            }
        """.trimIndent()
        )
        myFixture.checkHighlighting(false, true, false)
    }

    fun `test broken let`() {
        myFixture.configureByText(
            "test.td", """
            let
        """.trimIndent()
        )
        myFixture.checkHighlighting(false, true, false, true)
    }

    fun `test named argument in class instantiation`() {
        IndexingTestUtil.waitUntilIndexesAreReady(project)

        myFixture.configureByText(
            "test.td", """
            class C<int a, int b>;
            defvar v = C<1, <text_attr textAttributesKey="TABLEGEN_NAMED_ARGUMENT">b =</text_attr> 2>;
        """.trimIndent()
        )
        myFixture.checkHighlighting(false, true, false)
    }

    fun `test named argument in class ref`() {
        IndexingTestUtil.waitUntilIndexesAreReady(project)

        myFixture.configureByText(
            "test.td", """
            class C<int a>;
            def D : C<<text_attr textAttributesKey="TABLEGEN_NAMED_ARGUMENT">a =</text_attr> 1>;
        """.trimIndent()
        )
        myFixture.checkHighlighting(false, true, false)
    }
}