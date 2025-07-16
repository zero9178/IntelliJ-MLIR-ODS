package com.github.zero9178.mlirods.language.generated

import com.intellij.lang.PsiBuilder
import com.intellij.lang.parser.GeneratedParserUtilBase

class TableGenParserUtil {
    companion object {

        /**
         * External parsing rule implementing the subset of 'value' that object names support.
         */
        @JvmStatic
        fun objectName(builder: PsiBuilder, level: Int): Boolean {
            if (!GeneratedParserUtilBase.recursion_guard_(builder, level, "objectName")) return false

            GeneratedParserUtilBase.addVariant(builder, "<value>")
            var result: Boolean
            val marker =
                GeneratedParserUtilBase.enter_section_(builder, level, GeneratedParserUtilBase._NONE_, "<value>")
            result = TableGenParser.integer_value(builder, level + 1)
            if (!result) result = TableGenParser.string_value(builder, level + 1)
            if (!result) result = TableGenParser.bool_value(builder, level + 1)
            if (!result) result = TableGenParser.undef_value(builder, level + 1)
            if (!result) result = TableGenParser.list_init_value(builder, level + 1)
            if (!result) result = TableGenParser.dag_init_value(builder, level + 1)
            if (!result) result = TableGenParser.class_instantiation_value(builder, level + 1)
            if (!result) result = TableGenParser.identifier_value(builder, level + 1)
            if (!result) result = TableGenParser.cond_operator_value(builder, level + 1)
            if (!result) result = TableGenParser.foreach_operator_value(builder, level + 1)
            if (!result) result = TableGenParser.foldl_operator_value(builder, level + 1)
            if (!result) result = TableGenParser.bang_operator_value(builder, level + 1)
            val pinned = result

            result = result && objectNamePostfix(builder, level + 1)

            GeneratedParserUtilBase.exit_section_(builder, level, marker, null, result, pinned, null)
            return result || pinned
        }

        private fun objectNamePostfix(builder: PsiBuilder, level: Int): Boolean {
            if (!GeneratedParserUtilBase.recursion_guard_(builder, level, "objectNamePostfix")) return false

            while (true) {
                val marker =
                    GeneratedParserUtilBase.enter_section_(builder, level, GeneratedParserUtilBase._LEFT_, null)
                if (GeneratedParserUtilBase.nextTokenIs(builder, TableGenTypes.HASHTAG)) {
                    run {
                        val marker =
                            GeneratedParserUtilBase.enter_section_(builder, level, GeneratedParserUtilBase._NONE_)
                        val result = GeneratedParserUtilBase.consumeToken(builder, TableGenTypes.HASHTAG)

                        objectName(builder, level + 1)

                        GeneratedParserUtilBase.exit_section_(builder, level, marker, result, result, null)
                    }
                    GeneratedParserUtilBase.exit_section_(
                        builder, level, marker, TableGenTypes.CONCAT_VALUE, true, true, null
                    )
                } else if (TableGenParser.field_access_value_suffix(builder, level + 1)) {
                    GeneratedParserUtilBase.exit_section_(
                        builder, level, marker, TableGenTypes.FIELD_ACCESS_VALUE, true, true, null
                    )
                } else if (TableGenParser.slice_access_value_suffix(builder, level + 1)) {
                    GeneratedParserUtilBase.exit_section_(
                        builder, level, marker, TableGenTypes.SLICE_ACCESS_VALUE, true, true, null
                    )
                } else {
                    GeneratedParserUtilBase.exit_section_(builder, level, marker, null, false, false, null)
                    break
                }
            }
            return true
        }
    }
}