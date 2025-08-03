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


    fun doTest(source: String, expectedValue: TableGenValue) = doTest(source) {
        assertEquals(expectedValue, it)
    }

    fun doTest(source: String, expectedCondition: (TableGenValue) -> Unit) {
        val file = assertInstanceOf<TableGenFile>(myFixture.configureByText("test.td", source))
        val statement = assertInstanceOf<TableGenDefvarStatement>(file.lastChild)
        expectedCondition.invoke(requireNotNull(statement.valueNode?.evaluate(TableGenEvaluationContext())))
    }
}
