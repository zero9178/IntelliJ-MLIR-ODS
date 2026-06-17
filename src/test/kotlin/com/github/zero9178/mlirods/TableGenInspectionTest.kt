package com.github.zero9178.mlirods

import com.github.zero9178.mlirods.language.inspection.ConvertToLetQuickFix
import com.github.zero9178.mlirods.language.inspection.TableGenRedefinedFieldInspection
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class TableGenInspectionTest : BasePlatformTestCase() {

    override fun setUp() {
        super.setUp()
        myFixture.enableInspections(TableGenRedefinedFieldInspection::class.java)
    }

    fun `test no redefinition`() {
        myFixture.configureByText(
            "test.td", """
            class Base {
                int x = 0;
            }

            class Derived : Base {
                int y = 1;
            }
        """.trimIndent()
        )
        myFixture.checkHighlighting()
    }

    fun `test redefinition in same scope`() {
        myFixture.configureByText(
            "test.td", """
            class F {
                int x = 0;
                int <warning descr="Redefinition of existing field 'F:x'">x</warning> = 1;
            }
        """.trimIndent()
        )
        myFixture.checkHighlighting()
    }

    fun `test redefinition of inherited field`() {
        myFixture.configureByText(
            "test.td", """
            class Base {
                int x = 0;
            }

            class Derived : Base {
                int <warning descr="Redefinition of existing field 'Base:x'">x</warning> = 1;
            }
        """.trimIndent()
        )
        myFixture.checkHighlighting()
    }

    fun `test redefinition in anonymous record`() {
        myFixture.configureByText(
            "test.td", """
            def {
                int x = 0;
                int <warning descr="Redefinition of existing field '<anonymous>:x'">x</warning> = 1;
            }
        """.trimIndent()
        )
        myFixture.checkHighlighting()
    }

    fun `test convert to let quick fix`() {
        myFixture.configureByText(
            "test.td", """
            class Base {
                int x = 0;
            }

            class Derived : Base {
                int x<caret> = 1;
            }
        """.trimIndent()
        )
        val intention = myFixture.findSingleIntention(ConvertToLetQuickFix.familyName)
        myFixture.launchAction(intention)
        myFixture.checkResult(
            """
            class Base {
                int x = 0;
            }

            class Derived : Base {
                let x = 1;
            }
        """.trimIndent()
        )
    }
}
