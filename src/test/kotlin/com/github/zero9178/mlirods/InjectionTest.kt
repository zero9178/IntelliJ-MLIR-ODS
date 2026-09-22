package com.github.zero9178.mlirods

import com.intellij.injected.editor.EditorWindow
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.intellij.testFramework.EditorTestUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.testFramework.fixtures.InjectionTestFixture

class InjectionTest : BasePlatformTestCase() {
    private fun fileText(description: String) = """
        class Op {
            string description;
        }
        
        def Foo : Op {
            let description = DESCRIPTION;
        }
    """.trimIndent().replace("DESCRIPTION", description)

    private fun configureDescription(description: String) {
        myFixture.configureByText("test.td", fileText(description))
    }

    private val injectionFixture get() = InjectionTestFixture(myFixture)

    private val injectedFile: PsiFile
        get() = assertOneElement(injectionFixture.getAllInjections()).second

    fun `test markdown injection in description`() {
        configureDescription(
            """[{
                    # Markdown title
                    
                    Followed by a body.
                    
                    ```text
                    Even with a code block.
                    ```
                }]"""
        )

        val injected = injectedFile
        assertEquals("Markdown", injected.language.id)
        assertEquals(
            """
                # Markdown title
                
                Followed by a body.
                
                ```text
                Even with a code block.
                ```
            """.trimIndent(),
            injected.text
        )
    }

    fun `test injection keeps relative indentation and blank lines`() {
        configureDescription("[{  \n        Title\n\n          indented\n    \t\n        end  \n    }]")

        assertEquals("Title\n\n  indented\n\nend  ", injectedFile.text)
    }

    fun `test injection of single line and empty block strings`() {
        configureDescription("[{ single line }]")
        assertEquals("single line ", injectedFile.text)

        configureDescription("[{}]")
        assertEquals("", injectedFile.text)

        configureDescription("[{\n    }]")
        assertEquals("", injectedFile.text)

        configureDescription("[{\n\n}]")
        assertEquals("", injectedFile.text)
    }

    private fun checkTyped(description: String, injectedText: String) {
        PsiDocumentManager.getInstance(project).commitAllDocuments()
        assertEquals(fileText(description), injectionFixture.topLevelFile.text)
        assertEquals(injectedText, injectedFile.text)
    }

    private val descriptionWithCaret = """[{
                # Markdown title
                
                Followed by a <caret>body.
            }]"""

    private val descriptionAfterTyping = """[{
                # Markdown title
                
                Followed by a new 
                second linebody.
            }]"""

    private val injectedTextAfterTyping = "# Markdown title\n\nFollowed by a new \nsecond linebody."

    fun `test typing in injected editor`() {
        configureDescription(descriptionWithCaret)
        // Placing the caret inside the injected fragment makes the fixture operate on the injected editor.
        assertTrue(myFixture.editor is EditorWindow)

        myFixture.type("new \nsecond line")
        checkTyped(descriptionAfterTyping, injectedTextAfterTyping)
    }

    fun `test typing in host editor`() {
        configureDescription(descriptionWithCaret)
        val hostEditor = (myFixture.editor as EditorWindow).delegate

        for (c in "new \nsecond line") EditorTestUtil.performTypingAction(hostEditor, c)
        checkTyped(descriptionAfterTyping, injectedTextAfterTyping)
    }

    fun `test typing on empty line indents it`() {
        configureDescription(
            """[{
                # Markdown title
            <caret>
                Followed by a body.
            }]"""
        )

        myFixture.type("x")
        checkTyped(
            """[{
                # Markdown title
                x
                Followed by a body.
            }]""", "# Markdown title\nx\nFollowed by a body."
        )
    }

    fun `test enter continues markdown list with indentation`() {
        configureDescription(
            """[{
                * first<caret>
            }]"""
        )

        myFixture.type("\nsecond")
        checkTyped(
            """[{
                * first
                * second
            }]""", "* first\n* second"
        )
    }
}
