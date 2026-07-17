package com.github.zero9178.mlirods


import com.github.zero9178.mlirods.language.generated.psi.TableGenDefvarStatement
import com.github.zero9178.mlirods.language.types.*
import com.intellij.psi.util.parentOfType
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class TypeComputationTest : BasePlatformTestCase() {
    fun `test list single slice`() {
        doTest(
            """
            defvar x = [5];
            defvar <caret>v = x[0];
        """.trimIndent(), TableGenIntType
        )
    }

    fun `test list multi slice`() {
        doTest(
            """
            defvar x = [5];
            defvar <caret>v = x[0, 1];
        """.trimIndent(), TableGenListType(TableGenIntType)
        )
    }

    fun `test field access`() {
        doTest(
            """
            class Foo {
                string i = "";
            }
            def Bar {
                Foo foo = Foo<>;
            }
            defvar <caret>v = Bar.foo.i;
        """.trimIndent(), TableGenStringType
        )
    }

    fun `test concat`() {
        doTest(
            """
            defvar <caret>v = [5] # [3];
        """.trimIndent(), TableGenListType(TableGenIntType)
        )
    }

    fun `test foreach iterable type`() {
        doTest(
            """
            defvar <caret>v = !foreach(a, [5], a);
        """.trimIndent(), TableGenListType(TableGenIntType)
        )
    }

    fun `test foreach result type`() {
        doTest(
            """
            defvar <caret>v = !foreach(a, [5], [5]);
        """.trimIndent(), TableGenListType(TableGenListType(TableGenIntType))
        )
    }

    fun `test foreach over dag`() {
        doTest(
            """
            defvar <caret>v = !foreach(a, (ins 5), "");
        """.trimIndent(), TableGenDagType
        )
    }

    fun `test foreach over dag ignores the body type`() {
        doTest(
            """
            defvar d = (ins 5);
            defvar <caret>v = !foreach(a, d, [a]);
        """.trimIndent(), TableGenDagType
        )
    }

    fun `test foldl`() {
        doTest(
            """
            defvar <caret>v = !foldl(0, [5], acc, i, i);
        """.trimIndent(), TableGenIntType
        )
    }

    fun `test sort`() {
        doTest(
            """
            defvar <caret>v = !sort(x, [5], x);
        """.trimIndent(), TableGenListType(TableGenIntType)
        )
    }

    fun `test filter`() {
        doTest(
            """
            defvar <caret>v = !filter(x, [5], x);
        """.trimIndent(), TableGenListType(TableGenIntType)
        )
    }

    fun `test cast`() {
        doTest(
            """
            defvar <caret>v = !cast<string>(5);
        """.trimIndent(), TableGenStringType
        )
    }

    fun `test bool`() {
        doTest(
            """
            defvar <caret>v = true;
        """.trimIndent(), TableGenIntType
        )
    }


    fun `test undef`() {
        doTest(
            """
            defvar <caret>v = ?;
        """.trimIndent(), TableGenUndefType
        )
    }


    fun `test bits init`() {
        doTest(
            """
            defvar <caret>v = {1, 0, 1};
        """.trimIndent(), TableGenBitsType(3)
        )
    }

    fun `test bits init with bits operand`() {
        doTest(
            """
            class Foo {
                bits<4> b;
            }
            def Bar : Foo;
            defvar <caret>v = {Bar.b, 1};
        """.trimIndent(), TableGenBitsType(5)
        )
    }

    fun `test bits init with unknown operand`() {
        doTest(
            """
            defvar <caret>v = {undefined, 1};
        """.trimIndent(), TableGenBitsType(null)
        )
    }

    fun `test bit access single bit`() {
        doTest(
            """
            defvar b = {1, 0, 1, 0};
            defvar <caret>v = b{2};
        """.trimIndent(), TableGenBitsType(1)
        )
    }

    fun `test bit access range`() {
        doTest(
            """
            defvar b = {1, 0, 1, 0};
            defvar <caret>v = b{3-0};
        """.trimIndent(), TableGenBitsType(4)
        )
    }

    fun `test bit access multiple pieces`() {
        doTest(
            """
            defvar b = {1, 0, 1, 0};
            defvar <caret>v = b{3...2, 0};
        """.trimIndent(), TableGenBitsType(3)
        )
    }

    /**
     * A single bit piece selects exactly one bit, regardless of whether the index itself can be determined.
     */
    fun `test bit access non constant single bit`() {
        doTest(
            """
            defvar b = {1, 0, 1, 0};
            defvar <caret>v = b{undefined};
        """.trimIndent(), TableGenBitsType(1)
        )
    }

    fun `test bit access non constant range`() {
        doTest(
            """
            defvar b = {1, 0, 1, 0};
            defvar <caret>v = b{undefined...0};
        """.trimIndent(), TableGenBitsType(null)
        )
    }

    /**
     * Range bounds need not be integer literals, they only have to fold to an integer. TableGen parses them with no
     * current record, so a global 'defvar' folds while template arguments and fields never do.
     */
    fun `test bit access range folds constant bounds`() {
        doTest(
            """
            defvar b = {1, 0, 1, 0};
            defvar i = 3;
            defvar <caret>v = b{i-0};
        """.trimIndent(), TableGenBitsType(4)
        )
    }

    fun `test cond`() {
        doTest(
            """
            defvar <caret>v = !cond(true: 1, false: 2);
        """.trimIndent(), TableGenIntType
        )
    }

    fun `test cond resolves to common type`() {
        doTest(
            """
            defvar <caret>v = !cond(true: [1], false: []);
        """.trimIndent(), TableGenListType(TableGenIntType)
        )
    }

    fun `test cond ignores undef`() {
        doTest(
            """
            defvar <caret>v = !cond(true: ?, false: "a");
        """.trimIndent(), TableGenStringType
        )
    }

    fun `test switch`() {
        doTest(
            """
            defvar <caret>v = !switch(1, 1: "a", "b");
        """.trimIndent(), TableGenStringType
        )
    }

    fun `test switch default only`() {
        doTest(
            """
            defvar <caret>v = !switch(1, 1: 2, 3);
        """.trimIndent(), TableGenIntType
        )
    }

    fun `test bang operator comparison yields bit`() {
        doTest(
            """
            defvar <caret>v = !eq(1, 2);
        """.trimIndent(), TableGenBitType
        )
    }

    fun `test bang operator arithmetic`() {
        doTest(
            """
            defvar <caret>v = !add(1, 2, 3);
        """.trimIndent(), TableGenIntType
        )
    }

    fun `test bang operator strconcat`() {
        doTest(
            """
            defvar <caret>v = !strconcat("a", "b");
        """.trimIndent(), TableGenStringType
        )
    }

    fun `test bang operator size`() {
        doTest(
            """
            defvar <caret>v = !size([1, 2]);
        """.trimIndent(), TableGenIntType
        )
    }

    fun `test bang operator head`() {
        doTest(
            """
            defvar <caret>v = !head([[1]]);
        """.trimIndent(), TableGenListType(TableGenIntType)
        )
    }

    fun `test bang operator tail`() {
        doTest(
            """
            defvar <caret>v = !tail([1]);
        """.trimIndent(), TableGenListType(TableGenIntType)
        )
    }

    fun `test bang operator listsplat`() {
        doTest(
            """
            defvar <caret>v = !listsplat("a", 3);
        """.trimIndent(), TableGenListType(TableGenStringType)
        )
    }

    fun `test bang operator listflatten`() {
        doTest(
            """
            defvar <caret>v = !listflatten([[1], [2]]);
        """.trimIndent(), TableGenListType(TableGenIntType)
        )
    }

    fun `test bang operator listflatten of non lists`() {
        doTest(
            """
            defvar <caret>v = !listflatten([1, 2]);
        """.trimIndent(), TableGenListType(TableGenIntType)
        )
    }

    fun `test bang operator listconcat`() {
        doTest(
            """
            defvar <caret>v = !listconcat([1], []);
        """.trimIndent(), TableGenListType(TableGenIntType)
        )
    }

    fun `test bang operator range`() {
        doTest(
            """
            defvar <caret>v = !range(4);
        """.trimIndent(), TableGenListType(TableGenIntType)
        )
    }

    fun `test bang operator if`() {
        doTest(
            """
            defvar <caret>v = !if(true, "a", "b");
        """.trimIndent(), TableGenStringType
        )
    }

    fun `test bang operator if resolves to common type`() {
        doTest(
            """
            defvar <caret>v = !if(true, [1], []);
        """.trimIndent(), TableGenListType(TableGenIntType)
        )
    }

    fun `test bang operator subst`() {
        doTest(
            """
            defvar <caret>v = !subst("a", "b", "c");
        """.trimIndent(), TableGenStringType
        )
    }

    fun `test bang operator con yields dag`() {
        doTest(
            """
            def op;
            defvar <caret>v = !con((op 1), (op 2));
        """.trimIndent(), TableGenDagType
        )
    }

    fun `test bang operator getdagarg uses type argument`() {
        doTest(
            """
            def op;
            defvar <caret>v = !getdagarg<int>((op 1), 0);
        """.trimIndent(), TableGenIntType
        )
    }

    fun `test bang operator isa yields int`() {
        doTest(
            """
            class Foo;
            defvar <caret>v = !isa<Foo>(1);
        """.trimIndent(), TableGenIntType
        )
    }

    fun `test bang operator instances yields list of type argument`() {
        doTestString(
            """
            class Foo;
            defvar <caret>v = !instances<Foo>();
        """.trimIndent(), "list<Foo>"
        )
    }

    fun `test bang operator getdagop without type argument`() {
        doTest(
            """
            def op;
            defvar <caret>v = !getdagop((op 1));
        """.trimIndent(), TableGenUnknownType
        )
    }

    fun `test unknown bang operator`() {
        doTest(
            """
            defvar <caret>v = !nonsense(1);
        """.trimIndent(), TableGenUnknownType
        )
    }

    private fun typeAtCaret(source: String): TableGenType? {
        myFixture.configureByText("test.td", source)
        val statement = requireNotNull(myFixture.elementAtCaret.parentOfType<TableGenDefvarStatement>(withSelf = true))
        return statement.valueNode?.type
    }

    fun doTest(source: String, expectedType: TableGenType) {
        assertEquals(expectedType, typeAtCaret(source))
    }

    /**
     * Variant of [doTest] comparing the rendered type, for types such as record types which have no value equality.
     */
    fun doTestString(source: String, expectedType: String) {
        assertEquals(expectedType, typeAtCaret(source).toString())
    }
}
