package com.github.zero9178.mlirods

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.testFramework.fixtures.BasePlatformTestCase


class FormatterTest : BasePlatformTestCase() {
    fun `test colon spacing`() = doFormat(
        """
        class A;
        class B:A;
        defvar x = (A "test" : ${'$'}var)
    """.trimIndent(),
        """
        class A;
        class B : A;
        defvar x = (A "test":${'$'}var)
        """.trimIndent()
    )

    fun `test parent class list`() = doFormat(
        """
            class A;
            class B
            : A;
        """.trimIndent(),
        """
            class A;
            class B
              : A;
        """.trimIndent()
    )

    fun `test template arg decls`() = doFormat(
        """
            class A
            <int i>;
            
            class A<
            int i>;
            
            class A
            <int i,
            float f
            >;
        """.trimIndent(),
        """
            class A
                <int i>;
            
            class A<
                int i>;
            
            class A
                <int i,
                 float f
            >;
        """.trimIndent()
    )

    fun `test record body wrapping`() = doEnter(
        """
        class A {<caret>}
    """.trimIndent(),
        """
        class A {
          <caret>
        }
        """.trimIndent()
    )

    fun `test sticky block comment`() = doFormat(
        """
            class A<int i>;
            defvar v = A<
            /*i=*/ 0>;
        """.trimIndent(),
        """

        """.trimIndent()
    )

    private fun doFormat(before: String, after: String) {
        myFixture.configureByText("test.td", before)
        WriteCommandAction.writeCommandAction(project).run<RuntimeException> {
            CodeStyleManager.getInstance(project).reformatText(
                myFixture.file,
                listOf(myFixture.file.textRange)
            )
        }
        myFixture.checkResult(after)
    }

    private fun doEnter(before: String, after: String) {
        myFixture.configureByText("test.td", before)
        myFixture.type('\n')
        myFixture.checkResult(after)
    }
}