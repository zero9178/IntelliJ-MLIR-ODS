package com.github.zero9178.mlirods


import com.github.zero9178.mlirods.language.generated.psi.TableGenDefvarStatement
import com.github.zero9178.mlirods.language.psi.TableGenFile
import com.github.zero9178.mlirods.language.psi.impl.TableGenEvaluationContext
import com.github.zero9178.mlirods.language.types.TableGenIntType
import com.github.zero9178.mlirods.language.types.TableGenStringType
import com.github.zero9178.mlirods.language.values.*
import com.github.zero9178.mlirods.model.IncludePaths
import com.github.zero9178.mlirods.model.TableGenIncludeGraphService
import com.github.zero9178.mlirods.model.TableGenCompilationContext
import com.intellij.openapi.components.service
import com.intellij.psi.PsiManager
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.assertInstanceOf
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class ValueComputationTest : BasePlatformTestCase() {

    fun `test integer`() = doTest(
        """
        defvar v = 10;
    """.trimIndent(), TableGenIntegerValue(10L)
    )

    fun `test integer hex`() = doTest(
        """
        defvar v = 0x10;
    """.trimIndent(), TableGenIntegerValue(0x10L)
    )

    // A binary literal is a 'bits<n>' rather than an integer, and bits values are not modelled yet. Yielding an unknown
    // value keeps the evaluation honest until they are; folding it to an integer would claim the wrong type.
    fun `test integer binary is not folded`() = doTest(
        """
        defvar v = 0b10;
    """.trimIndent(), TableGenUnknownValue
    )

    fun `test integer huge hex`() = doTest(
        """
        defvar v = 0xFFFFFFFFFFFFFFFF;
    """.trimIndent(), TableGenIntegerValue(-1L)
    )

    fun `test integer huge decimal`() = doTest(
        """
        defvar v = 18446744073709551615;
    """.trimIndent(), TableGenIntegerValue(-1L)
    )

    fun `test integer huge negative decimal`() = doTest(
        """
        defvar v = -9223372036854775808;
    """.trimIndent(), TableGenIntegerValue(-9223372036854775807L - 1)
    )

    fun `test integer too large`() = doTest(
        """
        defvar v = 18446744073709551616;
    """.trimIndent(), TableGenUnknownValue
    )

    fun `test string literal`() = doTest(
        """
            defvar v = "a multi token"" string"" literal";
        """.trimIndent(),
        TableGenStringValue(
            "a multi token string literal"
        )
    )

    fun `test block string literal`() = doTest(
        """
            defvar v = [{ \n }];
        """.trimIndent(),
        TableGenStringValue(" \\n ")
    )

    fun `test string escapes`() = doTest(
        """
            defvar v = "\n\t\\n\"\'";
        """.trimIndent(),
        TableGenStringValue("\n\t\\n\"\'")
    )

    fun `test bool true`() = doTest(
        """
        defvar v = true;
    """.trimIndent(), TableGenIntegerValue(1)
    )

    fun `test bool false`() = doTest(
        """
        defvar v = false;
    """.trimIndent(), TableGenIntegerValue(0)
    )


    fun `test undef`() = doTest(
        """
        defvar v = ?;
    """.trimIndent(), TableGenUndefValue
    )

    fun `test identifier referencing defvar`() = doTest(
        """
        defvar a = 5;
        defvar v = a;
    """.trimIndent(), TableGenIntegerValue(5)
    )

    fun `test identifier referencing def yields record value`() = doTest(
        """
        def Foo;
        defvar v = Foo;
    """.trimIndent()
    ) {
        assertInstanceOf<TableGenRecordValue>(it)
    }

    fun `test record field evaluated with template argument`() = doTest(
        """
        class C<int x> {
            int y = x;
        }
        def D : C<5>;
        defvar v = D.y;
    """.trimIndent(), TableGenIntegerValue(5)
    )

    fun `test record field evaluated with named template argument`() = doTest(
        """
        class C<int x, int y> {
            int z = y;
        }
        def D : C<x = 1, y = 2>;
        defvar v = D.z;
    """.trimIndent(), TableGenIntegerValue(2)
    )

    fun `test record field referencing another field`() = doTest(
        """
        class C {
            int a = 5;
            int b = a;
        }
        def D : C;
        defvar v = D.b;
    """.trimIndent(), TableGenIntegerValue(5)
    )

    fun `test record field inherited from base class`() = doTest(
        """
        class Base {
            int a = 7;
        }
        class Derived : Base {
            int b = a;
        }
        def D : Derived;
        defvar v = D.b;
    """.trimIndent(), TableGenIntegerValue(7)
    )

    fun `test record field overridden by let in def`() = doTest(
        """
        class C {
            int y = 1;
        }
        def D : C {
            let y = 2;
        }
        defvar v = D.y;
    """.trimIndent(), TableGenIntegerValue(2)
    )

    fun `test field declared without value is undef`() = doTest(
        """
        class C {
            int x;
        }
        def D : C;
        defvar v = D.x;
    """.trimIndent(), TableGenUndefValue
    )

    // Declaring a field again replaces the value it had so far, even without giving it a new one.
    fun `test field redefined without value is undef`() = doTest(
        """
        class C {
            int x = 1;
        }
        def D : C {
            int x;
        }
        defvar v = D.x;
    """.trimIndent(), TableGenUndefValue
    )

    fun `test record template arg extra indirection`() = doTest(
        """
        class C<int x> {
            int y = x;
        }
        class D<int x> : C<x>;
        def E : D<3>;
        defvar v = E.y;
    """.trimIndent(), TableGenIntegerValue(3)
    )

    fun `test record field evaluated with default template argument`() = doTest(
        """
        class C<int x = 5> {
            int y = x;
        }
        def D : C;
        defvar v = D.y;
    """.trimIndent(), TableGenIntegerValue(5)
    )

    fun `test default template argument overridden by argument`() = doTest(
        """
        class C<int x, int y = x> {
            int z = y;
        }
        def D : C<1, 2>;
        defvar v = D.z;
    """.trimIndent(), TableGenIntegerValue(2)
    )

    // A default may refer to the template arguments of its class declared before it, which are bound by the same class
    // reference.
    fun `test default template argument referring to template argument`() = doTest(
        """
        class C<int x, int y = x> {
            int z = y;
        }
        def D : C<4>;
        defvar v = D.z;
    """.trimIndent(), TableGenIntegerValue(4)
    )

    fun `test default template argument of base class`() = doTest(
        """
        class Base<int x, int y = x> {
            int z = y;
        }
        class Derived<int w> : Base<w>;
        def D : Derived<3>;
        defvar v = D.z;
    """.trimIndent(), TableGenIntegerValue(3)
    )

    // Name lookup within the default happens in its class, not where the default is used.
    fun `test default template argument referring to defvar shadowed by deriving class`() = doTest(
        """
        defvar x = 1;
        class C<int y = x> {
            int z = y;
        }
        class Derived<int x> : C;
        def D : Derived<2>;
        defvar v = D.z;
    """.trimIndent(), TableGenIntegerValue(1)
    )

    fun `test field access on record`() = doTest(
        """
        class C {
            int a = 5;
        }
        def D : C;
        defvar v = D.a;
    """.trimIndent(), TableGenIntegerValue(5)
    )

    fun `test chained field access`() = doTest(
        """
        class Inner {
            int a = 9;
        }
        def I : Inner;
        class Outer {
            Inner inner = I;
        }
        def O : Outer;
        defvar v = O.inner.a;
    """.trimIndent(), TableGenIntegerValue(9)
    )

    fun `test class instantiation yields record value`() = doTest(
        """
        class C;
        defvar v = C<>;
    """.trimIndent()
    ) {
        assertEquals("C", assertInstanceOf<TableGenRecordValue>(it).type.toString())
    }

    fun `test field access on class instantiation`() = doTest(
        """
        class C {
            int a = 5;
        }
        defvar v = C<>.a;
    """.trimIndent(), TableGenIntegerValue(5)
    )

    fun `test class instantiation field evaluated with template argument`() = doTest(
        """
        class C<int x> {
            int y = x;
        }
        defvar v = C<3>.y;
    """.trimIndent(), TableGenIntegerValue(3)
    )

    fun `test class instantiation field evaluated with template argument of base class`() = doTest(
        """
        class Base<int x> {
            int y = x;
        }
        class C<int z> : Base<z>;
        defvar v = C<4>.y;
    """.trimIndent(), TableGenIntegerValue(4)
    )

    fun `test class instantiation field overridden by let`() = doTest(
        """
        class Base {
            int a = 1;
        }
        class C : Base {
            let a = 2;
        }
        defvar v = C<>.a;
    """.trimIndent(), TableGenIntegerValue(2)
    )

    fun `test class instantiation in def field`() = doTest(
        """
        class Inner<int x> {
            int a = x;
        }
        def D {
            Inner inner = Inner<6>;
        }
        defvar v = D.inner.a;
    """.trimIndent(), TableGenIntegerValue(6)
    )

    // The argument of the instantiation is evaluated where the instantiation is written, i.e. in the context of the
    // 'def' deriving from the class containing it. Each 'def' yields a record of its own.
    fun `test class instantiation argument evaluated in outer context`() = doTest(
        """
        class Inner<int x> {
            int a = x;
        }
        class Outer<int y> {
            Inner inner = Inner<y>;
        }
        def D1 : Outer<1>;
        def D2 : Outer<2>;
        defvar v1 = D1.inner.a;
        defvar v = D2.inner.a;
    """.trimIndent()
    ) {
        assertEquals(TableGenIntegerValue(2), it)
        val v1 = PsiTreeUtil.findChildrenOfType(myFixture.file, TableGenDefvarStatement::class.java).first()
        val context = TableGenEvaluationContext(TableGenCompilationContext.activeFor(v1))
        assertEquals(TableGenIntegerValue(1), v1.valueNode?.evaluateBlocking(context))
    }

    fun `test class instantiation field evaluated with default template argument`() = doTest(
        """
        class C<int x, int y = x> {
            int z = y;
        }
        defvar v = C<3>.z;
    """.trimIndent(), TableGenIntegerValue(3)
    )

    fun `test class instantiation field evaluated with default template argument of base class`() = doTest(
        """
        class Base<int x, int y = x> {
            int z = y;
        }
        class C<int w> : Base<w>;
        defvar v = C<4>.z;
    """.trimIndent(), TableGenIntegerValue(4)
    )

    // Unlike the arguments, the defaults of an instantiation are values of the instantiated class. Evaluated where the
    // instantiation is written, 'x' would be the one of 'D' instead.
    fun `test class instantiation default template argument evaluated in instantiated class`() = doTest(
        """
        class C<int x, int y = x> {
            int z = y;
        }
        def D : C<1> {
            C inner = C<2>;
        }
        defvar v = D.inner.z;
    """.trimIndent(), TableGenIntegerValue(2)
    )

    fun `test default template argument instantiating class`() = doTest(
        """
        class Inner<int x> {
            int y = x;
        }
        class C<int x, Inner i = Inner<x>> {
            int z = i.y;
        }
        def D : C<5>;
        defvar v = D.z;
    """.trimIndent(), TableGenIntegerValue(5)
    )

    fun `test list init`() = doTest(
        """
        defvar v = [1, 2];
    """.trimIndent(), TableGenListValue(listOf(TableGenIntegerValue(1), TableGenIntegerValue(2)), TableGenIntType)
    )

    fun `test list init with explicit element type`() = doTest(
        """
        defvar v = []<string>;
    """.trimIndent(), TableGenListValue(emptyList(), TableGenStringType)
    )

    fun `test list init with undef element`() = doTest(
        """
        defvar v = [?, 1];
    """.trimIndent(), TableGenListValue(listOf(TableGenUndefValue, TableGenIntegerValue(1)), TableGenIntType)
    )

    fun `test list init keeps unknown elements`() = doTest(
        """
        defvar v = [0b10, 1];
    """.trimIndent(), TableGenListValue(listOf(TableGenUnknownValue, TableGenIntegerValue(1)), TableGenIntType)
    )

    fun `test list init yields common element type`() = doTest(
        """
        class Foo;
        class Derived : Foo;
        defvar v = [Derived<>, Derived<>];
    """.trimIndent()
    ) {
        assertEquals("list<Derived>", it.type.toString())
    }

    fun `test list init element evaluated with template argument`() = doTest(
        """
        class C<int x> {
            list<int> l = [x, 2];
        }
        def D : C<1>;
        defvar v = D.l;
    """.trimIndent(), TableGenListValue(listOf(TableGenIntegerValue(1), TableGenIntegerValue(2)), TableGenIntType)
    )

    fun `test cyclic field reference within record`() {
        doTest(
            """
            def A {
                int f = 0;
                int g = f;
                let f = g;
            }
            defvar v = A.f;
        """.trimIndent(), TableGenUnknownValue
        )
    }

    fun `test cyclic field access across records`() = doTest(
        """
        def A {
            int f = B.g;
        }
        def B {
            int g = A.f;
        }
        defvar v = A.f;
    """.trimIndent(), TableGenUnknownValue
    )

    // Neither field can be evaluated without the other. Values on a cycle are unknown rather than recursing forever.
    fun `test fields referencing each other are unknown`() = doTest(
        """
        class C {
            int a = b;
            int b = a;
        }
        def D : C;
        defvar v = D.a;
    """.trimIndent(), TableGenUnknownValue
    )

    fun `test field referencing itself is unknown`() = doTest(
        """
        class C {
            int a = 1;
        }
        def D : C {
            let a = a;
        }
        defvar v = D.a;
    """.trimIndent(), TableGenUnknownValue
    )

    // The name of the argument to 'C' can only be evaluated knowing which template arguments the base classes of 'E'
    // bind, which is what that very argument is part of. TableGen rejects this as 'E' has no fields yet.
    fun `test argument name instantiating the class being defined is unknown`() = doTest(
        """
        class C<string n = "d"> { string f = n; }
        class E<string m> : C<E<"q">.f = "x">;
        def D : E<"z">;
        defvar v = D.f;
    """.trimIndent(), TableGenUnknownValue
    )

    fun `test record field evaluates within the root asked for`() {
        // A file pasted in by two roots derives from a different 'A' in each of them, so the same field has a
        // different value per root.
        myFixture.createFile("a.td", "class A { int x = 1; }")
        myFixture.createFile("b.td", "class A { int x = 2; }")
        val shared = myFixture.createFile("shared.td", "def D : A;\ndefvar v = D.x;")
        val rootA = myFixture.createFile("rootA.td", "include \"a.td\"\ninclude \"shared.td\"")
        val rootB = myFixture.createFile("rootB.td", "include \"b.td\"\ninclude \"shared.td\"")
        val paths = IncludePaths(listOf(shared.parent))
        compileCommandsUpdater(project)(mapOf(rootA to paths, rootB to paths))

        val file = assertInstanceOf<TableGenFile>(PsiManager.getInstance(project).findFile(shared))
        val statement = assertInstanceOf<TableGenDefvarStatement>(file.lastChild)
        val service = project.service<TableGenIncludeGraphService>()
        fun valueIn(context: TableGenCompilationContext) =
            statement.valueNode?.evaluateBlocking(TableGenEvaluationContext(context))

        assertEquals(TableGenIntegerValue(1), valueIn(service.compilationContextOf(rootA)))
        assertEquals(TableGenIntegerValue(2), valueIn(service.compilationContextOf(rootB)))
        // The context the file derives from the include graph is that of the first root reaching it.
        assertEquals(TableGenIntegerValue(1), valueIn(TableGenCompilationContext.activeFor(statement)))
    }

    fun doTest(source: String, expectedValue: TableGenValue) = doTest(source) {
        assertEquals(expectedValue, it)
    }

    fun doTest(source: String, expectedCondition: (TableGenValue) -> Unit) {
        val file = assertInstanceOf<TableGenFile>(myFixture.configureByText("test.td", source))
        val statement = assertInstanceOf<TableGenDefvarStatement>(file.lastChild)
        val context = TableGenEvaluationContext(TableGenCompilationContext.activeFor(file))
        expectedCondition.invoke(requireNotNull(statement.valueNode?.evaluateBlocking(context)))
    }
}
