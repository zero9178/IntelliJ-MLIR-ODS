package com.github.zero9178.mlirods.language.psi

import com.github.zero9178.mlirods.language.TableGenLanguage
import com.github.zero9178.mlirods.language.generated.psi.TableGenDefvarStatement
import com.github.zero9178.mlirods.language.generated.psi.TableGenIdentifierValueNode
import com.github.zero9178.mlirods.language.generated.psi.TableGenLetBodyItem
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
fun createIdentifierValueNode(project: Project, name: String): TableGenIdentifierValueNode {
    val text = "defvar $name = $name;"
    // The text is a literal built right here, so failing to find the node in it means the grammar no longer parses it
    // the way this factory assumes – something only a change to the grammar can cause, and something every caller
    // (rename, quick fixes) would otherwise report as a bare NPE.
    return checkNotNull(
        PsiTreeUtil.findChildOfAnyType(createFile(project, text), TableGenIdentifierValueNode::class.java)
    ) { "no identifier value node in '$text'" }
}

/**
 * Returns an identifier token with [name] as identifier.
 */
fun createIdentifier(project: Project, name: String): PsiElement {
    val text = "defvar $name = $name;"
    val statement = createFile(project, text).firstChild as? TableGenDefvarStatement
    return checkNotNull(statement?.nameIdentifier) { "no defvar name identifier in '$text'" }
}

/**
 * Returns a [TableGenLetBodyItem] of the form `let [name] = [value];`.
 */
fun createLetBodyItem(project: Project, name: String, value: String): TableGenLetBodyItem {
    val text = "class __dummy { let $name = $value; }"
    return checkNotNull(
        PsiTreeUtil.findChildOfType(createFile(project, text), TableGenLetBodyItem::class.java)
    ) { "no let body item in '$text'" }
}
