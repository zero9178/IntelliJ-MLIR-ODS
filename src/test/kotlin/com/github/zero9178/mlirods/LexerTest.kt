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
            ,
            (
            )
        """.trimMargin().filter { !it.isWhitespace() },
        "+ ('+')\n" +
                "- ('-')\n" +
                "[ ('[')\n" +
                "] (']')\n" +
                "{ ('{')\n" +
                "} ('}')\n" +
                "< ('<')\n" +
                "> ('>')\n" +
                ": (':')\n" +
                "; (';')\n" +
                "... ('...')\n" +
                ". ('.')\n" +
                "= ('=')\n" +
                "? ('?')\n" +
                "# ('#')\n" +
                ", (',')\n" +
                "( ('(')\n" +
                ") (')')"
    )

    fun `test keywords`() = doTest(
        """
            assert
            bit
            bits
            class
            code
            dag
            def
            dump
            else
            false
            foreach
            defm
            defset
            defvar
            field
            if
            in
            include
            int
            let
            list
            multiclass
            string
            then
            true
        """.trimIndent(), "assert ('assert')\n" +
                "WHITE_SPACE ('\\n')\n" +
                "bit ('bit')\n" +
                "WHITE_SPACE ('\\n')\n" +
                "bits ('bits')\n" +
                "WHITE_SPACE ('\\n')\n" +
                "class ('class')\n" +
                "WHITE_SPACE ('\\n')\n" +
                "code ('code')\n" +
                "WHITE_SPACE ('\\n')\n" +
                "dag ('dag')\n" +
                "WHITE_SPACE ('\\n')\n" +
                "def ('def')\n" +
                "WHITE_SPACE ('\\n')\n" +
                "dump ('dump')\n" +
                "WHITE_SPACE ('\\n')\n" +
                "else ('else')\n" +
                "WHITE_SPACE ('\\n')\n" +
                "false ('false')\n" +
                "WHITE_SPACE ('\\n')\n" +
                "foreach ('foreach')\n" +
                "WHITE_SPACE ('\\n')\n" +
                "defm ('defm')\n" +
                "WHITE_SPACE ('\\n')\n" +
                "defset ('defset')\n" +
                "WHITE_SPACE ('\\n')\n" +
                "defvar ('defvar')\n" +
                "WHITE_SPACE ('\\n')\n" +
                "field ('field')\n" +
                "WHITE_SPACE ('\\n')\n" +
                "if ('if')\n" +
                "WHITE_SPACE ('\\n')\n" +
                "in ('in')\n" +
                "WHITE_SPACE ('\\n')\n" +
                "include ('include')\n" +
                "WHITE_SPACE ('\\n')\n" +
                "int ('int')\n" +
                "WHITE_SPACE ('\\n')\n" +
                "let ('let')\n" +
                "WHITE_SPACE ('\\n')\n" +
                "list ('list')\n" +
                "WHITE_SPACE ('\\n')\n" +
                "multiclass ('multiclass')\n" +
                "WHITE_SPACE ('\\n')\n" +
                "string ('string')\n" +
                "WHITE_SPACE ('\\n')\n" +
                "then ('then')\n" +
                "WHITE_SPACE ('\\n')\n" +
                "true ('true')"
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
        "LINE_STRING_LITERAL ('\"quoted\\\"\\n ends now\"')"
    )

    fun `test strings negative`() = doTest(
        """
        "text
        "text2
    """.trimIndent(), "LINE_STRING_LITERAL_BAD ('\"text')\n" +
                "WHITE_SPACE ('\\n')\n" +
                "LINE_STRING_LITERAL_BAD ('\"text2')"
    )

    fun `test raw strings`() = doTest(
        """
       [{ some text
       that may have
       newlines inbetween
       that even have "quotes" up until }]
    """.trimIndent(),
        "BLOCK_STRING_LITERAL ('[{ some text\\n" +
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

    fun `test identifiers`() = doTest(
        """
            a
            _a
            0_a
            aa
        """.trimIndent(),
        "IDENTIFIER ('a')\n" +
                "WHITE_SPACE ('\\n')\n" +
                "IDENTIFIER ('_a')\n" +
                "WHITE_SPACE ('\\n')\n" +
                "IDENTIFIER ('0_a')\n" +
                "WHITE_SPACE ('\\n')\n" +
                "IDENTIFIER ('aa')"
    )

    fun `test preprocessor`() = doTest(
        """
            
               #ifdef TEST
            a
               #endif
            b #endif
        """.trimIndent(),
        "WHITE_SPACE ('\\n')\n" +
                "#ifdef ('   #ifdef')\n" +
                "WHITE_SPACE (' ')\n" +
                "IDENTIFIER ('TEST')\n" +
                "WHITE_SPACE ('\\n')\n" +
                "IDENTIFIER ('a')\n" +
                "WHITE_SPACE ('\\n')\n" +
                "#endif ('   #endif')\n" +
                "WHITE_SPACE ('\\n')\n" +
                "IDENTIFIER ('b')\n" +
                "WHITE_SPACE (' ')\n" +
                "# ('#')\n" +
                "IDENTIFIER ('endif')"
    )
}