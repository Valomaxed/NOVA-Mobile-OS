package com.nova.runtime.ai.model

data class OcrLineBox(val text: String, val left: Int, val top: Int, val right: Int, val bottom: Int)

/** Preserve weekday table columns instead of concatenating each column into a separate block. */
object OcrLayoutFormatter {
    fun format(lines: List<OcrLineBox>, fallback: String): String {
        val usable = lines.filter { it.text.isNotBlank() && it.bottom > it.top }
        if (usable.isEmpty()) return fallback
        val rows = mutableListOf<MutableList<OcrLineBox>>()
        for (line in usable.sortedWith(compareBy<OcrLineBox> { it.top }.thenBy { it.left })) {
            val row = rows.lastOrNull()
            val reference = row?.firstOrNull()
            val overlaps = reference != null &&
                minOf(reference.bottom, line.bottom) - maxOf(reference.top, line.top) >=
                minOf(reference.bottom - reference.top, line.bottom - line.top) / 2
            if (overlaps) row!!.add(line) else rows.add(mutableListOf(line))
        }
        val dayPattern = Regex("""\b(?:mon(?:day)?|tue(?:sday)?|wed(?:nesday)?|thu(?:rsday)?|fri(?:day)?|sat(?:urday)?|sun(?:day)?)\b""", RegexOption.IGNORE_CASE)
        val header = rows.firstOrNull { row -> row.count { dayPattern.containsMatchIn(it.text) } >= 2 }
            ?: return fallback // Do not interleave unrelated columns of prose.
        val columns = header.filter { dayPattern.containsMatchIn(it.text) }.sortedBy { it.left }
        val hasLeadingColumn = rows.any { row -> row.any { it.left < columns.first().left - 10 } }
        return rows.joinToString("\n") { row ->
            val cells = Array(columns.size + if (hasLeadingColumn) 1 else 0) { mutableListOf<String>() }
            for (line in row.sortedBy { it.left }) {
                val column = if (hasLeadingColumn && line.left < columns.first().left - 10) 0 else {
                    columns.indices.minBy { kotlin.math.abs(columns[it].left - line.left) } + if (hasLeadingColumn) 1 else 0
                }
                cells[column] += line.text.trim()
            }
            cells.joinToString(" | ") { it.joinToString(" ") }
        }
    }
}
