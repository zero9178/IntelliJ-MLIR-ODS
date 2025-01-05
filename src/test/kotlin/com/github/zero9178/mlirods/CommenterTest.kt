package com.github.zero9178.mlirods


import com.intellij.codeInsight.generation.actions.CommentByBlockCommentAction
import com.intellij.codeInsight.generation.actions.CommentByLineCommentAction
import com.intellij.testFramework.LightPlatformCodeInsightTestCase

class CommenterTest : LightPlatformCodeInsightTestCase() {
    fun `test line commenter`() {
        configureFromFileText(
            "test.td", """
            <caret> def Foo;
        """.trimIndent()
        )
        val action = CommentByLineCommentAction()
        action.actionPerformedImpl(project, editor)
        checkResultByText("// def Foo;")
    }

    fun `test block commenter`() {
        configureFromFileText(
            "test.td", """
            <selection>def Foo;</selection>
        """.trimIndent()
        )
        val action = CommentByBlockCommentAction()
        action.actionPerformedImpl(project, editor)
        checkResultByText(
            "/*\n" +
                    "def Foo;*/\n"
        )
    }
}
