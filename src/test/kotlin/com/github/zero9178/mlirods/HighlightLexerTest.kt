package com.github.zero9178.mlirods

import com.github.zero9178.mlirods.highlighting.TableGenHighlighterLexer
import com.intellij.testFramework.LexerTestCase

class HighlightLexerTest : LexerTestCase() {
    override fun createLexer() = TableGenHighlighterLexer()

    override fun getDirPath() = ""

    fun `test strings`() = doTest(
        """
       "quoted\"\n ends now"
    """.trimIndent(),
        "LINE_STRING_LITERAL ('\"quoted')\n" +
                "VALID_STRING_ESCAPE_TOKEN ('\\\"')\n" +
                "VALID_STRING_ESCAPE_TOKEN ('\\n')\n" +
                "LINE_STRING_LITERAL (' ends now\"')"
    )
}
