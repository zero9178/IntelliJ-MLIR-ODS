package com.github.zero9178.mlirods.language

import com.github.zero9178.mlirods.language.generated.psi.TableGenBlockStringValue
import com.intellij.lang.injection.MultiHostRegistrar
import com.intellij.lang.injection.general.Injection
import com.intellij.lang.injection.general.LanguageInjectionPerformer
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.util.text.LineTokenizer
import com.intellij.psi.ElementManipulators
import com.intellij.psi.PsiElement
import kotlin.math.min

/**
 * Performs language injections into block strings.
 *
 * Block strings in TableGen are usually indented to match the surrounding code. The injected file must not see this
 * indentation, as it is meaningful in languages such as Markdown. Instead of decoding it away, which the platform does
 * not support for languages with more than one PSI root, the content is injected one line at a time with the
 * indentation shared by all lines lying between the injected places.
 */
internal class TableGenLanguageInjectionPerformer : LanguageInjectionPerformer {
    override fun isPrimary() = false

    override fun performInjection(registrar: MultiHostRegistrar, injection: Injection, context: PsiElement): Boolean {
        if (context !is TableGenBlockStringValue) return false
        val language = injection.injectedLanguage ?: return false

        val ranges = trimmedContentRanges(context)
        registrar.startInjecting(language)
        ranges.forEachIndexed { index, range ->
            registrar.addPlace(
                if (index == 0) injection.prefix else null,
                if (index == ranges.lastIndex) injection.suffix else null,
                context,
                range
            )
        }
        registrar.doneInjecting()
        return true
    }
}

/**
 * Returns the ranges within [host] that make up its content once the indentation shared by all non-blank lines is
 * removed, following the semantics of Kotlin's `trimIndent`: A blank first or last line is dropped entirely, the common
 * indentation is removed from every remaining line and the lines are joined by the line breaks of the host. A trailing
 * line break does not start another line. The result is never empty; a block string without content yields a single
 * empty range.
 */
private fun trimmedContentRanges(host: TableGenBlockStringValue): List<TextRange> {
    val contentRange = ElementManipulators.getValueTextRange(host)
    val content = contentRange.subSequence(host.node.chars)
    val lines = mutableListOf<TextRange>()
    val tokenizer = LineTokenizer(content)
    while (!tokenizer.atEnd()) {
        lines.add(TextRange.from(tokenizer.offset, tokenizer.length))
        tokenizer.advance()
    }
    val kept = lines.filterIndexed { index, line ->
        !((index == 0 || index == lines.lastIndex) && line.subSequence(content).isBlank())
    }
    if (kept.isEmpty()) return listOf(TextRange.from(contentRange.startOffset, 0))

    val indent = commonIndentation(kept.asSequence().map { it.subSequence(content) })?.length ?: 0
    val result = mutableListOf<TextRange>()
    kept.forEachIndexed { index, line ->
        val start = line.startOffset + min(indent, line.length)
        // A line reaches up to the start of the next one, including its line break. The last line has none to include.
        val end = kept.getOrNull(index + 1)?.startOffset ?: line.endOffset
        val range = TextRange(start, end).shiftRight(contentRange.startOffset)
        // Empty lines and lines without indentation to remove continue the previous place rather than starting a new
        // one, keeping the number of places small.
        val previous = result.lastOrNull()
        if (previous != null && previous.endOffset == range.startOffset) {
            result[result.lastIndex] = previous.union(range)
        } else {
            result.add(range)
        }
    }
    return result
}

/**
 * Returns the indentation shared by all non-blank [lines], i.e. the shortest leading whitespace among them, or null if
 * all lines are blank. This is the indentation left out by [TableGenLanguageInjectionPerformer].
 */
internal fun commonIndentation(lines: Sequence<CharSequence>): CharSequence? =
    lines.filter { it.isNotBlank() }.map { it.takeWhile(Char::isWhitespace) }.minByOrNull { it.length }
