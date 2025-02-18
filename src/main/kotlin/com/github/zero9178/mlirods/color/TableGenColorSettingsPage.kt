package com.github.zero9178.mlirods.color

import com.github.zero9178.mlirods.MyIcons
import com.github.zero9178.mlirods.highlighting.TableGenSyntaxHighlighter
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighter
import com.intellij.openapi.options.colors.AttributesDescriptor
import com.intellij.openapi.options.colors.ColorDescriptor
import com.intellij.openapi.options.colors.ColorSettingsPage
import com.intellij.openapi.util.NlsContexts
import org.jetbrains.annotations.NonNls

private val DESCRIPTORS = arrayOf(
    AttributesDescriptor({ "Comments//Line comment" }, LINE_COMMENT),
    AttributesDescriptor({ "Comments//Block comment" }, BLOCK_COMMENT),
    AttributesDescriptor({ "Number" }, NUMBER),
    AttributesDescriptor({ "Braces and Operators//Braces" }, BRACES),
    AttributesDescriptor({ "Braces and Operators//Parentheses" }, PARENTHESES),
    AttributesDescriptor({ "Braces and Operators//Brackets" }, BRACKETS),
    AttributesDescriptor({ "Braces and Operators//Comma" }, COMMA),
    AttributesDescriptor({ "Braces and Operators//Semicolon" }, SEMICOLON),
    AttributesDescriptor({ "Braces and Operators//Dot" }, DOT),
    AttributesDescriptor({ "Braces and Operators//Operators" }, OPERATION_SIGN),
    AttributesDescriptor({ "Braces and Operators//Bang operators" }, BANG_OPERATOR),
    AttributesDescriptor({ "String//Escape sequence" }, STRING_ESCAPE),
    AttributesDescriptor({ "String//String text" }, STRING),
    AttributesDescriptor({ "Keyword" }, KEYWORD),
    AttributesDescriptor({ "Identifiers//Field" }, FIELD),
    AttributesDescriptor({ "Identifiers//Other" }, IDENTIFIER),
    AttributesDescriptor({ "Preprocessor//Directive" }, PREPROCESSOR_DIRECTIVE),
)

private class TableGenColorSettingsPage : ColorSettingsPage {
    override fun getIcon() = MyIcons.TableGenIcon

    override fun getHighlighter(): SyntaxHighlighter {
        return TableGenSyntaxHighlighter()
    }

    override fun getDemoText(): @NonNls String {
        return """
        // this a line comment
        /*
        this a block comment
        */
        class Foo<int i = 0, bit f = 0> {
            string s = "string literal" # " after operator";
            code c = [{
                code literal
            }];
        }
        
        #define FOO
        #ifndef FOO
            unreachable
        #endif
        
        defvar b = !empty(Foo<>.s);
        """.trimIndent()
    }

    override fun getAdditionalHighlightingTagToDescriptorMap(): Map<String?, TextAttributesKey?>? {
        return null
    }

    override fun getAttributeDescriptors() = DESCRIPTORS

    override fun getColorDescriptors(): Array<out ColorDescriptor?> {
        return ColorDescriptor.EMPTY_ARRAY
    }

    override fun getDisplayName(): @NlsContexts.ConfigurableName String {
        return "TableGen"
    }
}