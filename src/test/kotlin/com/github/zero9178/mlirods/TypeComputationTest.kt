package com.github.zero9178.mlirods


import com.github.zero9178.mlirods.language.generated.psi.TableGenDefvarStatement
import com.github.zero9178.mlirods.language.types.TableGenIntType
import com.github.zero9178.mlirods.language.types.TableGenListType
import com.github.zero9178.mlirods.language.types.TableGenStringType
import com.github.zero9178.mlirods.language.types.TableGenType
import com.intellij.psi.util.parentOfType
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class TypeComputationTest : BasePlatformTestCase() {
    fun `test list single slice`() {
        doTest(
            """
            defvar x = [5];
            defvar <caret>v = x[0];
        """.trimIndent(), TableGenIntType
        )
    }

    fun `test list multi slice`() {
        doTest(
            """
            defvar x = [5];
            defvar <caret>v = x[0, 1];
        """.trimIndent(), TableGenListType(TableGenIntType)
        )
    }

    fun `test field access`() {
        doTest(
            """
            class Foo {
                string i = "";
            }
            def Bar {
                Foo foo = Foo<>;
            }
            defvar <caret>v = Bar.foo.i;
        """.trimIndent(), TableGenStringType
        )
    }

    fun `test concat`() {
        doTest(
            """
            defvar <caret>v = [5] # [3];
        """.trimIndent(), TableGenListType(TableGenIntType)
        )
    }

    fun `test foreach`() {
        doTest(
            """
            defvar <caret>v = !foreach(a, [5], a);
        """.trimIndent(), TableGenListType(TableGenIntType)
        )
    }

    fun doTest(source: String, expectedType: TableGenType) {
        myFixture.configureByText("test.td", source)
        val statement = requireNotNull(myFixture.elementAtCaret.parentOfType<TableGenDefvarStatement>(withSelf = true))
        assertEquals(expectedType, statement.value?.type)
    }
}
