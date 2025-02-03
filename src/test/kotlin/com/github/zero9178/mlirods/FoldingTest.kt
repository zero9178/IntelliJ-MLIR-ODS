package com.github.zero9178.mlirods

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class FoldingTest : BasePlatformTestCase() {
    fun `test folding`() {
        myFixture.testFolding("$testDataPath/folding.td")
    }

    override fun getTestDataPath(): String? {
        return "src/test/testData/folding"
    }
}