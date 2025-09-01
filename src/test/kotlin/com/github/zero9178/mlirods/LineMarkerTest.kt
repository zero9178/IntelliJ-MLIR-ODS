package com.github.zero9178.mlirods

import com.intellij.codeInsight.daemon.impl.DaemonCodeAnalyzerImpl
import com.intellij.testFramework.ExpectedHighlightingData
import com.intellij.testFramework.IndexingTestUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class LineMarkerTest : BasePlatformTestCase() {
    fun `test let override`() = doTest(
        """
            class <lineMarker descr="Navigate to derived records of 'A'">A</lineMarker> {
                int i = 5;
                let i = 7;
            }
            def B : A {
                let <lineMarker descr="Navigate to previous value of 'i'">i</lineMarker> = 8;
            }
        """.trimIndent()
    )

    fun `test derived class`() = doTest(
        """
            class <lineMarker descr="Navigate to derived records of 'A'">A</lineMarker> {}
            class B : A;
        """.trimIndent()
    )

    private fun doTest(source: String) = run {
        val file = myFixture.configureByText("test.td", source)
        IndexingTestUtil.waitUntilIndexesAreReady(project)

        val document = myFixture.editor.document
        val data = ExpectedHighlightingData(document, false, false)
        data.init()

        myFixture.doHighlighting()
        val markerInfos = DaemonCodeAnalyzerImpl.getLineMarkers(document, project)
        data.checkLineMarkers(file, markerInfos, document.text)
    }
}