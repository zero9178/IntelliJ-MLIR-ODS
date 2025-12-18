package com.github.zero9178.mlirods.language.psi.impl

import com.github.zero9178.mlirods.language.generated.psi.TableGenBlockStringValue
import com.github.zero9178.mlirods.language.generated.psi.impl.TableGenStringValueNodeImpl
import com.intellij.lang.ASTNode
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.util.text.isLineBreak
import com.intellij.psi.ElementManipulators
import com.intellij.psi.LiteralTextEscaper
import com.intellij.psi.PsiLanguageInjectionHost
import com.intellij.refactoring.suggested.strip
import com.intellij.refactoring.suggested.stripWhitespace
import java.util.*
import kotlin.math.min

class LeadingWhiteSpaceLiteralTextEscaper(host: TableGenBlockStringValue) :
    LiteralTextEscaper<TableGenBlockStringValue>(
        host
    ) {

    private fun sanitizeRange(rangeInsideHost: TextRange): Pair<TextRange, Boolean> {
        var range = rangeInsideHost
        val relevantRange = relevantTextRange
        assert(relevantRange.startOffset >= 0)
        assert(relevantRange.isProperRange)

        var valid = true
        if (range.startOffset >= relevantRange.endOffset) {
            valid = false
            range = TextRange(0, 0)
        }
        if (range.startOffset < relevantRange.startOffset) {
            valid = false
            range = TextRange(relevantRange.startOffset, range.endOffset)
        }
        if (range.endOffset > relevantRange.endOffset) {
            valid = false
            range = TextRange(range.startOffset, relevantRange.endOffset)
        }
        return range to valid
    }

    private val myHostOffsetToLine = TreeMap<Int, String>()
    private val myDecodedOffsetToLine = TreeMap<Int, TextRange>()

    private fun calculateWhitespacePrefix() {

        // Calculate the lowest prefix first. This is done by ignoring all blank lines and then counting the smallest
        // prefix of whitespace.
        var lowestPrefix = Int.MAX_VALUE
        val text = relevantTextRange.substring(myHost.text)
        for (line in text.lineSequence()) {
            if (line.isBlank())
                continue
            lowestPrefix = min(lowestPrefix, line.length - line.trimStart().length)
        }

        val linesWithOffset = sequence {
            var startLineIndex = 0
            val sb = StringBuilder()
            for ((index, c) in text.withIndex()) {
                when (c) {
                    '\n' -> {
                        sb.append(c)
                        yield(startLineIndex to sb.toString())
                        sb.clear()
                        startLineIndex = index + 1
                    }

                    '\r' -> {
                        sb.append(c)
                        yield(startLineIndex to sb.toString())
                        sb.clear()
                        startLineIndex = index + 1
                    }

                    else -> sb.append(c)
                }
            }
            if (sb.isNotEmpty())
                yield(startLineIndex to sb.toString())
        }.map { (index, l) ->
            (relevantTextRange.startOffset + index to l)
        }

        for ((offset, line) in linesWithOffset)
            if (line.length <= lowestPrefix) {
                myHostOffsetToLine[offset] = line.dropWhile { it.isWhitespace() && !it.isLineBreak() }
            }
            else
                myHostOffsetToLine[offset + lowestPrefix] = line.drop(lowestPrefix)

        var runningSize = 0
        for (entry in myHostOffsetToLine) {
            myDecodedOffsetToLine[runningSize] = TextRange(entry.key, entry.key + entry.value.length)
            runningSize += entry.value.length
        }
    }

    override fun decode(
        rangeInsideHost: TextRange,
        outChars: StringBuilder
    ): Boolean {
        calculateWhitespacePrefix()

        val (range, valid) = sanitizeRange(rangeInsideHost)

        val map = myHostOffsetToLine.subMap(
            myHostOffsetToLine.lowerKey(range.startOffset) ?: 0,
            myHostOffsetToLine.ceilingKey(range.endOffset) ?: relevantTextRange.endOffset
        )

        for (entry in map) {
            var line = entry.value
            val range = TextRange(entry.key, entry.key + line.length)
            if (range.containsOffset(rangeInsideHost.startOffset))
                line = line.drop(rangeInsideHost.startOffset - range.startOffset)
            if (range.containsOffset(rangeInsideHost.endOffset))
                line = line.dropLast(range.endOffset - rangeInsideHost.endOffset)

            outChars.append(line)
        }
//        outChars.append(range.substring(myHost.text))
        return valid
    }

    override fun getOffsetInHost(
        offsetInDecoded: Int,
        rangeInsideHost: TextRange
    ): Int {
        val entry = myDecodedOffsetToLine.floorEntry(offsetInDecoded) ?: return -1
        return entry.value.startOffset + (offsetInDecoded - entry.key)
//        return rangeInsideHost.startOffset + offsetInDecoded
    }

    override fun isOneLine() = false

    override fun getRelevantTextRange(): TextRange {
        val textRange = myHost.relevantTextRange
        val chars = myHost.text
        var endOffset = textRange.endOffset
        while (endOffset > textRange.startOffset && (chars[endOffset - 1].isWhitespace() && !chars[endOffset - 1].isLineBreak())) {
            endOffset--
        }

        return TextRange(textRange.startOffset, endOffset)
    }
}

abstract class TableGenBlockStringValueMixin(node: ASTNode) : TableGenStringValueNodeImpl(node),
    TableGenBlockStringValue {

    override fun isValidHost() = true

    override fun updateText(text: String): PsiLanguageInjectionHost =
        ElementManipulators.handleContentChange(this, text)

    override fun createLiteralTextEscaper() = LeadingWhiteSpaceLiteralTextEscaper(this)
}
