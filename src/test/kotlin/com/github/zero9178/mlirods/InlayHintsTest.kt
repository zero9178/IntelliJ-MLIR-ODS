package com.github.zero9178.mlirods

import com.github.zero9178.mlirods.language.TableGenTypeInlayHintsProvider
import com.intellij.testFramework.utils.inlays.declarative.DeclarativeInlayHintsProviderTestCase

class InlayHintsTest : DeclarativeInlayHintsProviderTestCase() {

    /**
     * Checks that the hints in [code], written as `/*<# hint #>*/` at their position, are exactly the ones shown.
     */
    private fun doTest(code: String) =
        doTestProvider("test.td", code.trimIndent(), TableGenTypeInlayHintsProvider())

    fun `test defvar`() = doTest(
        """
        class Op;
        class Dialect<list<Op> operations> {
          defvar /*<# list<|Op|> #>*/ops = operations;
          defvar /*<# Op #>*/first = ops[0];
        }
        """
    )

    fun `test defvar defined as literal or class instantiation`() = doTest(
        """
        class Op<string n> {
          string name = n;
        }
        defvar i = 42;
        defvar b = 0b101;
        defvar t = true;
        defvar s = "str";
        defvar o = Op<"add">;
        defvar /*<# string #>*/name = o.name;
        """
    )

    fun `test unknown type is not shown`() = doTest(
        """
        class Op;
        def op : Op;
        defvar d = !getdagop((op));
        """
    )

    fun `test foreach iterator`() = doTest(
        """
        defvar /*<# list<string> #>*/names = ["a", "b"];
        foreach /*<# string #>*/n = names in
          def n;
        foreach i = 0-3 in
          def "r" # i;
        foreach j = {0-3, 5} in
          def "s" # j;
        foreach /*<# string #>*/k = ["x", "y"] in
          def k;
        """
    )

    fun `test bang operator variables`() = doTest(
        """
        defvar /*<# list<string> #>*/names = ["a", "b"];
        defvar /*<# list<string> #>*/upper = !foreach(/*<# string #>*/n, names, !toupper(n));
        defvar /*<# int #>*/total = !foldl(0, [1, 2], /*<# int #>*/acc, /*<# int #>*/i, !add(acc, i));
        defvar /*<# list<string> #>*/sorted = !sort(/*<# string #>*/s, names, s);
        """
    )

    fun `test preview`() = doTestProvider(
        "preview.td",
        javaClass.getResource("/inlayProviders/tableGen.type.hints/preview.td")!!.readText(),
        TableGenTypeInlayHintsProvider()
    )
}
