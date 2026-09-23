package com.github.zero9178.mlirods

import com.intellij.codeInsight.CodeInsightSettings
import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.testFramework.AutoPopupParameterInfoTestUtil
import com.intellij.testFramework.PlatformTestUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.testFramework.fixtures.EditorHintFixture


class EditorTest : BasePlatformTestCase() {

    private lateinit var hintFixture: EditorHintFixture
    private var parameterInfoDelay = 0

    override fun setUp() {
        super.setUp()
        hintFixture = EditorHintFixture(testRootDisposable)
        val settings = CodeInsightSettings.getInstance()
        parameterInfoDelay = settings.PARAMETER_INFO_DELAY
        // Speed up the tests waiting for the parameter info popup.
        settings.PARAMETER_INFO_DELAY = 100
    }

    override fun tearDown() {
        try {
            CodeInsightSettings.getInstance().PARAMETER_INFO_DELAY = parameterInfoDelay
        } catch (e: Throwable) {
            addSuppressedException(e)
        } finally {
            super.tearDown()
        }
    }

    fun `test brace matching`() {
        for (pair in arrayOf('{' to '}', '[' to ']', '(' to ')', '<' to '>')) {
            val (lhs, rhs) = pair
            myFixture.configureByText(
                "test.td", """
                <caret>
            """.trimIndent()
            )

            myFixture.type(lhs)
            myFixture.checkResult(
                """
                ${lhs}<caret>${rhs}
            """.trimIndent()
            )
        }
        myFixture.configureByText(
            "test.td", """
                [<caret>]
            """.trimIndent()
        )

        myFixture.type('{')
        myFixture.checkResult(
            """
                [{<caret>}]
            """.trimIndent()
        )

        myFixture.configureByText(
            "test.td", """
                [{   [<caret>]  }]
            """.trimIndent()
        )

        myFixture.type('{')
        myFixture.checkResult(
            """
                [{   [{<caret>]  }]
            """.trimIndent()
        )
    }

    fun `test angle bracket is paired`() = doTestTyping(
        "class Foo<caret>", '<', "class Foo<<caret>>"
    )

    fun `test angle bracket is not paired inside string`() = doTestTyping(
        """def x = "<caret>";""", '<', """def x = "<<caret>";"""
    )

    fun `test angle bracket is not paired inside comment`() = doTestTyping(
        "// <caret>", '<', "// <<caret>"
    )

    fun `test angle bracket is not paired if already closed`() = doTestTyping(
        "def x : Foo<caret>>;", '<', "def x : Foo<<caret>>;"
    )

    fun `test closing angle bracket is stepped over`() = doTestTyping(
        "def x : Foo<<caret>>;", '>', "def x : Foo<><caret>;"
    )

    fun `test closing angle bracket is not stepped over if unbalanced`() = doTestTyping(
        "list<list<int<caret>>;", '>', "list<list<int><caret>>;"
    )

    fun `test paired angle bracket can be tabbed out of`() {
        myFixture.configureByText("test.td", "class Foo<caret>")
        myFixture.type('<')
        myFixture.performEditorAction(IdeActions.ACTION_BRACE_OR_QUOTE_OUT)
        myFixture.checkResult("class Foo<><caret>")
    }

    fun `test angle bracket shows parameter info of class`() = doTestParameterInfo(
        """
        class Foo<int x, string y>;
        def : Foo<caret>
    """.trimIndent(), "<html><b>int x</b>, string y</html>"
    )

    fun `test angle bracket shows parameter info of multiclass in defm`() = doTestParameterInfo(
        """
        multiclass Foo<int x, string y> { def a; }
        defm : Foo<caret>
    """.trimIndent(), "<html><b>int x</b>, string y</html>"
    )

    fun `test angle bracket shows parameter info of class in defm`() = doTestParameterInfo(
        """
        multiclass M { def a; }
        class Foo<int x, string y>;
        defm : M, Foo<caret>
    """.trimIndent(), "<html><b>int x</b>, string y</html>"
    )

    fun `test angle bracket shows parameter info of multiclass in multiclass`() = doTestParameterInfo(
        """
        multiclass Foo<int x, string y> { def a; }
        multiclass Bar : Foo<caret>
    """.trimIndent(), "<html><b>int x</b>, string y</html>"
    )

    fun `test deleting angle bracket deletes its pair`() = doTestBackspace(
        "class Foo<<caret>>", "class Foo<caret>"
    )

    fun `test deleting angle bracket keeps closing bracket of other pair`() = doTestBackspace(
        "class Foo<A<<caret>>", "class Foo<A<caret>>"
    )

    fun `test deleting angle bracket keeps closing bracket inside string`() = doTestBackspace(
        """def x = "<<caret>>";""", """def x = "<caret>>";"""
    )

    private fun doTestTyping(source: String, char: Char, expected: String) {
        myFixture.configureByText("test.td", source)
        myFixture.type(char)
        myFixture.checkResult(expected)
    }

    private fun doTestBackspace(source: String, expected: String) {
        myFixture.configureByText("test.td", source)
        myFixture.performEditorAction(IdeActions.ACTION_EDITOR_BACKSPACE)
        myFixture.checkResult(expected)
    }

    /**
     * Types a '<' at the caret in [source], which must pair it with a '>', and checks that the parameter info popup
     * shows [expectedHint].
     */
    private fun doTestParameterInfo(source: String, expectedHint: String) {
        doTestTyping(source, '<', source.replace("<caret>", "<<caret>>"))
        waitForParameterInfo()
        assertEquals(expectedHint, currentHintText())
    }

    /**
     * Waits for a parameter info popup scheduled by typing to be shown.
     */
    private fun waitForParameterInfo() {
        // Showing the popup is a chain of non-blocking read actions handing over to the event queue and back, with the
        // delay before the popup in between.
        repeat(5) {
            PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue()
            AutoPopupParameterInfoTestUtil.waitForParameterInfoUpdate(myFixture.editor)
        }
        AutoPopupParameterInfoTestUtil.waitForAutoPopup(project)
        repeat(5) {
            PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue()
            AutoPopupParameterInfoTestUtil.waitForParameterInfoUpdate(myFixture.editor)
        }
    }

    /**
     * Returns the text of the currently shown hint without its styling.
     */
    private fun currentHintText() = hintFixture.currentHintText
        ?.replace(Regex("<style>[^<]*</style>\\s*"), "")
        ?.replace(Regex("</?span[^>]*>"), "")
}
