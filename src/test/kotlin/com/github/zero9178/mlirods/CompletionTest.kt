package com.github.zero9178.mlirods

import com.github.zero9178.mlirods.language.BANG_OPERATORS
import com.github.zero9178.mlirods.language.generated.TableGenTypes
import com.github.zero9178.mlirods.language.psi.TableGenBangOperator
import com.github.zero9178.mlirods.model.IncludePaths
import com.intellij.codeInsight.lookup.impl.LookupImpl
import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.editor.ex.EditorSettingsExternalizable
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.DumbModeTestUtils
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.testFramework.utils.vfs.createDirectory
import com.intellij.testFramework.utils.vfs.createFile
import kotlin.collections.listOf

class CompletionTest : BasePlatformTestCase() {
    fun `test foldl completion`() = doTest(
        """
            defvar values = [1];
            defvar test = !foldl(0, <caret>, acc, i, i);
        """.trimIndent(), "values", doesNotContain = listOf("acc", "i")
    )


    fun `test foreach completion`() = doTest(
        """
            defvar values = [1];
            defvar test = !foreach(i, <caret>, i);
        """.trimIndent(), "values", doesNotContain = listOf("i")
    )

    fun `test bang operator completion`() = doTest(
        """
            defvar test = !<caret>;
        """.trimIndent(),
        "!add",
        "!eq",
        "!getdagarg",
        "!cast",
        "!cond",
        "!filter",
        "!foldl",
        "!foreach",
        "!sort",
        "!switch",
    )

    /**
     * Every operator with a dedicated token has to be part of [TableGenBangOperator] as well to be offered for
     * completion.
     */
    fun `test bang operator completion contains every dedicated operator`() = doTest(
        """
            defvar test = !<caret>;
        """.trimIndent(),
        *BANG_OPERATORS.types.filter { it != TableGenTypes.BANG_OPERATOR }.map { it.toString() }.toTypedArray()
    )

    fun `test bang operator completion at eof`() = doTest(
        """
            defvar test = !<caret>
        """.trimIndent(), "!add", "!cond"
    )

    fun `test bang operator completion in nested value`() = doTest(
        """
            defvar test = [1, !<caret>];
        """.trimIndent(), "!add", "!cond"
    )

    fun `test bang operator completion typing`() = doTestTyping(
        """
            defvar test = !listrem<caret>;
        """.trimIndent(), """
            defvar test = !listremove(<caret>);
        """.trimIndent()
    )

    fun `test bang operator completion keeps existing parentheses`() = doTestTyping(
        """
            defvar test = !listrem<caret>(1, 2);
        """.trimIndent(), """
            defvar test = !listremove(<caret>1, 2);
        """.trimIndent()
    )

    fun `test cast operator completion typing`() = doTestTyping(
        """
            defvar test = !cas<caret>;
        """.trimIndent(), """
            defvar test = !cast<<caret>>();
        """.trimIndent()
    )

    fun `test cast operator completion adds type argument to existing parentheses`() = doTestTyping(
        """
            defvar test = !cas<caret>(1);
        """.trimIndent(), """
            defvar test = !cast<<caret>>(1);
        """.trimIndent()
    )

    fun `test cast operator completion keeps existing type argument`() = doTestTyping(
        """
            defvar test = !cas<caret><int>(1);
        """.trimIndent(), """
            defvar test = !cast<caret><int>(1);
        """.trimIndent()
    )

    fun `test cast operator completion adds parentheses to existing type argument`() = doTestTyping(
        """
            defvar test = !cas<caret><int>;
        """.trimIndent(), """
            defvar test = !cast<int>(<caret>);
        """.trimIndent()
    )

    fun `test cast operator completion enters existing empty type argument`() = doTestTyping(
        """
            defvar test = !cas<caret><>(1);
        """.trimIndent(), """
            defvar test = !cast<<caret>>(1);
        """.trimIndent()
    )

    fun `test cast operator completion closes unbalanced type argument`() = doTestTyping(
        """
            defvar test = !cas<caret><;
        """.trimIndent(), """
            defvar test = !cast<<caret>>();
        """.trimIndent()
    )

    fun `test cast operator completion closes unbalanced type argument with parentheses`() = doTestTyping(
        """
            defvar test = !cas<caret><int(1);
        """.trimIndent(), """
            defvar test = !cast<int>(<caret>1);
        """.trimIndent()
    )

    fun `test bang operator completion allows tabbing out of parentheses`() = doTestTyping(
        """
            defvar test = !ad<caret>;
        """.trimIndent(), """
            defvar test = !add(1, 2)<caret>;
        """.trimIndent()
    ) {
        myFixture.type("1, 2")
        myFixture.performEditorAction(IdeActions.ACTION_BRACE_OR_QUOTE_OUT)
    }

    fun `test cast operator completion allows tabbing from type argument into parentheses`() = doTestTyping(
        """
            defvar test = !cas<caret>;
        """.trimIndent(), """
            defvar test = !cast<int>(<caret>);
        """.trimIndent()
    ) {
        myFixture.type("int")
        myFixture.performEditorAction(IdeActions.ACTION_BRACE_OR_QUOTE_OUT)
    }

    fun `test cast operator completion allows tabbing into existing parentheses`() = doTestTyping(
        """
            defvar test = !cas<caret>(1);
        """.trimIndent(), """
            defvar test = !cast<int>(<caret>1);
        """.trimIndent()
    ) {
        myFixture.type("int")
        myFixture.performEditorAction(IdeActions.ACTION_BRACE_OR_QUOTE_OUT)
    }

    fun `test cast operator completion selected with angle bracket`() = doTestSelectWithChar(
        """
            defvar test = !c<caret>;
        """.trimIndent(), "!cast", '<', """
            defvar test = !cast<<caret>>();
        """.trimIndent()
    )

    fun `test cast operator completion without automatic parentheses`() {
        val settings = EditorSettingsExternalizable.getInstance()
        val previous = settings.isInsertParenthesesAutomatically
        settings.isInsertParenthesesAutomatically = false
        try {
            doTestTyping(
                """
                    defvar test = !cas<caret>;
                """.trimIndent(), """
                    defvar test = !cast<caret>;
                """.trimIndent()
            )
        } finally {
            settings.isInsertParenthesesAutomatically = previous
        }
    }

    fun `test cast operator completion allows tabbing through both empty pairs`() = doTestTyping(
        """
            defvar test = !cas<caret>;
        """.trimIndent(), """
            defvar test = !cast<>()<caret>;
        """.trimIndent()
    ) {
        myFixture.performEditorAction(IdeActions.ACTION_BRACE_OR_QUOTE_OUT)
        myFixture.performEditorAction(IdeActions.ACTION_BRACE_OR_QUOTE_OUT)
    }

    fun `test cast operator completion allows tabbing out after writing both pairs`() = doTestTyping(
        """
            defvar test = !cas<caret>;
        """.trimIndent(), """
            defvar test = !cast<int>(1)<caret>;
        """.trimIndent()
    ) {
        myFixture.type("int")
        myFixture.performEditorAction(IdeActions.ACTION_BRACE_OR_QUOTE_OUT)
        myFixture.type("1")
        myFixture.performEditorAction(IdeActions.ACTION_BRACE_OR_QUOTE_OUT)
    }

    fun `test field access lookup`() = doTest(
        """
            defvar v = 0;
            
            class B {
                int j = 0;
            }
            
            defvar l = B<>.<caret>
        """.trimIndent(), "j"
    )

    fun `test let field lookup`() = doTest(
        """
            defvar v = 0;
            
            class B {
                int j = 0;
            }
            
            class C : B {
                let <caret>
            }
        """.trimIndent(), "j"
    )

    fun `test field cross file access lookup`() = doCrossFileTestTyping(
        """
            class B {
                int j = 0;
            }
        """.trimIndent(), """
            include "other.td"
                
            defvar l = B<>.<caret>
        """.trimIndent(), """
            include "other.td"
                
            defvar l = B<>.j<caret>
        """.trimIndent()
    )

    fun `test include directory completion`() {
        val testTD = myFixture.createFile(
            "test.td", """
            include "<caret>"
        """.trimIndent()
        )
        var directory = testTD.parent
        directory = WriteAction.computeAndWait<VirtualFile, Throwable> {
            directory.createDirectory("subdir").apply {
                createDirectory("to-complete").apply {
                    createDirectory("second-to-complete")
                }
            }
        }

        installCompileCommands(
            project, mapOf(
                testTD to IncludePaths(listOf(directory))
            )
        )
        myFixture.configureFromExistingVirtualFile(testTD)

        myFixture.completeBasic()
        assertSameElements(
            requireNotNull(myFixture.lookupElementStrings), "to-complete"
        )
        myFixture.type('\t')
        myFixture.checkResult(
            """
            include "to-complete/<caret>"
        """.trimIndent()
        )

        myFixture.type('/')
        myFixture.editor.caretModel.moveCaretRelatively(-1, 0, false, false, false)
        myFixture.completeBasic()
        assertSameElements(
            requireNotNull(myFixture.lookupElementStrings), "second-to-complete"
        )
        myFixture.type('\t')
        myFixture.checkResult(
            """
            include "to-complete/second-to-complete/<caret>"
        """.trimIndent()
        )
    }

    fun `test include file completion`() {
        val testTD = myFixture.createFile(
            "test.td", """
            include "<caret>"
        """.trimIndent()
        )
        var directory = testTD.parent
        directory = WriteAction.computeAndWait<VirtualFile, Throwable> {
            directory.createDirectory("subdir").apply {
                createFile("to-complete.td")
            }
        }

        installCompileCommands(
            project, mapOf(
                testTD to IncludePaths(listOf(directory))
            )
        )
        myFixture.configureFromExistingVirtualFile(testTD)

        myFixture.completeBasic()
        assertSameElements(
            requireNotNull(myFixture.lookupElementStrings), "to-complete.td"
        )
        myFixture.type('\t')

        myFixture.checkResult(
            """
            include "to-complete.td<caret>"
        """.trimIndent()
        )
    }

    fun `test dumb class completion`() {
        doDumbTest(
            """
            class A;
            
            class B<<caret>
        """.trimIndent(),
            "A",
            "bit",
            "int",
            "string",
            "dag",
            "code",
            "list",
            "bits",
        )

        doDumbTest(
            """
            class A;
            
            class B {
                <caret>
            }
        """.trimIndent(),
            "A",
            "bit",
            "int",
            "string",
            "dag",
            "code",
            "list",
            "bits",
            "assert",
            "let",
            "defvar",
        )


        doDumbTest(
            """
            class A;
            
            class B : <caret>
        """.trimIndent(), "A", doesNotContain = listOf("int")
        )
    }

    fun `test dumb top level completion`() = doDumbTest(
        """
        <caret>
    """.trimIndent(),
        "assert",
        "class",
        "def",
        "defm",
        "defset",
        "deftype",
        "defvar",
        "dump",
        "foreach",
        "if",
        "include",
        "let",
        "multiclass"
    )

    fun `test list completion typing`() = doTestTyping(
        """
        class A {
            lis<caret>
        }
    """.trimIndent(), """
        class A {
            list<<caret>>
        }
        """.trimIndent()
    )

    fun `test bits completion typing`() = doTestTyping(
        """
        class A {
            bits<caret>
        }
    """.trimIndent(), """
        class A {
            bits<<caret>>
        }
        """.trimIndent()
    )

    fun `test list completion keeps existing type argument`() = doTestTyping(
        """
        class A {
            lis<caret><int> x;
        }
    """.trimIndent(), """
        class A {
            list<caret><int> x;
        }
        """.trimIndent()
    )

    fun `test list completion enters existing empty type argument`() = doTestTyping(
        """
        class A {
            lis<caret><> x;
        }
    """.trimIndent(), """
        class A {
            list<<caret>> x;
        }
        """.trimIndent()
    )

    fun `test list completion allows tabbing out of type argument`() = doTestTyping(
        """
        class A {
            lis<caret>
        }
    """.trimIndent(), """
        class A {
            list<int><caret>
        }
        """.trimIndent()
    ) {
        myFixture.type("int")
        myFixture.performEditorAction(IdeActions.ACTION_BRACE_OR_QUOTE_OUT)
    }

    fun `test bits completion selected with angle bracket`() = doTestSelectWithChar(
        """
        class A {
            bit<caret>
        }
    """.trimIndent(), "bits", '<', """
        class A {
            bits<<caret>>
        }
        """.trimIndent()
    )

    fun `test space after field`() = doTestTyping(
        """
        class A {
            int<caret>
        }
        """.trimIndent(), """
        class A {
            int <caret>
        }
        """.trimIndent()
    )

    fun `test space after template arg`() = doTestTyping(
        """
        class A<int<caret>
        """.trimIndent(), """
        class A<int <caret>
        """.trimIndent()
    )

    fun `test space after defset`() = doTestTyping(
        """
        defset int<caret>
        """.trimIndent(), """
        defset int <caret>
        """.trimIndent()
    )

    fun `test class angled brackets`() {
        doTestTyping(
            """
            class ALong<int i>;
            
            def : A<caret>;
        """.trimIndent(), """
            class ALong<int i>;
            
            def : ALong<<caret>>;
        """.trimIndent()
        )

        doTestTyping(
            """
            class ALong<int i>;
            
            def {
                list<A<caret>> l;
            }
        """.trimIndent(), """
            class ALong<int i>;
            
            def {
                list<ALong<caret>> l;
            }
        """.trimIndent()
        )

        doTestTyping(
            """
            class BLong;
            defvar v = [B<caret>];
        """.trimIndent(), """
            class BLong;
            defvar v = [BLong<><caret>];
        """.trimIndent()
        )

        doTestTyping(
            """
            class BLong;
            defvar v = [B<caret><>];
        """.trimIndent(), """
            class BLong;
            defvar v = [BLong<caret><>];
        """.trimIndent()
        )
    }

    fun `test class completion enters existing empty brackets`() = doTestTyping(
        """
            class ALong<int i>;

            def : A<caret><>;
        """.trimIndent(), """
            class ALong<int i>;

            def : ALong<<caret>>;
        """.trimIndent()
    )

    fun `test class completion selected with angle bracket`() = doTestSelectWithChar(
        """
            class ALong<int i>;
            class AOther<int i>;

            def : A<caret>;
        """.trimIndent(), "ALong", '<', """
            class ALong<int i>;
            class AOther<int i>;

            def : ALong<<caret>>;
        """.trimIndent()
    )

    fun `test class completion allows tabbing out of brackets`() = doTestTyping(
        """
            class ALong<int i>;

            def : A<caret>;
        """.trimIndent(), """
            class ALong<int i>;

            def : ALong<1><caret>;
        """.trimIndent()
    ) {
        myFixture.type("1")
        myFixture.performEditorAction(IdeActions.ACTION_BRACE_OR_QUOTE_OUT)
    }


    fun `test class cross file access lookup`() = doCrossFileTestTyping(
        """
            class BLong;
        """.trimIndent(), """
            include "other.td"
            defvar l = B<caret>;
        """.trimIndent(), """
            include "other.td"
            defvar l = BLong<><caret>;
        """.trimIndent()
    )

    fun `test def cross file access lookup`() = doCrossFileTestTyping(
        """
            def BLong;
        """.trimIndent(), """
            include "other.td"
            defvar l = B<caret>;
        """.trimIndent(), """
            include "other.td"
            defvar l = BLong<caret>;
        """.trimIndent()
    )

    fun `test defvar cross file access lookup`() = doCrossFileTestTyping(
        """
            defvar BLong = 0;
        """.trimIndent(), """
            include "other.td"
            defvar l = B<caret>;
        """.trimIndent(), """
            include "other.td"
            defvar l = BLong<caret>;
        """.trimIndent()
    )

    private fun doTest(source: String, vararg expected: String, doesNotContain: List<String> = emptyList()) {
        myFixture.configureByText(
            "test.td", source
        )


        myFixture.completeBasic()
        val collection = requireNotNull(myFixture.lookupElementStrings)
        assertContainsElements(collection, *expected)
        assertDoesntContain(collection, doesNotContain)
    }

    private fun doTestTyping(source: String, expectedText: String, afterCompletion: () -> Unit = {}) {
        myFixture.configureByText(
            "test.td", source
        )

        myFixture.completeBasicAllCarets(null)
        afterCompletion()
        myFixture.checkResult(expectedText)
    }

    /**
     * Completes at the caret in [source], selecting [lookupString] from the lookup by typing [completionChar].
     */
    private fun doTestSelectWithChar(
        source: String, lookupString: String, completionChar: Char, expectedText: String
    ) {
        myFixture.configureByText(
            "test.td", source
        )

        myFixture.completeBasic()
        val lookup = myFixture.lookup as LookupImpl
        lookup.currentItem = myFixture.lookupElements!!.first { it.lookupString == lookupString }
        myFixture.finishLookup(completionChar)
        myFixture.checkResult(expectedText)
    }

    private fun doCrossFileTestTyping(otherTD: String, source: String, expectedText: String) {
        val otherTD = myFixture.configureByText(
            "other.td", otherTD
        )

        val testTD = myFixture.configureByText(
            "test.td", source
        )
        installCompileCommands(
            project, mapOf(
                testTD.virtualFile to IncludePaths(listOf(otherTD.virtualFile.parent))
            )
        )

        myFixture.completeBasicAllCarets(null, '\t')
        myFixture.checkResult(expectedText)
    }

    private fun doDumbTest(source: String, vararg expected: String, doesNotContain: List<String> = emptyList()) =
        DumbModeTestUtils.runInDumbModeSynchronously(project) {
            doTest(source, *expected, doesNotContain = doesNotContain)
        }
}