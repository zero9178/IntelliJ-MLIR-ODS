package com.github.zero9178.mlirods


import com.github.zero9178.mlirods.language.generated.psi.TableGenDefvarStatement
import com.github.zero9178.mlirods.language.psi.TableGenFile
import com.github.zero9178.mlirods.language.psi.impl.TableGenEvaluationContext
import com.github.zero9178.mlirods.language.values.*
import com.intellij.testFramework.assertInstanceOf
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class ValueComputationTest : BasePlatformTestCase() {

    fun `test integer`() = doTest(
        """
        defvar v = 10;
    """.trimIndent(), TableGenIntegerValue(10L)
    )

    fun `test integer hex`() = doTest(
        """
        defvar v = 0x10;
    """.trimIndent(), TableGenIntegerValue(0x10L)
    )

    fun `test integer binary`() = doTest(
        """
        defvar v = 0b10;
    """.trimIndent(), TableGenIntegerValue(0b10L)
    )

    fun `test integer huge hex`() = doTest(
        """
        defvar v = 0xFFFFFFFFFFFFFFFF;
    """.trimIndent(), TableGenIntegerValue(-1L)
    )

    fun `test integer huge decimal`() = doTest(
        """
        defvar v = 18446744073709551615;
    """.trimIndent(), TableGenIntegerValue(-1L)
    )

    fun `test integer huge negative decimal`() = doTest(
        """
        defvar v = -9223372036854775808;
    """.trimIndent(), TableGenIntegerValue(-9223372036854775807L - 1)
    )

    fun `test integer too large`() = doTest(
        """
        defvar v = 18446744073709551616;
    """.trimIndent(), TableGenUnknownValue
    )

    fun `test string literal`() = doTest(
        """
            defvar v = "a multi token"" string"" literal";
        """.trimIndent(),
        TableGenStringValue(
            "a multi token string literal"
        )
    )

    fun `test block string literal`() = doTest(
        """
            defvar v = [{ \n }];
        """.trimIndent(),
        TableGenStringValue(" \\n ")
    )

    fun `test string escapes`() = doTest(
        """
            defvar v = "\n\t\\n\"\'";
        """.trimIndent(),
        TableGenStringValue("\n\t\\n\"\'")
    )

    fun `test bool true`() = doTest(
        """
        defvar v = true;
    """.trimIndent(), TableGenIntegerValue(1)
    )

    fun `test bool false`() = doTest(
        """
        defvar v = false;
    """.trimIndent(), TableGenIntegerValue(0)
    )


    fun `test undef`() = doTest(
        """
        defvar v = ?;
    """.trimIndent(), TableGenUndefValue
    )

    fun `test identifier referencing defvar`() = doTest(
        """
        defvar a = 5;
        defvar v = a;
    """.trimIndent(), TableGenIntegerValue(5)
    )

    fun `test identifier referencing def yields record value`() = doTest(
        """
        def Foo;
        defvar v = Foo;
    """.trimIndent()
    ) {
        assertInstanceOf<TableGenRecordValue>(it)
    }

    fun `test record field evaluated with template argument`() = doTest(
        """
        class C<int x> {
            int y = x;
        }
        def D : C<5>;
        defvar v = D.y;
    """.trimIndent(), TableGenIntegerValue(5)
    )

    fun `test record field evaluated with named template argument`() = doTest(
        """
        class C<int x, int y> {
            int z = y;
        }
        def D : C<x = 1, y = 2>;
        defvar v = D.z;
    """.trimIndent(), TableGenIntegerValue(2)
    )

    fun `test record field referencing another field`() = doTest(
        """
        class C {
            int a = 5;
            int b = a;
        }
        def D : C;
        defvar v = D.b;
    """.trimIndent(), TableGenIntegerValue(5)
    )

    fun `test record field inherited from base class`() = doTest(
        """
        class Base {
            int a = 7;
        }
        class Derived : Base {
            int b = a;
        }
        def D : Derived;
        defvar v = D.b;
    """.trimIndent(), TableGenIntegerValue(7)
    )

    fun `test record field overridden by let in def`() = doTest(
        """
        class C {
            int y = 1;
        }
        def D : C {
            let y = 2;
        }
        defvar v = D.y;
    """.trimIndent(), TableGenIntegerValue(2)
    )

    fun `test record template arg extra indirection`() = doTest(
        """
        class C<int x> {
            int y = x;
        }
        class D<int x> : C<x>;
        def E : D<3>;
        defvar v = E.y;
    """.trimIndent(), TableGenIntegerValue(3)
    )

    fun `test field access on record`() = doTest(
        """
        class C {
            int a = 5;
        }
        def D : C;
        defvar v = D.a;
    """.trimIndent(), TableGenIntegerValue(5)
    )

    fun `test chained field access`() = doTest(
        """
        class Inner {
            int a = 9;
        }
        def I : Inner;
        class Outer {
            Inner inner = I;
        }
        def O : Outer;
        defvar v = O.inner.a;
    """.trimIndent(), TableGenIntegerValue(9)
    )

    fun doTest(source: String, expectedValue: TableGenValue) = doTest(source) {
        assertEquals(expectedValue, it)
    }

    fun doTest(source: String, expectedCondition: (TableGenValue) -> Unit) {
        val file = assertInstanceOf<TableGenFile>(myFixture.configureByText("test.td", source))
        val statement = assertInstanceOf<TableGenDefvarStatement>(file.lastChild)
        expectedCondition.invoke(requireNotNull(statement.valueNode?.evaluate(TableGenEvaluationContext())))
    }
}
