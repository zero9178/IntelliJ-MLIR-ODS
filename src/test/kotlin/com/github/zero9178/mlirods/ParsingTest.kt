package com.github.zero9178.mlirods

import com.github.zero9178.mlirods.language.TableGenParserDefinition
import com.intellij.testFramework.ParsingTestCase

class ParsingTest : ParsingTestCase("", "td", TableGenParserDefinition()) {
    fun testPreprocessor() = doTest(true, true)

    fun testStatements() = doTest(true, true)
    fun testExpressions() = doTest(true, true)
    fun testErrorRecovery() = doTest(true, false)

    override fun getTestDataPath(): String? {
        return "src/test/testData/parser"
    }

    override fun includeRanges(): Boolean {
        return true
    }
}