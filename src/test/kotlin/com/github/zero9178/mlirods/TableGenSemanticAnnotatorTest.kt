package com.github.zero9178.mlirods

import com.github.zero9178.mlirods.model.IncludePaths
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class TableGenSemanticAnnotatorTest : BasePlatformTestCase() {

    fun `test all required arguments provided`() {
        doResolvingTest(
            """
            class C<int a, int b>;
            def D : C<1, 2>;
        """.trimIndent()
        )
    }

    fun `test default arguments may be omitted`() {
        doResolvingTest(
            """
            class C<int a, int b = 0>;
            def D : C<1>;
        """.trimIndent()
        )
    }

    fun `test duplicate named argument`() {
        doResolvingTest(
            """
            class C<int a, int b = 0>;
            def D : C<a = 1, <error descr="Template argument 'a' is assigned more than once">a = 2</error>>;
        """.trimIndent()
        )
    }

    fun `test duplicate positional and named argument`() {
        doResolvingTest(
            """
            class C<int a, int b = 0>;
            def D : C<1, <error descr="Template argument 'a' is assigned more than once">a = 2</error>>;
        """.trimIndent()
        )
    }

    fun `test unknown named argument`() {
        doResolvingTest(
            """
            class C<int a = 0>;
            def D : C<<error descr="Class 'C' has no template argument named 'b'">b = 1</error>>;
        """.trimIndent()
        )
    }

    fun `test too many positional arguments`() {
        doResolvingTest(
            """
            class C<int a, int b>;
            def D : C<1, 2, <error descr="Too many arguments for class 'C'; expected at most 2">3</error>>;
        """.trimIndent()
        )
    }

    fun `test missing required argument`() {
        doResolvingTest(
            """
            class C<int a>;
            def D : <error descr="Missing value for required template argument 'a'">C</error>;
        """.trimIndent()
        )
    }

    fun `test class used as type is not validated`() {
        // A class referenced as a field type does not pass template arguments and must not be flagged.
        doResolvingTest(
            """
            class C<int a>;
            class D { C f; }
        """.trimIndent()
        )
    }

    fun `test class with decl`() {
        doResolvingTest(
            """
            class C;

            class C<int a>;

            class D : C<0>;
        """.trimIndent()
        )
    }

    fun `test argument of matching type is not flagged`() {
        doResolvingTest(
            """
            class C<string a>;
            def D : C<"hello">;
        """.trimIndent()
        )
    }

    fun `test argument of a convertible type is not flagged`() {
        // A 'bit' is convertible to an 'int'.
        doResolvingTest(
            """
            class B { bit x = 1; }
            class C<int a>;
            def D : C<B<>.x>;
        """.trimIndent()
        )
    }

    fun `test argument of a mismatching type is flagged`() {
        doResolvingTest(
            """
            class C<string a>;
            def D : C<<error descr="Value of type 'int' cannot be assigned to template argument 'a' of type 'string'">1</error>>;
        """.trimIndent()
        )
    }

    fun `test argument named by a string is checked against that template argument`() {
        // A named argument may use a string literal instead of an identifier as the name.
        doResolvingTest(
            """
            class C<int a, string b>;
            def D : C<0, "b" = <error descr="Value of type 'int' cannot be assigned to template argument 'b' of type 'string'">1</error>>;
        """.trimIndent()
        )
    }

    fun `test named argument of a mismatching type is flagged`() {
        doResolvingTest(
            """
            class C<int a = 0>;
            def D : C<a = <error descr="Value of type 'string' cannot be assigned to template argument 'a' of type 'int'">"oops"</error>>;
        """.trimIndent()
        )
    }

    fun `test undef argument is not flagged`() {
        doResolvingTest(
            """
            class C<int a>;
            def D : C<?>;
        """.trimIndent()
        )
    }

    fun `test argument of a subclass record type is not flagged`() {
        doResolvingTest(
            """
            class Base;
            class Derived : Base;
            class C<Base b>;
            def D : C<Derived<>>;
        """.trimIndent()
        )
    }

    fun `test argument of an unrelated record type is flagged`() {
        doResolvingTest(
            """
            class Base;
            class Other;
            class C<Base b>;
            def D : C<<error descr="Value of type 'Other' cannot be assigned to template argument 'b' of type 'Base'">Other<></error>>;
        """.trimIndent()
        )
    }

    /**
     * Creates the files the forward-declaration tests share, mirroring LLVM: 'Target.td' includes
     * 'TargetInstrPredicate.td', which forward declares 'Instruction' and already uses it as a template argument type,
     * and only then defines 'Instruction' itself.
     */
    private fun addForwardDeclaredInstruction() {
        myFixture.addFileToProject(
            "predicate.td", """
            class Instruction;
            class CheckOpcode<list<Instruction> opcodes>;
        """.trimIndent()
        )
        myFixture.addFileToProject(
            "target.td", """
            include "predicate.td"
            class Instruction { int size = 0; }
        """.trimIndent()
        )
    }

    fun `test argument deriving from a forward declared class is not flagged`() {
        // Both statements of 'Instruction' denote the same class, making a record deriving from the definition
        // assignable to a template argument typed with the declaration.
        addForwardDeclaredInstruction()
        doResolvingTest(
            """
            include "target.td"
            def BLR : Instruction;
            def D : CheckOpcode<[BLR]>;
        """.trimIndent()
        )
    }

    fun `test value typed with a forward declaration is not flagged`() {
        // Mirrors 'Predicate' of LLVM's 'Target.td': the field is typed while only the declaration is visible, the
        // template argument once the definition is. Both statements denote the same class.
        doResolvingTest(
            """
            class Predicate;
            class Instruction { list<Predicate> Predicates = []; }
            class Predicate { int x = 0; }
            class Requires<list<Predicate> preds>;
            def I : Instruction;
            def D : Requires<I.Predicates>;
        """.trimIndent()
        )
    }

    fun `test argument not deriving from a forward declared class is flagged`() {
        addForwardDeclaredInstruction()
        doResolvingTest(
            """
            include "target.td"
            def NotAnInstruction;
            def D : CheckOpcode<<error descr="Value of type 'list<NotAnInstruction>' cannot be assigned to template argument 'opcodes' of type 'list<Instruction>'">[NotAnInstruction]</error>>;
        """.trimIndent()
        )
    }

    fun `test a declaration shared by several compilations denotes the class of each`() {
        // 'predicate.td' takes part in two compilations that each define 'Instruction' themselves, as LLVM's
        // per-target files do. The declaration denotes the class of whichever definition it precedes.
        addForwardDeclaredInstruction()
        val otherTarget = myFixture.addFileToProject(
            "other_target.td", """
            include "predicate.td"
            class Instruction { int width = 0; }
            def RET : Instruction;
            def NotAnInstruction;
            def D : CheckOpcode<[RET]>;
            def E : CheckOpcode<<error descr="Value of type 'list<NotAnInstruction>' cannot be assigned to template argument 'opcodes' of type 'list<Instruction>'">[NotAnInstruction]</error>>;
        """.trimIndent()
        )
        val main = myFixture.configureByText(
            "test.td", """
            include "target.td"
            def BLR : Instruction;
            def NotAnInstruction;
            def D : CheckOpcode<[BLR]>;
            def E : CheckOpcode<<error descr="Value of type 'list<NotAnInstruction>' cannot be assigned to template argument 'opcodes' of type 'list<Instruction>'">[NotAnInstruction]</error>>;
        """.trimIndent()
        )
        val dir = main.virtualFile.parent
        installCompileCommands(
            project, mapOf(
                main.virtualFile to IncludePaths(listOf(dir)),
                otherTarget.virtualFile to IncludePaths(listOf(dir)),
            )
        )
        myFixture.checkHighlighting()

        myFixture.configureFromExistingVirtualFile(otherTarget.virtualFile)
        myFixture.checkHighlighting()
    }

    fun `test declarations of sibling files are the same class where both are pasted in`() {
        // Neither file includes the other, so from within either one only its own declaration of 'Base' is visible.
        // This file pastes both in, making them one and the same class, while 'other.td' is a higher-priority root
        // pasting in only 'derived.td', from whose context 'sink.td' is not visible at all.
        myFixture.addFileToProject(
            "sink.td", """
            class Base;
            class Sink<Base b>;
        """.trimIndent()
        )
        myFixture.addFileToProject(
            "derived.td", """
            class Base;
            def D : Base;
        """.trimIndent()
        )
        val other = myFixture.addFileToProject("other.td", "include \"derived.td\"")
        val main = myFixture.configureByText(
            "test.td", """
            include "sink.td"
            include "derived.td"
            def U : Sink<D>;
        """.trimIndent()
        )
        val dir = main.virtualFile.parent
        installCompileCommands(
            project, mapOf(
                other.virtualFile to IncludePaths(listOf(dir)),
                main.virtualFile to IncludePaths(listOf(dir)),
            )
        )
        myFixture.checkHighlighting()
    }

    fun `test base class resolving to a forward declaration is not flagged`() {
        // 'derived.td' only sees the forward declaration of 'Base', making 'Derived' derive from the declaration,
        // while 'Base' used as a type in this file resolves to the definition. Both denote the same class.
        myFixture.addFileToProject("decl.td", "class Base;")
        myFixture.addFileToProject(
            "derived.td", """
            include "decl.td"
            class Derived : Base;
        """.trimIndent()
        )
        myFixture.addFileToProject(
            "def.td", """
            include "decl.td"
            class Base { int x = 0; }
        """.trimIndent()
        )
        doResolvingTest(
            """
            include "derived.td"
            include "def.td"
            class Sink<Base b>;
            def D : Derived;
            def U : Sink<D>;
        """.trimIndent()
        )
    }

    fun `test base class resolving to a forward declaration of another class is flagged`() {
        // That 'Base' is defined later on does not make it any more related to 'Other'.
        doResolvingTest(
            """
            class Base;
            class Derived : Base;
            class Base { int x = 0; }
            class Other;
            class Other { int y = 0; }
            class Sink<Other o>;
            def D : Derived;
            def U : Sink<<error descr="Value of type 'D' cannot be assigned to template argument 'o' of type 'Other'">D</error>>;
        """.trimIndent()
        )
    }

    fun `test classes only ever declared are told apart`() {
        // 'Sink' only sees the first declaration of 'Base' while 'D' derives from the second one. Both denote the same
        // class despite there not being a definition to tell.
        doResolvingTest(
            """
            class Base;
            class Other;
            class Sink<Base b>;
            class Base;
            def D : Base;
            def O : Other;
            def U : Sink<D>;
            def V : Sink<<error descr="Value of type 'O' cannot be assigned to template argument 'b' of type 'Base'">O</error>>;
        """.trimIndent()
        )
    }

    fun `test list argument with a mismatching element type is flagged`() {
        doResolvingTest(
            """
            class C<list<int> a>;
            def D : C<<error descr="Value of type 'list<string>' cannot be assigned to template argument 'a' of type 'list<int>'">["a"]</error>>;
        """.trimIndent()
        )
    }

    fun `test empty list argument is not flagged`() {
        // An empty list has an unknown element type, so its convertibility is indeterminate.
        doResolvingTest(
            """
            class C<list<int> a>;
            def D : C<[]>;
        """.trimIndent()
        )
    }

    fun `test iteration variable of a foreach over a list has the element type`() {
        doResolvingTest(
            """
            class C<int x>;
            defvar v = !foreach(a, [5], C<a>);
        """.trimIndent()
        )
    }

    fun `test iteration variable of a foreach over a dag is a dag`() {
        doResolvingTest(
            """
            class C<int x>;
            def ins;
            defvar d = (ins 5);
            defvar v = !foreach(a, d, C<<error descr="Value of type 'dag' cannot be assigned to template argument 'x' of type 'int'">a</error>>);
        """.trimIndent()
        )
    }

    fun `test class defined twice`() {
        doResolvingTest(
            """
            class C { int a = 0; }
            class <error descr="Class 'C' is already defined">C</error> { int b = 0; }
        """.trimIndent()
        )
    }

    fun `test class declared before its definition`() {
        doResolvingTest(
            """
            class C;
            class C;
            class C<int a>;
        """.trimIndent()
        )
    }

    fun `test class declared after its definition`() {
        // TableGen only allows declaring a class up until it is defined.
        doResolvingTest(
            """
            class C<int a>;
            class <error descr="Class 'C' is already defined">C</error>;
        """.trimIndent()
        )
    }

    fun `test class defined by include before`() {
        myFixture.addFileToProject("c.td", "class C { int a = 0; }")
        doResolvingTest(
            """
            include "c.td"
            class <error descr="Class 'C' is already defined">C</error> { int b = 0; }
        """.trimIndent()
        )
    }

    fun `test class defined by include after`() {
        myFixture.addFileToProject("c.td", "class C;")
        doResolvingTest(
            """
            class C { int b = 0; }
            include "c.td"
        """.trimIndent()
        )
    }

    fun `test class defined by include between declaration and definition`() {
        // The declaration of the same file must not hide what the include defines.
        myFixture.addFileToProject("c.td", "class C { int a = 0; }")
        doResolvingTest(
            """
            class C;
            include "c.td"
            class <error descr="Class 'C' is already defined">C</error> { int b = 0; }
        """.trimIndent()
        )
    }

    fun `test class defined by file not included`() {
        myFixture.addFileToProject("c.td", "class C { int a = 0; }")
        doResolvingTest("class C { int b = 0; }")
    }

    fun `test navigates to the closest definition in the same file`() {
        myFixture.addFileToProject("c.td", "class C { int a = 0; }")
        doResolvingTest(
            """
            include "c.td"
            class <error descr="Class 'C' is already defined">C</error> { int b = 0; }
            class <error descr="Class 'C' is already defined">C</error> { int c = 0; }
        """.trimIndent()
        )
        assertNavigatesTo("test.td", "C { int b")
    }

    fun `test navigates to the closest definition in an include`() {
        myFixture.addFileToProject("c.td", "class C { int a = 0; }")
        doResolvingTest(
            """
            class C { int b = 0; }
            include "c.td"
            class <error descr="Class 'C' is already defined">C</error> { int c = 0; }
        """.trimIndent()
        )
        assertNavigatesTo("c.td", "C { int a")
    }

    fun `test navigates to the closest definition among includes`() {
        // 'c.td' begins being pasted in first but only defines the class after having pasted 'nested.td' in.
        myFixture.addFileToProject("nested.td", "class C { int nested = 0; }")
        myFixture.addFileToProject(
            "c.td", """
            include "nested.td"
            class C { int a = 0; }
        """.trimIndent()
        )
        doResolvingTest(
            """
            include "c.td"
            class <error descr="Class 'C' is already defined">C</error> { int c = 0; }
        """.trimIndent()
        )
        assertNavigatesTo("c.td", "C { int a")
    }

    fun `test navigates to the closest definition of an includer`() {
        // 'root.td' begins being pasted in first and defines the class before pasting 'c.td' in.
        myFixture.addFileToProject("c.td", "class C { int a = 0; }")
        val file = myFixture.configureByText("test.td", "class C { int c = 0; }")
        val root = myFixture.addFileToProject(
            "root.td", """
            class C { int root = 0; }
            include "c.td"
            include "test.td"
        """.trimIndent()
        )
        installCompileCommands(
            project, mapOf(root.virtualFile to IncludePaths(listOf(file.virtualFile.parent)))
        )
        assertNavigatesTo("c.td", "C { int a")
    }

    fun `test multiclass defined twice`() {
        doResolvingTest(
            """
            multiclass M { def a; }
            multiclass <error descr="Multiclass 'M' is already defined">M</error> { def b; }
        """.trimIndent()
        )
    }

    fun `test multiclass without a body is a definition`() {
        // Unlike a class, a multiclass cannot be declared ahead of its definition.
        doResolvingTest(
            """
            multiclass B { def a; }
            multiclass M : B;
            multiclass <error descr="Multiclass 'M' is already defined">M</error> : B;
        """.trimIndent()
        )
    }

    fun `test multiclass and class of the same name`() {
        doResolvingTest(
            """
            class C;
            multiclass C { def a; }
            class C { int a = 0; }
        """.trimIndent()
        )
    }

    fun `test multiclass defined by include before`() {
        myFixture.addFileToProject("m.td", "multiclass M { def a; }")
        doResolvingTest(
            """
            include "m.td"
            multiclass <error descr="Multiclass 'M' is already defined">M</error> { def b; }
        """.trimIndent()
        )
    }

    fun `test multiclass defined by include after`() {
        myFixture.addFileToProject("m.td", "multiclass M { def a; }")
        doResolvingTest(
            """
            multiclass M { def b; }
            include "m.td"
        """.trimIndent()
        )
    }

    fun `test multiclass defined by file not included`() {
        myFixture.addFileToProject("m.td", "multiclass M { def a; }")
        doResolvingTest("multiclass M { def b; }")
    }

    fun `test navigates to the closest multiclass definition in the same file`() {
        myFixture.addFileToProject("c.td", "multiclass C { def a; }")
        doResolvingTest(
            """
            include "c.td"
            multiclass <error descr="Multiclass 'C' is already defined">C</error> { def b; }
            multiclass <error descr="Multiclass 'C' is already defined">C</error> { def c; }
        """.trimIndent()
        )
        assertNavigatesTo("test.td", "C { def b", keyword = "multiclass")
    }

    fun `test navigates to the closest multiclass definition in an include`() {
        myFixture.addFileToProject("c.td", "multiclass C { def a; }")
        doResolvingTest(
            """
            multiclass C { def b; }
            include "c.td"
            multiclass <error descr="Multiclass 'C' is already defined">C</error> { def c; }
        """.trimIndent()
        )
        assertNavigatesTo("c.td", "C { def a", keyword = "multiclass")
    }

    /**
     * Asserts that the quick fix of the last redefinition of `C` by a [keyword] statement navigates to the occurrence of
     * [text] within [file].
     */
    private fun assertNavigatesTo(file: String, text: String, keyword: String = "class") {
        myFixture.editor.caretModel.moveToOffset(myFixture.file.text.lastIndexOf("$keyword C") + "$keyword ".length)
        myFixture.launchAction(myFixture.findSingleIntention("Navigate to previous definition"))

        val editor = FileEditorManager.getInstance(project).selectedTextEditor!!
        assertEquals(file, editor.virtualFile?.name)
        assertEquals(editor.document.text.indexOf(text), editor.caretModel.offset)
    }

    /**
     * Highlighting test that additionally installs compile commands so that class references resolve. [source] becomes
     * the sole compile-command root and may include any other file previously added to the fixture.
     */
    private fun doResolvingTest(source: String) {
        val file = myFixture.configureByText("test.td", source)
        installCompileCommands(
            project, mapOf(
                file.virtualFile to IncludePaths(listOf(file.virtualFile.parent))
            )
        )
        myFixture.checkHighlighting()
    }
}
