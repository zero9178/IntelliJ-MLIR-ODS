package com.github.zero9178.mlirods

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.testFramework.fixtures.BasePlatformTestCase


class FormatterTest : BasePlatformTestCase() {
    fun `test colon spacing`() = doTest(
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


    private fun doTest(before: String, after: String) {
        myFixture.configureByText("test.td", before)
        WriteCommandAction.writeCommandAction(project).run<RuntimeException> {
            CodeStyleManager.getInstance(project).reformatText(
                myFixture.file,
                listOf(myFixture.file.textRange)
            )
        }
        myFixture.checkResult(after)
    }
}