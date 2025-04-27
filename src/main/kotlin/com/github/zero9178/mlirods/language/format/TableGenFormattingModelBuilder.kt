package com.github.zero9178.mlirods.language.format

import com.github.zero9178.mlirods.language.BANG_OPERATORS
import com.github.zero9178.mlirods.language.TableGenLanguage
import com.github.zero9178.mlirods.language.generated.TableGenTypes
import com.intellij.formatting.*
import com.intellij.psi.codeStyle.CodeStyleSettings


class TableGenFormattingModelBuilder : FormattingModelBuilder {

    private fun createSpacingBuilder(codeStyleSettings: CodeStyleSettings): SpacingBuilder {
        return SpacingBuilder(codeStyleSettings, TableGenLanguage.INSTANCE).apply {
            after(BANG_OPERATORS).none()
            around(TableGenTypes.LANGLE).none()
            before(TableGenTypes.RANGLE).none()

            after(TableGenTypes.LPAREN).none()
            before(TableGenTypes.RPAREN).none()
            after(TableGenTypes.LBRACKET).none()
            before(TableGenTypes.RBRACKET).none()
            beforeInside(TableGenTypes.LBRACKET, TableGenTypes.SLICE_ACCESS_VALUE).none()

            aroundInside(TableGenTypes.ELLIPSE, TableGenTypes.BIT_RANGE).none()
            aroundInside(TableGenTypes.ELLIPSE, TableGenTypes.SLICE_ELEMENT_RANGE).none()

            after(TableGenTypes.LBRACE).none()
            before(TableGenTypes.RBRACE).none()
            beforeInside(TableGenTypes.LBRACE, TableGenTypes.CLASS_STATEMENT).spaces(1)
            beforeInside(TableGenTypes.LBRACE, TableGenTypes.DEF_STATEMENT).spaces(1)
            beforeInside(TableGenTypes.LBRACE, TableGenTypes.MULTICLASS_STATEMENT).spaces(1)
            beforeInside(TableGenTypes.LBRACE, TableGenTypes.LET_BODY_ITEM).none()
            beforeInside(TableGenTypes.LBRACE, TableGenTypes.BIT_ACCESS_VALUE).none()

            after(TableGenTypes.COMMA).spaces(1)
            before(TableGenTypes.COMMA).none()
            before(TableGenTypes.SEMICOLON).none()
            after(TableGenTypes.INCLUDE).spaces(1)
            after(TableGenTypes.ASSERT).spaces(1)
            aroundInside(TableGenTypes.DOT, TableGenTypes.FIELD_ACCESS_VALUE).none()

            aroundInside(TableGenTypes.COLON, TableGenTypes.DAG_ARG).none()
            around(TableGenTypes.COLON).spaces(1)
            around(TableGenTypes.EQUALS).spaces(1)
            around(TableGenTypes.HASHTAG).spaces(1)
            around(TableGenTypes.ELSE).spaces(1)
            around(TableGenTypes.THEN).spaces(1)
            around(TableGenTypes.IN).spaces(1)
            after(TableGenTypes.IF).spaces(1)
        }
    }

    override fun createModel(formattingContext: FormattingContext): FormattingModel {
        val codeStyleSettings = formattingContext.codeStyleSettings
        return FormattingModelProvider
            .createFormattingModelForPsiFile(
                formattingContext.containingFile,
                TableGenBlock(
                    formattingContext.node,
                    createSpacingBuilder(codeStyleSettings),
                    Wrap.createWrap(WrapType.NONE, false),
                    Alignment.createAlignment(),
                ),
                codeStyleSettings
            )
    }
}