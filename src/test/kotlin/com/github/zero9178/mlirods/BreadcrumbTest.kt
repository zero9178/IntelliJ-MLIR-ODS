package com.github.zero9178.mlirods

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.ui.components.breadcrumbs.Crumb
import com.intellij.util.Consumer

class BreadcrumbTest : BasePlatformTestCase() {
    fun `test class`() = doTest(
        """
        class A {
            <caret>
        }
    """.trimIndent(), arrayOf("A")
    )

    fun `test let`() = doTest(
        """
        def A {
            let f = [<caret>];
        }
    """.trimIndent(), arrayOf("A", "let f")
    )

    fun `test def`() = doTest(
        """
        def A {
            <caret>
        }
    """.trimIndent(), arrayOf("A")
    )

    fun `test field`() = doTest(
        """
        def A {
            list<int> f = [<caret>];
        }
    """.trimIndent(), arrayOf("A", "f")
    )

    private fun doTest(source: String, data: Array<String>) = doTest(source, data.map {
        Consumer<Crumb> { c ->
            assertEquals(it, c.text)
        }
    })

    private fun doTest(source: String, conditions: List<Consumer<Crumb>>) {
        myFixture.configureByText("test.td", source)
        val crumbs = myFixture.breadcrumbsAtCaret
        assertSize(conditions.size, crumbs)
        crumbs.zip(conditions).forEach { (c, p) ->
            p.consume(c)
        }
    }
}