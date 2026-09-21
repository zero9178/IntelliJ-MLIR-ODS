package com.github.zero9178.mlirods

import com.intellij.openapi.util.text.HtmlChunk
import java.nio.file.Path
import java.util.*
import kotlin.io.path.readLines

/**
 * A problem reported by an inspection.
 */
data class Problem(
    /** Name of the severity as used by the IDE, e.g. 'ERROR' or 'WEAK WARNING'. */
    val severity: String,
    /** Path relative to the root of the project. */
    val file: String,
    val line: String,
    val inspection: String,
    val description: String,
) {
    /**
     * Whether the file is one of the tests of TableGen itself. Many of these are invalid on purpose to test the errors
     * TableGen reports, making problems found in them expected rather than a sign of something being wrong with the
     * plugin. Tests of other projects, e.g. 'mlir/test', are tests of what is generated and expected to be valid.
     */
    val isInTest get() = file.startsWith("llvm/test/")
}

/**
 * Reads the problems written to [file] by the plugin of the integration tests: one line per problem consisting of the
 * fields of [Problem] separated by tabs.
 */
fun readProblems(file: Path): List<Problem> = file.readLines().map {
    val (severity, path, line, inspection, description) = it.split('\t', limit = 5)
    Problem(severity, path, line, inspection, description)
}

private const val STYLE = """
body { font-family: system-ui, sans-serif; margin: 2rem; }
table { border-collapse: collapse; }
th, td { border: 1px solid #8884; padding: .25rem .5rem; text-align: left; vertical-align: top; }
.ERROR { color: #d32f2f; } .WARNING { color: #b26a00; }
input { margin: 1rem 0; padding: .4rem; width: 40rem; max-width: 100%; }
"""

private const val FILTER_SCRIPT = """
const rows = [...document.querySelectorAll("#problems tr")].slice(1);
document.getElementById("filter").addEventListener("input", event => {
  const needle = event.target.value.toLowerCase();
  for (const row of rows) row.hidden = !row.textContent.toLowerCase().includes(needle);
});
"""

private fun Long.seconds() = "%.1f s".format(Locale.ROOT, this / 1000.0)

private fun row(tag: String, vararg cells: String) = HtmlChunk.tag("tr").children(cells.map { HtmlChunk.tag(tag).addText(it) })

private fun Problem.toRow() = HtmlChunk.tag("tr").children(
    HtmlChunk.tag("td").setClass(severity).addText(severity),
    HtmlChunk.tag("td").addText(file),
    HtmlChunk.tag("td").addText(line),
    HtmlChunk.tag("td").addText(inspection),
    HtmlChunk.tag("td").addText(description),
)

/**
 * Outcome of inspecting all TableGen files of LLVM. All times are in milliseconds.
 */
class Summary(
    private val ide: String,
    llvmCommit: String,
    private val files: Int,
    private val filesWithContext: Int,
    private val cold: Long,
    private val measured: List<Long>,
    private val cached: Long,
    /** Load average of the machine right before the IDE was started. */
    private val loadAverage: Double,
    private val problems: List<Problem>,
) {
    private val median = measured.sorted().let { (it[(it.size - 1) / 2] + it[it.size / 2]) / 2 }

    /**
     * Half the distance between the fastest and the slowest run relative to the median.
     */
    private val spread = "±%.1f%%".format(Locale.ROOT, (measured.max() - measured.min()) / 2.0 / median * 100)

    private fun List<Problem>.counts() = listOf("ERROR", "WARNING", "WEAK WARNING").joinToString { severity ->
        "${count { it.severity == severity }} ${severity.lowercase()}s"
    }

    /**
     * The problems to keep an eye on: the ones found in the tests of TableGen are for the most part errors made on
     * purpose.
     */
    private val counts = problems.filterNot { it.isInTest }.counts()

    private val testCounts = problems.filter { it.isInTest }.counts()

    /**
     * The one line worth remembering of a run.
     */
    override fun toString() = "TableGen inspections of LLVM in $ide: $files files ($filesWithContext with context), " +
            "median ${median.seconds()} $spread over ${measured.size} runs, cold ${cold.seconds()}, " +
            "cached ${cached.seconds()}, load average ${"%.1f".format(Locale.ROOT, loadAverage)}, $counts " +
            "(llvm/test: $testCounts)"

    private val title = "TableGen inspections of LLVM"

    private val overview = HtmlChunk.tag("table").children(
        row("td", "IDE", ide),
        row("td", "LLVM commit", llvmCommit),
        row("td", "TableGen files", "$files ($filesWithContext with context)"),
        row("td", "Median", "${median.seconds()} $spread"),
        row("td", "Runs", measured.joinToString { it.seconds() }),
        row("td", "Cold", cold.seconds()),
        row("td", "Cached", cached.seconds()),
        row("td", "Load average before the run", "%.1f".format(Locale.ROOT, loadAverage)),
        row("td", "Problems", counts),
        row("td", "Problems in llvm/test", testCounts),
    )

    private val allProblems = HtmlChunk.tag("table").attr("id", "problems").children(
        listOf(row("th", "Severity", "File", "Line", "Inspection", "Description")) + problems.map { it.toRow() }
    )

    fun toHtml() = "<!DOCTYPE html>\n" + HtmlChunk.html().attr("lang", "en").children(
        HtmlChunk.head().children(
            HtmlChunk.tag("meta").attr("charset", "utf-8"),
            HtmlChunk.tag("title").addText(title),
            HtmlChunk.styleTag(STYLE),
        ),
        HtmlChunk.body().children(
            HtmlChunk.tag("h1").addText(title),
            overview,
            HtmlChunk.tag("h2").addText("Problems"),
            HtmlChunk.tag("input").attr("id", "filter").attr("type", "search")
                .attr("placeholder", "Filter by file, severity or description"),
            allProblems,
            HtmlChunk.tag("script").addRaw(FILTER_SCRIPT),
        ),
    )

    /**
     * The report as shown by GitHub as the summary of a job. GitHub renders HTML but neither styles nor scripts.
     */
    fun toJobSummary() = HtmlChunk.fragment(
        HtmlChunk.tag("h2").addText(title),
        overview,
        HtmlChunk.tag("details").children(HtmlChunk.tag("summary").addText("Problems"), allProblems),
    ).toString() + "\n"
}
