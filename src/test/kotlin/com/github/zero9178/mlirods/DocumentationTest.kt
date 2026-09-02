package com.github.zero9178.mlirods

import com.github.zero9178.mlirods.language.generated.psi.TableGenMulticlassStatement
import com.intellij.lang.documentation.DocumentationMarkup
import com.intellij.platform.backend.documentation.DocumentationData
import com.intellij.platform.backend.documentation.DocumentationTarget
import com.intellij.platform.backend.documentation.PsiDocumentationTargetProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.jsoup.Jsoup
import org.jsoup.nodes.Element

class DocumentationTest : BasePlatformTestCase() {

    /**
     * Returns the documentation target the registered providers yield for [element].
     */
    private fun documentationTarget(element: PsiElement): DocumentationTarget? =
        PsiDocumentationTargetProvider.EP_NAME.extensionList.firstNotNullOfOrNull {
            it.documentationTarget(element, null)
        }

    /**
     * Returns the documentation HTML shown for [element].
     */
    private fun documentation(element: PsiElement): String? =
        (documentationTarget(element)?.computeDocumentation() as? DocumentationData)?.html

    /**
     * Returns the documentation HTML shown for the element the caret resolves to.
     */
    private fun docAtCaret(code: String): String? {
        myFixture.configureByText("test.td", code)
        return documentation(myFixture.elementAtCaret)
    }

    /**
     * Returns the element of the documentation section with the given [cssClass].
     */
    private fun section(html: String?, cssClass: String): Element {
        assertNotNull(html)
        val sections = Jsoup.parse(html!!).select("div.$cssClass")
        assertSize(1, sections)
        return sections.single()
    }

    /**
     * Returns the text of the documentation section with the given [cssClass] with all markup stripped and any
     * whitespace collapsed.
     */
    private fun sectionText(html: String?, cssClass: String): String =
        section(html, cssClass).text().replace('\u00a0', ' ').replace(Regex("\\s+"), " ").trim()

    private fun contentText(html: String?): String = sectionText(html, DocumentationMarkup.CLASS_CONTENT)

    private fun assertContent(html: String?, vararg contents: String) {
        val text = contentText(html)
        contents.forEach {
            assertTrue("expected \"$it\" within:\n$html", text.contains(it))
        }
    }

    private fun definitionText(html: String?): String = sectionText(html, DocumentationMarkup.CLASS_DEFINITION)

    private fun assertDefinition(html: String?, definition: String) {
        val text = definitionText(html)
        assertTrue("expected definition \"$definition\" within:\n$html", text.contains(definition))
    }

    private fun assertNoContent(html: String?) {
        assertNotNull(html)
        val content = Jsoup.parse(html!!).select("div.${DocumentationMarkup.CLASS_CONTENT}")
        assertTrue("expected no content within:\n$html", content.isEmpty())
    }

    fun `test class documentation`() {
        val html = docAtCaret(
            """
                // Doc line one.
                // Doc line two.
                class Base;

                class Derived : Ba<caret>se;
            """.trimIndent()
        )
        assertDefinition(html, "class Base")
        assertContent(html, "Doc line one. Doc line two.")
    }

    fun `test def documentation`() {
        val html = docAtCaret(
            """
                // A def.
                def bar {
                    int x = 0;
                }
                defvar v = ba<caret>r;
            """.trimIndent()
        )
        assertDefinition(html, "def bar")
        assertContent(html, "A def.")
    }

    fun `test defvar documentation`() {
        val html = docAtCaret(
            """
                // The answer.
                defvar value = 42;
                defvar other = val<caret>ue;
            """.trimIndent()
        )
        assertDefinition(html, "defvar value = 42")
        assertContent(html, "The answer.")
    }

    fun `test field documentation`() {
        val html = docAtCaret(
            """
                class B {
                    // Field doc.
                    int j = 0;
                }
                defvar l = B<>.<caret>j
            """.trimIndent()
        )
        assertDefinition(html, "int j = 0")
        assertDefinition(html, "in class B")
        assertContent(html, "Field doc.")
    }

    fun `test field with composite initializer is elided`() {
        val html = docAtCaret(
            """
                class B {
                    // Field doc.
                    list<int> l = [1, 2];
                }
                defvar v = B<>.<caret>l
            """.trimIndent()
        )
        assertDefinition(html, "list<int> l = ...")
        assertDefinition(html, "in class B")
    }

    fun `test field in def`() {
        val html = docAtCaret(
            """
                def bar {
                    // Field doc.
                    string s = "value";
                }
                defvar v = bar.<caret>s
            """.trimIndent()
        )
        assertDefinition(html, "string s = \"value\"")
        assertDefinition(html, "in def bar")
    }

    fun `test def parent template arguments are elided`() {
        val html = docAtCaret(
            """
                class Op<int x>;
                // Doc.
                def foo : Op<4>;
                defvar v = f<caret>oo;
            """.trimIndent()
        )
        assertDefinition(html, "def foo : Op<...>")
    }

    fun `test class keeps own template arguments and elides parent ones`() {
        val html = docAtCaret(
            """
                class Op<int x>;
                // Doc.
                class Foo<int y> : Op<y>;
                class Bar : F<caret>oo<4>;
            """.trimIndent()
        )
        assertDefinition(html, "class Foo<int y> : Op<...>")
    }

    fun `test template argument default values are elided`() {
        val html = docAtCaret(
            """
                // Doc.
                class Op<int x = 4, string s = "abc">;
                class Bar : O<caret>p<>;
            """.trimIndent()
        )
        assertDefinition(html, "class Op<int x, string s>")
        assertFalse(definitionText(html).contains("="))
    }

    fun `test parent without template arguments is not elided`() {
        val html = docAtCaret(
            """
                class Op;
                // Doc.
                def foo : Op;
                defvar v = f<caret>oo;
            """.trimIndent()
        )
        assertDefinition(html, "def foo : Op")
        assertFalse(definitionText(html).contains("Op<...>"))
    }

    fun `test multiclass documentation`() {
        myFixture.configureByText(
            "test.td", """
                multiclass Base<int x> {
                    def _b;
                }

                // Multiclass doc.
                multiclass Mc<int x> : Base<x> {
                    def _a;
                }
            """.trimIndent()
        )
        val multiclass = PsiTreeUtil.findChildrenOfType(myFixture.file, TableGenMulticlassStatement::class.java)
            .single { it.name == "Mc" }
        val html = documentation(multiclass)
        assertDefinition(html, "multiclass Mc<int x> : Base<...>")
        assertContent(html, "Multiclass doc.")
    }

    fun `test blank line detaches comment`() {
        val html = docAtCaret(
            """
                // Not doc.

                def bar;
                defvar v = ba<caret>r;
            """.trimIndent()
        )
        assertNoContent(html)
    }

    fun `test trailing comment of previous statement is not doc`() {
        val html = docAtCaret(
            """
                def foo; // trailing
                def bar;
                defvar v = ba<caret>r;
            """.trimIndent()
        )
        assertNoContent(html)
    }

    fun `test blank comment line separates paragraphs`() {
        val html = docAtCaret(
            """
                // Para one.
                //
                // Para two.
                def bar;
                defvar v = ba<caret>r;
            """.trimIndent()
        )
        assertContent(html, "Para one.", "Para two.")
    }

    fun `test triple slash comments`() {
        val html = docAtCaret(
            """
                /// Doc.
                def bar;
                defvar v = ba<caret>r;
            """.trimIndent()
        )
        assertContent(html, "Doc.")
    }

    fun `test block comment documentation`() {
        val html = docAtCaret(
            """
                /* Block doc. */
                def bar;
                defvar v = ba<caret>r;
            """.trimIndent()
        )
        assertContent(html, "Block doc.")
    }

    fun `test multi line block comment documentation`() {
        val html = docAtCaret(
            """
                /* Line one.
                 * Line two.
                 */
                def bar;
                defvar v = ba<caret>r;
            """.trimIndent()
        )
        assertContent(html, "Line one. Line two.")
    }

    fun `test blank line within comment block splits it`() {
        val html = docAtCaret(
            """
                // Detached.

                // Doc.
                def bar;
                defvar v = ba<caret>r;
            """.trimIndent()
        )
        assertContent(html, "Doc.")
        assertFalse(html!!.contains("Detached."))
    }

    fun `test comment above field in body`() {
        val html = docAtCaret(
            """
                class B {
                    // Field doc.
                    int j = 0;
                }
                defvar l = B<>.<caret>j
            """.trimIndent()
        )
        assertContent(html, "Field doc.")
    }

    fun `test definition is syntax highlighted`() {
        val html = docAtCaret(
            """
                def bar;
                defvar v = ba<caret>r;
            """.trimIndent()
        )
        val spans = section(html, DocumentationMarkup.CLASS_DEFINITION).select("span")
        assertTrue("expected highlighting spans within:\n$html", spans.isNotEmpty())
    }

    fun `test special characters in comment`() {
        val html = docAtCaret(
            """
                // Value of a & b < c.
                def bar;
                defvar v = ba<caret>r;
            """.trimIndent()
        )
        assertContent(html, "Value of a & b < c.")
        // The special characters must be escaped in the HTML rather than appear literally.
        assertTrue("expected escaped characters within:\n$html", html!!.contains("&amp;"))
    }

    fun `test documentation target presentation`() {
        myFixture.configureByText(
            "test.td", """
                class Foo;
                class Bar : F<caret>oo;
            """.trimIndent()
        )
        val presentation = documentationTarget(myFixture.elementAtCaret)!!.computePresentation()
        assertEquals("Foo", presentation.presentableText)
        assertNotNull(presentation.icon)
    }

    fun `test multiclass target presentation`() {
        myFixture.configureByText(
            "test.td", """
                multiclass Mc {
                    def _a;
                }
            """.trimIndent()
        )
        val multiclass = PsiTreeUtil.findChildOfType(myFixture.file, TableGenMulticlassStatement::class.java)!!
        val presentation = documentationTarget(multiclass)!!.computePresentation()
        assertEquals("Mc", presentation.presentableText)
        assertNotNull(presentation.icon)
    }
}
