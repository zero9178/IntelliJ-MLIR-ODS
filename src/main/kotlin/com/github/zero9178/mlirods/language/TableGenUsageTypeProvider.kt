package com.github.zero9178.mlirods.language

import com.github.zero9178.mlirods.MyBundle
import com.github.zero9178.mlirods.language.generated.psi.*
import com.intellij.psi.PsiElement
import com.intellij.psi.util.parentOfTypes
import com.intellij.usages.impl.rules.UsageType
import com.intellij.usages.impl.rules.UsageTypeProvider

private val CLASS_REF = UsageType(MyBundle.messagePointer("tableGen.class.ref.usage.type"))
private val CLASS_INSTANTIATION = UsageType(MyBundle.messagePointer("tableGen.class.instantiation.usage.type"))
private val CLASS_TEMPLATE_ARG_TYPE = UsageType(MyBundle.messagePointer("tableGen.template.arg"))
private val FIELD_DEFINITION_TYPE = UsageType(MyBundle.messagePointer("tableGen.field.definition"))
private val INCLUDE_DIRECTIVE = UsageType(MyBundle.messagePointer("tableGen.include.directive"))

private class TableGenUsageTypeProvider : UsageTypeProvider {
    override fun getUsageType(element: PsiElement): UsageType? {
        return when (element) {
            is TableGenClassRef -> CLASS_REF
            is TableGenClassInstantiationValue -> CLASS_INSTANTIATION
            is TableGenClassTypeNode -> {
                when (element.parentOfTypes(TableGenTemplateArgDecl::class, TableGenFieldBodyItem::class)) {
                    is TableGenTemplateArgDecl -> CLASS_TEMPLATE_ARG_TYPE
                    is TableGenFieldBodyItem -> FIELD_DEFINITION_TYPE
                    else -> null
                }
            }
            is TableGenIncludeDirective -> INCLUDE_DIRECTIVE

            is TableGenIdentifierValue, is TableGenFieldAccessValue -> UsageType.READ
            is TableGenLetBodyItem, is TableGenLetStatement -> UsageType.WRITE
            else -> null
        }
    }
}