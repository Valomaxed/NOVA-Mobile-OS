package com.nova.runtime.storage.search

/** Selects a readable source window near the query, including late sections. */
object DocumentPassageSelector {
    const val DEFAULT_CHARS = 8_000

    fun select(content: String, query: String, maxChars: Int = DEFAULT_CHARS, preferredStart: Int? = null): String? {
        require(maxChars > 0)
        if (content.isBlank()) return null
        if (content.length <= maxChars) return content.trim()
        val tokens = DocumentContentRanker.tokenize(query).distinct()
        val positions = tokens.associateWith { token ->
            Regex(Regex.escape(token), RegexOption.IGNORE_CASE).findAll(content).take(80).map { it.range.first }.toList()
        }
        val anchors = positions.values.flatten().toMutableList()
        preferredStart?.takeIf { it in content.indices }?.let { anchors += it }
        if (anchors.isEmpty()) anchors += 0
        val windows = anchors.map { anchor ->
            var start = (anchor - maxChars / 3).coerceAtLeast(0)
            val paragraphStart = content.lastIndexOf('\n', start)
            if (paragraphStart >= (start - maxChars / 6).coerceAtLeast(0)) start = paragraphStart + 1
            var end = (start + maxChars).coerceAtMost(content.length)
            if (end < content.length) {
                val boundary = content.lastIndexOfAny(charArrayOf('\n', '.', ' ', '\t'), end - 1)
                if (boundary > start + maxChars * 3 / 4 && boundary > anchor) end = boundary + 1
            }
            if (start > 0 && Character.isLowSurrogate(content[start])) start--
            if (end < content.length && Character.isHighSurrogate(content[end - 1])) end--
            val score = positions.values.sumOf { occurrences ->
                val matches = occurrences.count { it >= start && it < end }.coerceAtMost(3)
                matches.toDouble() / kotlin.math.sqrt(occurrences.size.coerceAtLeast(1).toDouble())
            } + if (preferredStart != null && preferredStart in start until end) 0.5 else 0.0
            Triple(start, end, score)
        }.distinctBy { it.first to it.second }
        val best = windows.maxBy { it.third }
        return content.substring(best.first, best.second).trim().takeIf { it.isNotBlank() }
    }
}
