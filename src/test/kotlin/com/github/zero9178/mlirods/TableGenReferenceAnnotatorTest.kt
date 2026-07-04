package com.github.zero9178.mlirods

import com.github.zero9178.mlirods.model.IncludePaths
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class TableGenReferenceAnnotatorTest : BasePlatformTestCase() {

    fun `test resolving include is not flagged`() {
        myFixture.addFileToProject("sub/target.td", "")
        val main = myFixture.configureByText(
            "test.td", """
            include "sub/target.td"
        """.trimIndent()
        )
        installCompileCommands(
            project, mapOf(main.virtualFile to IncludePaths(listOf(main.virtualFile.parent)))
        )
        myFixture.checkHighlighting()
    }

    fun `test include with a missing directory is flagged`() {
        val main = myFixture.configureByText(
            "test.td", """
            include <error descr="Cannot resolve file 'missing/target.td'">"missing/target.td"</error>
        """.trimIndent()
        )
        installCompileCommands(
            project, mapOf(main.virtualFile to IncludePaths(listOf(main.virtualFile.parent)))
        )
        myFixture.checkHighlighting()
    }

    fun `test include with a missing file is flagged`() {
        myFixture.addFileToProject("sub/keep.td", "")
        val main = myFixture.configureByText(
            "test.td", """
            include <error descr="Cannot resolve file 'sub/missing.td'">"sub/missing.td"</error>
        """.trimIndent()
        )
        installCompileCommands(
            project, mapOf(main.virtualFile to IncludePaths(listOf(main.virtualFile.parent)))
        )
        myFixture.checkHighlighting()
    }

    fun `test unresolved include without a context is suppressed`() {
        // A file without an active context cannot resolve any include, so the annotator does not run there instead of
        // drowning the file in false positives.
        myFixture.configureByText(
            "test.td", """
            include "missing/target.td"
        """.trimIndent()
        )
        myFixture.checkHighlighting()
    }

    fun `test unresolved class is flagged`() {
        val main = myFixture.configureByText(
            "test.td", """
            def D : <error descr="Cannot resolve class 'Missing'">Missing</error>;
        """.trimIndent()
        )
        installCompileCommands(
            project, mapOf(main.virtualFile to IncludePaths(emptyList()))
        )
        myFixture.checkHighlighting()
    }

    fun `test resolved class is not flagged`() {
        val main = myFixture.configureByText(
            "test.td", """
            class Base;
            def D : Base;
        """.trimIndent()
        )
        installCompileCommands(
            project, mapOf(main.virtualFile to IncludePaths(emptyList()))
        )
        myFixture.checkHighlighting()
    }

    fun `test unresolved class without a context is suppressed`() {
        // Without a context, references cannot be resolved, so the annotator does not run there.
        myFixture.configureByText(
            "test.td", """
            def D : Missing;
        """.trimIndent()
        )
        myFixture.checkHighlighting()
    }
}
