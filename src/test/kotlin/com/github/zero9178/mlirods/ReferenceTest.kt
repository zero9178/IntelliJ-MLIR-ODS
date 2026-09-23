package com.github.zero9178.mlirods

import com.github.zero9178.mlirods.language.generated.psi.*
import com.github.zero9178.mlirods.language.psi.TableGenClassReference
import com.github.zero9178.mlirods.model.IncludePaths
import com.github.zero9178.mlirods.model.TableGenIncludeGraphService
import com.github.zero9178.mlirods.model.TableGenCompilationContext
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.components.service
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiPolyVariantReference
import com.intellij.psi.util.parentOfType
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.testFramework.utils.vfs.deleteRecursively

class ReferenceTest : BasePlatformTestCase() {
    fun `test IncludeReference`() {
        val testFile = myFixture.copyFileToProject("test.td")
        val virtualFile = myFixture.copyFileToProject("HasCompileCommands.td")
        val targetFile = myFixture.copyFileToProject("IncludeReference.td")
        installCompileCommands(
            project, mapOf(
                virtualFile to IncludePaths(listOf(testFile.parent))
            )
        )

        myFixture.configureFromExistingVirtualFile(targetFile)
        val element = assertInstanceOf(myFixture.elementAtCaret, PsiFile::class.java)
        assertEquals(element.viewProvider.virtualFile.name, "test.td")
    }

    fun `test infinite include recursion`() = assertNull(
        resolveAcrossFiles(
            "other.td" to """include "test.td"""",
            "test.td" to """
                include "other.td"

                defvar i = <caret>a;
            """
        )
    )

    fun `test class of include before reference`() = assertResolvesToFile(
        "a.td", resolveAcrossFiles(
            "a.td" to "class A;",
            "root.td" to """
                include "a.td"
                def : <caret>A;
            """
        )
    )

    fun `test class of include after reference`() = assertNull(
        resolveAcrossFiles(
            "a.td" to "class A;",
            "root.td" to """
                def : <caret>A;
                include "a.td"
            """
        )
    )

    fun `test class of transitive include before reference`() = assertResolvesToFile(
        "a.td", resolveAcrossFiles(
            "a.td" to "class A;",
            "mid.td" to """include "a.td"""",
            "root.td" to """
                include "mid.td"
                def : <caret>A;
            """
        )
    )

    fun `test class of transitive include after reference`() = assertNull(
        resolveAcrossFiles(
            "a.td" to "class A;",
            "mid.td" to """include "a.td"""",
            "root.td" to """
                def : <caret>A;
                include "mid.td"
            """
        )
    )

    fun `test class only of includes before reference`() = assertResolvesToFile(
        "a.td", resolveAcrossFiles(
            "a.td" to "class A;",
            "b.td" to "class A;",
            "root.td" to """
                include "a.td"
                def : <caret>A;
                include "b.td"
            """
        )
    )

    fun `test class of includer before include`() = assertResolvesToFile(
        "root.td", resolveAcrossFiles(
            "test.td" to "def : <caret>A;",
            "root.td" to """
                class A;
                include "test.td"
            """
        )
    )

    fun `test class of includer after include`() = assertNull(
        resolveAcrossFiles(
            "test.td" to "def : <caret>A;",
            "root.td" to """
                include "test.td"
                class A;
            """
        )
    )

    fun `test class of transitive includer before include`() = assertResolvesToFile(
        "root.td", resolveAcrossFiles(
            "test.td" to "def : <caret>A;",
            "mid.td" to """include "test.td"""",
            "root.td" to """
                class A;
                include "mid.td"
            """
        )
    )

    fun `test class of transitive includer after include`() = assertNull(
        resolveAcrossFiles(
            "test.td" to "def : <caret>A;",
            "mid.td" to """include "test.td"""",
            "root.td" to """
                include "mid.td"
                class A;
            """
        )
    )

    fun `test class of includer between repeated includes`() = assertNull(
        resolveAcrossFiles(
            "test.td" to "def : <caret>A;",
            "root.td" to """
                include "test.td"
                class A;
                include "test.td"
            """
        )
    )

    fun `test class of file included before`() = assertResolvesToFile(
        "a.td", resolveAcrossFiles(
            "a.td" to "class A;",
            "test.td" to "def : <caret>A;",
            "root.td" to """
                include "a.td"
                include "test.td"
            """
        )
    )

    fun `test class of file included after`() = assertNull(
        resolveAcrossFiles(
            "a.td" to "class A;",
            "test.td" to "def : <caret>A;",
            "root.td" to """
                include "test.td"
                include "a.td"
            """
        )
    )

    fun `test class of file included by file included before`() = assertResolvesToFile(
        "a.td", resolveAcrossFiles(
            "a.td" to "class A;",
            "sibling.td" to """include "a.td"""",
            "test.td" to "def : <caret>A;",
            "root.td" to """
                include "sibling.td"
                include "test.td"
            """
        )
    )

    fun `test class of file included before despite repeated include after reference`() = assertResolvesToFile(
        "a.td", resolveAcrossFiles(
            "a.td" to "class A;",
            "test.td" to """
                def : <caret>A;
                include "a.td"
            """,
            "root.td" to """
                include "a.td"
                include "test.td"
            """
        )
    )

    fun `test class declared before include defining it`() {
        // All statements of the class preceding the reference are found, the closest one last.
        resolveAcrossFiles(
            "a.td" to "class A { int i = 0; }",
            "root.td" to """
                class A;
                include "a.td"
                def : <caret>A;
            """
        )
        val reference = myFixture.file.findReferenceAt(myFixture.caretOffset) as PsiPolyVariantReference
        assertEquals(listOf("root.td", "a.td"), reference.multiResolve(false).map { it.element?.containingFile?.name })
    }

    fun `test class of other file without context`() = assertNull(
        resolveAcrossFiles(
            "a.td" to "class A;",
            "test.td" to """
                include "a.td"
                def : <caret>A;
            """,
            "root.td" to ""
        )
    )

    fun `test class of file included after despite include cycle`() = assertNull(
        resolveAcrossFiles(
            "a.td" to "class A;",
            "test.td" to """
                include "root.td"
                def : <caret>A;
            """,
            "root.td" to """
                include "test.td"
                include "a.td"
            """
        )
    )

    fun `test IncludeReference exception`() {
        val testFile = myFixture.copyFileToProject("test.td")
        val virtualFile = myFixture.copyFileToProject("HasCompileCommands.td", "toBeDeleted/HasCompileCommands.td")
        val targetFile = myFixture.copyFileToProject("IncludeReference.td")
        installCompileCommands(
            project, mapOf(
                targetFile to IncludePaths(listOf(virtualFile.parent, testFile.parent))
            )
        )
        runWriteAction {
            virtualFile.parent.deleteRecursively()
        }

        myFixture.configureFromExistingVirtualFile(targetFile)
        val element = assertInstanceOf(myFixture.elementAtCaret, PsiFile::class.java)
        assertEquals(element.viewProvider.virtualFile.name, "test.td")
    }

    fun `test GlobalDefResolution`() {
        val element = doTest<TableGenDefvarStatement>("test.td")
        assertEquals(element.name, "f")
        assertEquals(element.containingFile.name, "test.td")
    }

    fun `test LocalDefvarResolution`() {
        val element = doTest<TableGenDefvarStatement>()
        assertEquals(element.name, "i")
        assertNotNull(element.parentOfType<TableGenClassStatement>())
    }

    fun `test ClassArgResolution`() {
        val element = doTest<TableGenTemplateArgDecl>()
        assertEquals(element.name, "i")
        assertNotNull(element.parentOfType<TableGenClassStatement>())
    }

    fun `test ClassArgInheritanceResolution`() {
        val element = doTest<TableGenTemplateArgDecl>()
        assertEquals(element.name, "i")
        assertNotNull(element.parentOfType<TableGenClassStatement>())
    }

    fun `test LocalDefvarStatementResolution`() {
        val element = doTest<TableGenDefvarStatement>()
        assertEquals(element.name, "i")
        assertNotNull(element.parentOfType<TableGenIfBody>())
    }

    fun `test DefVarShadowingResolution`() {
        val element = doTest<TableGenDefvarStatement>()
        assertEquals(element.name, "i")
        assertNotNull(element.parentOfType<TableGenIfBody>())
        assertNull(element.parentOfType<TableGenIfBody>()?.parentOfType<TableGenIfBody>())
    }

    fun `test DefInIncludeResolution`() {
        val element = doTest<TableGenDefvarStatement>("test2.td", "test.td")
        assertEquals(element.name, "f")
        assertEquals(element.containingFile.name, "test2.td")
    }

    fun `test def of include before reference`() = assertResolvesToFile(
        "a.td", resolveAcrossFiles(
            "a.td" to "def A;",
            "root.td" to """
                include "a.td"
                defvar v = <caret>A;
            """
        )
    )

    fun `test def of include after reference`() = assertNull(
        resolveAcrossFiles(
            "a.td" to "def A;",
            "root.td" to """
                defvar v = <caret>A;
                include "a.td"
            """
        )
    )

    fun `test def of includer before include`() = assertResolvesToFile(
        "root.td", resolveAcrossFiles(
            "test.td" to "defvar v = <caret>A;",
            "root.td" to """
                def A;
                include "test.td"
            """
        )
    )

    fun `test def of includer after include`() = assertNull(
        resolveAcrossFiles(
            "test.td" to "defvar v = <caret>A;",
            "root.td" to """
                include "test.td"
                def A;
            """
        )
    )

    fun `test def of file included before`() = assertResolvesToFile(
        "a.td", resolveAcrossFiles(
            "a.td" to "def A;",
            "test.td" to "defvar v = <caret>A;",
            "root.td" to """
                include "a.td"
                include "test.td"
            """
        )
    )

    fun `test def of file included after`() = assertNull(
        resolveAcrossFiles(
            "a.td" to "def A;",
            "test.td" to "defvar v = <caret>A;",
            "root.td" to """
                include "test.td"
                include "a.td"
            """
        )
    )

    fun `test ParentClassListResolution`() {
        val element = doTest<TableGenClassStatement>("ParentClassListResolution.td")
        assertEquals(element.name, "F")
    }

    fun `test ClassInstantiationResolution`() {
        val element = doTest<TableGenClassStatement>("ParentClassListResolution.td")
        assertEquals(element.name, "F")
    }

    fun `test ClassTypeResolution`() {
        val element = doTest<TableGenClassStatement>("ParentClassListResolution.td")
        assertEquals(element.name, "F")
    }

    fun `test ClassSelfTypeResolution`() {
        val element = doTest<TableGenClassStatement>("ClassSelfTypeResolution.td")
        assertEquals(element.name, "G")
    }

    fun `test class after reference`() = assertNull(
        resolveAcrossFiles(
            "root.td" to """
                def : <caret>A;
                class A;
            """
        )
    )

    fun `test GlobalClassInstantiationResolution`() {
        val element = doTest<TableGenClassStatement>("GlobalClassInstantiationResolution.td", "test.td")
        assertEquals(element.name, "F")
        assertEquals(element.containingFile.name, "test.td")
    }

    fun `test FieldDefResolution`() {
        val element = doTest<TableGenFieldBodyItem>()
        assertEquals(element.name, "i")
    }

    fun `test FieldDefResolutionLexical`() {
        val element = doTest<TableGenDefvarStatement>()
        assertEquals(element.name, "i")
    }

    fun `test FieldDefParentResolutionLexical`() {
        val element = doTest<TableGenTemplateArgDecl>()
        assertEquals(element.name, "i")
    }

    fun `test ParentClassFieldDefResolution`() {
        val element = doTest<TableGenFieldBodyItem>()
        assertEquals(element.name, "i")
        val parentClass = assertInstanceOf(element.parent, TableGenClassStatement::class.java)
        assertEquals(parentClass.name, "A")
    }

    fun `test GlobalFieldDefResolution`() {
        val element = doTest<TableGenFieldBodyItem>("test.td")
        assertEquals(element.name, "i")
        val parentClass = assertInstanceOf(element.parent, TableGenClassStatement::class.java)
        assertEquals(parentClass.name, "A")
        assertEquals(parentClass.containingFile.name, "test.td")
    }

    fun `test ClassTemplateArgFieldResolutionPos`() {
        val element = doTest<TableGenTemplateArgDecl>()
        assertEquals(element.name, "i")
        val parentClass = assertInstanceOf(element.parent, TableGenClassStatement::class.java)
        assertEquals(parentClass.name, "Foobar")
    }

    fun `test ClassTemplateArgFieldResolutionNeg`() {
        val element = doTest<TableGenFieldBodyItem>()
        assertEquals(element.name, "i")
        val parentClass = assertInstanceOf(element.parent, TableGenClassStatement::class.java)
        assertEquals(parentClass.name, "Bar")
    }

    fun `test LetBodyItemResolution`() {
        val element = doTest<TableGenFieldBodyItem>()
        assertEquals(element.name, "i")
    }

    fun `test FieldAccessResolution`() {
        val element = doTest<TableGenFieldBodyItem>()
        assertEquals(element.name, "i")
    }

    fun `test ForeachDefvarResolution`() {
        val element = doTest<TableGenBangOperatorDefinition>()
        assertEquals(element.name, "i")
    }

    fun `test ForeachDefvarResolutionIterable`() {
        val element = doTest<TableGenDefvarStatement>()
        assertEquals(element.name, "i")
    }

    fun `test ForeachDefvarResolutionIterableParent`() {
        val element = doTest<TableGenDefvarStatement>()
        assertEquals(element.name, "i")
    }

    fun `test FoldlIteratorDefvarResolution`() {
        val element = doTest<TableGenBangOperatorDefinition>()
        assertEquals(element.name, "i")
    }

    fun `test FoldlAccDefvarResolution`() {
        val element = doTest<TableGenBangOperatorDefinition>()
        assertEquals(element.name, "acc")
    }


    fun `test FoldlDefvarResolutionIterable`() {
        val element = doTest<TableGenDefvarStatement>()
        assertEquals(element.name, "i")
    }

    fun `test SortDefvarResolution`() {
        val element = doTestInline<TableGenBangOperatorDefinition>(
            "defvar a = !sort(i, [5], <caret>i);"
        )
        assertEquals(element.name, "i")
    }

    fun `test FilterDefvarResolution`() {
        val element = doTestInline<TableGenBangOperatorDefinition>(
            "defvar a = !filter(i, [5], <caret>i);"
        )
        assertEquals(element.name, "i")
    }

    fun `test ParentMultiClassListResolution`() {
        // The first name of a 'defm' refers to a multiclass, even if a class of the same name is visible.
        val element = doTest<TableGenMulticlassStatement>()
        assertEquals("F", element.name)
    }

    fun `test defm name following a multiclass resolves to multiclass`() {
        val element = doTestInline<TableGenMulticlassStatement>(
            """
            multiclass M { def a; }
            multiclass N { def b; }
            defm d : M, <caret>N;
        """.trimIndent()
        )
        assertEquals("N", element.name)
    }

    fun `test defm name following a multiclass resolves to class of the same name`() {
        // A name following the first one refers to a class if there is a class of that name, even if there is a
        // multiclass of the same name as well.
        val element = doTestInline<TableGenClassStatement>(
            """
            multiclass M { def a; }
            class N;
            multiclass N { def b; }
            defm d : M, <caret>N;
        """.trimIndent()
        )
        assertEquals("N", element.name)
    }

    fun `test defm names following a class refer to classes`() = assertNull(
        resolveAcrossFiles(
            "test.td" to """
                multiclass M { def a; }
                class C;
                multiclass N { def b; }
                defm d : M, C, <caret>N;
            """
        )
    )

    fun `test defm name only refers to class preceding it`() {
        val element = doTestInline<TableGenMulticlassStatement>(
            """
            multiclass M { def a; }
            multiclass N { def b; }
            defm d : M, <caret>N;
            class N;
        """.trimIndent()
        )
        assertEquals("N", element.name)
    }

    fun `test multiclass parent resolves to multiclass despite class of the same name`() {
        // Unlike in a 'defm', every name of a 'multiclass' statement refers to a multiclass.
        val element = doTestInline<TableGenMulticlassStatement>(
            """
            multiclass M { def a; }
            class C;
            multiclass C { def c; }
            multiclass N : M, <caret>C;
        """.trimIndent()
        )
        assertEquals("C", element.name)
    }

    fun `test defm in multiclass resolves to multiclass`() {
        val element = doTestInline<TableGenMulticlassStatement>(
            """
            multiclass M { def a; }
            multiclass N {
                defm b : <caret>M;
            }
        """.trimIndent()
        )
        assertEquals("M", element.name)
    }

    fun `test multiclass after reference`() = assertNull(
        resolveAcrossFiles(
            "test.td" to """
                defm d : <caret>M;
                multiclass M { def a; }
            """
        )
    )

    fun `test multiclass of include before reference`() = assertResolvesToFile(
        "a.td", resolveAcrossFiles(
            "a.td" to "multiclass M { def a; }",
            "root.td" to """
                include "a.td"
                defm d : <caret>M;
            """
        )
    )

    fun `test multiclass of include after reference`() = assertNull(
        resolveAcrossFiles(
            "a.td" to "multiclass M { def a; }",
            "root.td" to """
                defm d : <caret>M;
                include "a.td"
            """
        )
    )

    fun `test multiclass defined twice resolves to the last definition`() = assertResolvesToFile(
        // TableGen rejects every definition of a multiclass following the first one, but the one closest to the
        // reference is the one more likely to be meant.
        "b.td", resolveAcrossFiles(
            "a.td" to "multiclass M { def a; }",
            "b.td" to "multiclass M { def b; }",
            "root.td" to """
                include "a.td"
                include "b.td"
                defm d : <caret>M;
            """
        )
    )

    fun `test in let statement`() {
        val statement = doTestInline<TableGenDefStatement>(
            """
            class AArch64Unsupported { list<Predicate> F; }

            let F = [] in def SVE2p1Unsupported : AArch64Unsupported;

            defvar v = <caret>SVE2p1Unsupported;
        """.trimIndent()
        )
        assertEquals(statement.name, "SVE2p1Unsupported")
    }

    fun `test class decl statement`() {
        val field = doTestInline<TableGenFieldBodyItem>(
            """
            class Instruction;

            class Instruction {
                int i = 0;
            }
            
            def : Instruction {
                let <caret>i = 5;
            }
        """.trimIndent()
        )
        assertEquals(field.fieldName, "i")
    }

    fun `test foreach statement`() {
        val iter = doTestInline<TableGenForeachIterator>(
            """
            foreach i = [0, 1, 2] in {
                defvar v = <caret>i;
            }
        """.trimIndent()
        )
        assertEquals("i", iter.name)
    }

    fun `test multiclass template arg resolution`() {
        val element = doTestInline<TableGenTemplateArgDecl>(
            """
            multiclass M<int i> {
                defvar v = <caret>i;
            }
        """.trimIndent()
        )
        assertEquals("i", element.name)
        assertNotNull(element.parentOfType<TableGenMulticlassStatement>())
    }

    fun `test multiclass defvar scope resolution`() {
        val element = doTestInline<TableGenDefvarStatement>(
            """
            multiclass M {
                defvar i = 0;
                defvar v = <caret>i;
            }
        """.trimIndent()
        )
        assertEquals("i", element.name)
        assertNotNull(element.parentOfType<TableGenMulticlassStatement>())
    }

    fun `test multiclass def not referenceable`() = assertNull(
        resolveAcrossFiles(
            "test.td" to """
                multiclass M {
                    def foo;
                    def bar { int x = <caret>foo; }
                }
            """
        )
    )

    fun `test multiclass def does not shadow global def`() {
        val element = doTestInline<TableGenDefStatement>(
            """
            def foo;

            multiclass M {
                def foo;
                def bar { int x = <caret>foo; }
            }
        """.trimIndent()
        )
        assertEquals("foo", element.name)
        assertNull(element.parentOfType<TableGenMulticlassStatement>())
    }

    fun `test multiclass def not in index`() = assertNull(
        resolveAcrossFiles(
            "test.td" to """
                multiclass M {
                    foreach i = [0] in {
                        def foo;
                    }
                }

                defvar v = <caret>foo;
            """
        )
    )

    fun `test nested def before reference`() = assertResolvesToFile(
        "root.td", resolveAcrossFiles(
            "root.td" to """
                foreach i = [0] in {
                    def A;
                }
                defvar v = <caret>A;
            """
        )
    )

    fun `test nested def after reference`() = assertNull(
        resolveAcrossFiles(
            "root.td" to """
                defvar v = <caret>A;
                foreach i = [0] in {
                    def A;
                }
            """
        )
    )

    fun `test multiclass defvar shadows outer defvar`() {
        val element = doTestInline<TableGenDefvarStatement>(
            """
            defvar i = 0;
            multiclass M {
                defvar i = 1;
                defvar v = <caret>i;
            }
        """.trimIndent()
        )
        assertEquals("i", element.name)
        assertNotNull(element.parentOfType<TableGenMulticlassStatement>())
    }

    fun `test append let`() {
        val iter = doTestInline<TableGenFieldBodyItem>(
            """
            class F {
                list<int> i = [];

                let append <caret>i = [10];
            }
        """.trimIndent()
        )
        assertEquals("i", iter.name)
    }

    fun `test named arg identifier resolution`() {
        val element = doTestInline<TableGenTemplateArgDecl>(
            """
            class F<int i, int j>;

            def : F<<caret>i = 0, j = 1>;
        """.trimIndent()
        )
        assertEquals("i", element.name)
        assertEquals("F", element.parentOfType<TableGenClassStatement>()?.name)
    }

    fun `test named arg identifier resolution out of order`() {
        // Named arguments resolve by name, not position.
        val element = doTestInline<TableGenTemplateArgDecl>(
            """
            class F<int i, int j>;

            def : F<j = 1, <caret>i = 0>;
        """.trimIndent()
        )
        assertEquals("i", element.name)
    }

    // Positional arguments do not expose a UI reference.
    fun `test positional arg has no reference`() = assertNull(
        resolveAcrossFiles(
            "test.td" to """
                class F<int i, int j>;

                def : F<10, <caret>20>;
            """
        )
    )

    fun `test unresolved named arg`() = assertNull(
        resolveAcrossFiles(
            "test.td" to """
                class F<int i>;

                def : F<<caret>unknown = 0>;
            """
        )
    )

    fun `test ifdef resolves to define`() {
        val element = doTestInline<TableGenDefineDirective>(
            """
            #define FOO
            #ifdef <caret>FOO
            #endif
        """.trimIndent()
        )
        assertEquals("FOO", element.macroName)
    }

    fun `test ifdef resolves to define after it`() {
        // Resolution is not order-sensitive within a file: a '#define' following the '#ifdef' still resolves.
        val element = doTestInline<TableGenDefineDirective>(
            """
            #ifdef <caret>FOO
            #endif
            #define FOO
        """.trimIndent()
        )
        assertEquals("FOO", element.macroName)
    }

    fun `test ifndef resolves to define`() {
        val element = doTestInline<TableGenDefineDirective>(
            """
            #ifndef <caret>FOO
            #define FOO
            #endif
        """.trimIndent()
        )
        assertEquals("FOO", element.macroName)
    }

    fun `test ifdef resolves to define in include`() = assertResolvesToFile(
        "define.td", resolveAcrossFiles(
            "define.td" to "#define FOO",
            "test.td" to """
                #ifdef <caret>FOO
                #endif
            """,
            "root.td" to """
                include "define.td"
                include "test.td"
            """
        )
    )

    fun `test inherited field context follows the base class`() {
        val mainVF = myFixture.createFile(
            "test.td", """
            class A {
            }
            class B : A {
                defvar v = <caret>i;
            }
        """.trimIndent()
        )
        installCompileCommands(
            project, mapOf(
                mainVF to IncludePaths(emptyList())
            )
        )
        myFixture.configureFromExistingVirtualFile(mainVF)

        // Resolving 'i' populates the id map of 'B' while 'A' does not declare it yet.
        assertNull(myFixture.file.findReferenceAt(myFixture.caretOffset)?.resolve())

        // Declare the field in 'A', which is outside of the subtree of 'B'.
        val document = myFixture.getDocument(myFixture.file)
        WriteCommandAction.runWriteCommandAction(project) {
            document.insertString(document.text.indexOf('{') + 1, "int i = 5;")
            PsiDocumentManager.getInstance(project).commitDocument(document)
        }

        val element = assertInstanceOf(
            myFixture.file.findReferenceAt(myFixture.caretOffset)?.resolve(), TableGenFieldBodyItem::class.java
        )
        assertEquals("A", element.parentOfType<TableGenClassStatement>()?.name)
    }

    fun `test inherited field context of a def follows the base class`() {
        val mainVF = myFixture.createFile(
            "test.td", """
            class A {
            }
            def B : A {
                defvar v = <caret>i;
            }
        """.trimIndent()
        )
        installCompileCommands(
            project, mapOf(
                mainVF to IncludePaths(emptyList())
            )
        )
        myFixture.configureFromExistingVirtualFile(mainVF)

        // Resolving 'i' populates the id map of 'B' while 'A' does not declare it yet.
        assertNull(myFixture.file.findReferenceAt(myFixture.caretOffset)?.resolve())

        // Declare the field in 'A', which is outside of the subtree of 'B'.
        val document = myFixture.getDocument(myFixture.file)
        WriteCommandAction.runWriteCommandAction(project) {
            document.insertString(document.text.indexOf('{') + 1, "int i = 5;")
            PsiDocumentManager.getInstance(project).commitDocument(document)
        }

        val element = assertInstanceOf(
            myFixture.file.findReferenceAt(myFixture.caretOffset)?.resolve(), TableGenFieldBodyItem::class.java
        )
        assertEquals("A", element.parentOfType<TableGenClassStatement>()?.name)
    }

    // A macro that is never '#define'd resolves to nothing (soft reference).
    fun `test ifndef unresolved`() = assertNull(
        resolveAcrossFiles(
            "test.td" to """
                #ifndef <caret>FOO
                #endif
            """
        )
    )

    override fun getTestDataPath(): String? {
        return "src/test/testData/references"
    }

    private inline fun <reified T> doTest(vararg additionalFiles: String): T {
        val name = getTestName(false).trim()

        val mainVF = myFixture.copyFileToProject("${name}.td")
        val list = additionalFiles.map { myFixture.copyFileToProject(it).parent }.toList()
        installCompileCommands(
            project, mapOf(
                mainVF to IncludePaths(list)
            )
        )

        myFixture.configureFromExistingVirtualFile(mainVF)
        return assertInstanceOf(myFixture.elementAtCaret, T::class.java)
    }


    private inline fun <reified T> doTestInline(source: String): T =
        assertInstanceOf(resolveAcrossFiles("test.td" to source), T::class.java)

    fun `test class resolves within the root asked for`() {
        // A file pasted in by two roots sees a different 'A' in each of them: whichever the root pastes in before it.
        myFixture.createFile("a.td", "class A;")
        myFixture.createFile("b.td", "class A;")
        val shared = myFixture.createFile("shared.td", "def : A;")
        val rootA = myFixture.createFile("rootA.td", "include \"a.td\"\ninclude \"shared.td\"")
        val rootB = myFixture.createFile("rootB.td", "include \"b.td\"\ninclude \"shared.td\"")
        val paths = IncludePaths(listOf(shared.parent))
        val update = compileCommandsUpdater(project)
        update(mapOf(rootA to paths, rootB to paths))

        myFixture.configureFromExistingVirtualFile(shared)
        val reference = myFixture.file.findReferenceAt(myFixture.file.text.indexOf("A;")) as TableGenClassReference
        val service = project.service<TableGenIncludeGraphService>()
        fun resolvedIn(context: TableGenCompilationContext) =
            TableGenClassReference.findVisibleClasses(reference.element, context).map { it.containingFile.name }

        // The platform resolves in the context of the first root reaching the file.
        assertResolvesToFile("a.td", reference.resolve())
        // Asking for the other root yields its answer rather than what was cached for the first one, and vice versa.
        assertEquals(listOf("b.td"), resolvedIn(service.compilationContextOf(rootB)))
        assertEquals(listOf("a.td"), resolvedIn(service.compilationContextOf(rootA)))

        // Swapping the priority of the roots switches what the platform sees.
        update(mapOf(rootB to paths, rootA to paths))
        assertResolvesToFile("b.td", reference.resolve())
    }

    /**
     * Creates [files], given as pairs of name and content with the root of the compilation being last, and returns what
     * the reference at the caret resolves to.
     */
    private fun resolveAcrossFiles(vararg files: Pair<String, String>): PsiElement? {
        val virtualFiles = files.map { (name, content) -> myFixture.createFile(name, content.trimIndent()) }
        val root = virtualFiles.last()
        installCompileCommands(project, mapOf(root to IncludePaths(listOf(root.parent))))

        myFixture.configureFromExistingVirtualFile(virtualFiles[files.indexOfFirst { "<caret>" in it.second }])
        return myFixture.file.findReferenceAt(myFixture.caretOffset)?.resolve()
    }

    private fun assertResolvesToFile(file: String, element: PsiElement?) {
        assertNotNull(element)
        assertEquals(file, element!!.containingFile.name)
    }
}
