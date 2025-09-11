package com.github.zero9178.mlirods

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.ui.components.breadcrumbs.Crumb
import com.intellij.util.Consumer
import javax.swing.Icon

class BreadcrumbTest : BasePlatformTestCase() {
    fun `test class`() = doTestData(
        """
        class A {
            <caret>
        }
    """.trimIndent(), listOf(IconAndText("A", MyIcons.TableGenClass))
    )

    fun `test let`() = doTestData(
        """
        def A {
            let f = [<caret>];
        }
    """.trimIndent(), listOf(IconAndText("A", MyIcons.TableGenDef), IconAndText("let f ="))
    )

    fun `test def`() = doTestData(
        """
        def A {
            <caret>
        }
    """.trimIndent(), listOf(IconAndText("A", MyIcons.TableGenDef))
    )

    fun `test field`() = doTestData(
        """
        def A {
            list<int> f = [<caret>];
        }
    """.trimIndent(), listOf(IconAndText("A", MyIcons.TableGenDef), IconAndText("f", MyIcons.TableGenDef))
    )

    data class IconAndText(val text: String, val icon: Icon? = null)

    private fun doTestData(source: String, data: List<IconAndText>) = doTest(source, data.map {
        Consumer<Crumb> { c ->
            assertEquals(it.text, c.text)
            assertEquals(it.icon, c.icon)
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