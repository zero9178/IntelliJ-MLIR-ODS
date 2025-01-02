package com.github.zero9178.mlirods

import com.github.zero9178.mlirods.language.TableGenLexerAdapter
import com.intellij.testFramework.LexerTestCase

class LexerTest : LexerTestCase() {
    override fun createLexer() = TableGenLexerAdapter()

    override fun getDirPath() = ""

    fun `test punctuation`() = doTest(
        """
            +
            -
            [
            ]
            {
            }
            <
            >
            :
            ;
            .
            ...
            =
            ?
            #
        """.trimMargin().filter { !it.isWhitespace() },
        "+ ('+')\n" + "- ('-')\n" + "[ ('[')\n" + "] (']')\n" + "{ ('{')\n" + "} ('}')\n" + "< ('<')\n" + "> ('>')\n" + ": (':')\n" + "; (';')\n" + "... ('...')\n" + ". ('.')\n" + "= ('=')\n" + "? ('?')\n" + "# ('#')"
    )

    fun `test keywords`() = doTest(
        "def", "def ('def')"
    )

    fun `test integers`() = doTest(
        """
        0
        +1
        -15
        0xaFfE
        0b0101
    """.trimIndent(),
        "INTEGER ('0')\n" +
                "WHITE_SPACE ('\\n')\n" +
                "INTEGER ('+1')\n" +
                "WHITE_SPACE ('\\n')\n" +
                "INTEGER ('-15')\n" +
                "WHITE_SPACE ('\\n')\n" +
                "INTEGER ('0xaFfE')\n" +
                "WHITE_SPACE ('\\n')\n" +
                "INTEGER ('0b0101')"
    )

    fun `test strings`() = doTest(
        """
       "quoted\"\n ends now"
    """.trimIndent(),
        "STRING_LITERAL ('\"quoted\\\"\\n ends now\"')"
    )

    fun `test strings negative`() = doTest(
        """
        "
        "
    """.trimIndent(), "OTHER ('\"')\n" +
                "WHITE_SPACE ('\\n')\n" +
                "OTHER ('\"')"
    )

    fun `test raw strings`() = doTest(
        """
       [{ some text
       that may have
       newlines inbetween
       that even have "quotes" up until }]
    """.trimIndent(),
        "STRING_LITERAL ('[{ some text\\n" +
                "that may have\\n" +
                "newlines inbetween\\n" +
                "that even have \"quotes\" up until }]')"
    )

    fun `test line comment`() = doTest(
        """
        // text
        0
    """.trimIndent(), "LINE_COMMENT ('// text')\n" +
                "WHITE_SPACE ('\\n')\n" +
                "INTEGER ('0')"
    )

    fun `test block comment`() = doTest(
        """
        /* text
        0
        */
        0
    """.trimIndent(), "BLOCK_COMMENT ('/* text\\n0\\n*/')\n" +
                "WHITE_SPACE ('\\n')\n" +
                "INTEGER ('0')"
    )

    fun `test block comment negative`() = doTest(
        """
            /*/
    """.trimIndent(), "OTHER ('/')\n" +
                "OTHER ('*')\n" +
                "OTHER ('/')"
    )
}