package com.github.zero9178.mlirods

import com.intellij.openapi.vfs.readText
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.testFramework.fixtures.InjectionTestFixture

class InjectionTest : BasePlatformTestCase() {
    fun `test markdown injection in description`() {
        myFixture.configureByText(
            "test.td", """
            class Op {
                string description;
            }
            
            def Foo : Op {
                let description = [{
                    # Markdown title
                    
                    Followed by a body.
                    
                    ```text
                    Even with a code block.
                    ```
                }];
            }
        """.trimIndent()
        )

        val fixture = InjectionTestFixture(myFixture)
        assertEquals(
            """
                    # Markdown title
                    
                    Followed by a body.
                    
                    ```text
                    Even with a code block.
                    ```
            """.trimIndent(),
            assertOneElement(fixture.getAllInjections()).second.virtualFile?.readText()
        )
    }
}