package com.github.zero9178.mlirods.language.psi

import com.github.zero9178.mlirods.language.TableGenLanguage
import com.github.zero9178.mlirods.language.generated.psi.TableGenDefvarStatement
import com.github.zero9178.mlirods.language.generated.psi.TableGenIdentifierValue
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.util.PsiTreeUtil

/**
 * Returns a [TableGenFile] from the given text.
 */
fun createFile(project: Project, text: String): TableGenFile {
    return PsiFileFactory.getInstance(project)
        .createFileFromText("dummy.td", TableGenLanguage.INSTANCE, text) as TableGenFile
}

/**
 * Returns a [PsiElement] for a line string literal that has [text] as value.
 */
fun createLineStringLiteral(project: Project, text: String): PsiElement {
    // TODO: Encode string.
    return createEncodedLineStringLiteral(project, text)
}

/**
 * Returns a [PsiElement] for a line string literal that has [text] as lexical representation.
 */
fun createEncodedLineStringLiteral(project: Project, encodedText: String): PsiElement {
    val file = createFile(project, "include \"$encodedText\"")
    return file.firstChild.lastChild
}

/**
 * Returns an [TableGenIdentifierValue] using [name] as value.
 */
fun createIdentifierValue(project: Project, name: String): TableGenIdentifierValue {
    return PsiTreeUtil.findChildOfAnyType(
        createFile(project, "defvar $name = $name;"),
        TableGenIdentifierValue::class.java
    )!!
}

/**
 * Returns an identifier token with [name] as identifier.
 */
fun createIdentifier(project: Project, name: String) =
    (createFile(project, "defvar $name = $name;").firstChild as TableGenDefvarStatement).nameIdentifier!!
