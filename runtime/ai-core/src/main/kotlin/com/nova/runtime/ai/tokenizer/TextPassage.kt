package com.nova.runtime.ai.tokenizer

data class TextPassage(val start: Int, val end: Int, val text: String)

/** Overlapping source windows, sized with the encoder's actual tokenizer. */
object TextPassages {
    fun split(
        text: String,
        tokenCount: (String) -> Int,
        maxTokens: Int = 254,
        overlapTokens: Int = 32,
    ): List<TextPassage> {
        require(maxTokens > 0 && overlapTokens in 0 until maxTokens)
        if (text.isBlank()) return emptyList()
        val passages = mutableListOf<TextPassage>()
        var start = 0
        while (start < text.length) {
            var end = minOf(text.length, start + maxTokens * 12)
            while (tokenCount(text.substring(start, end)) > maxTokens) {
                end = start + (end - start) / 2
                require(end > start) { "A source character exceeds the token budget" }
            }
            // Grow to the largest fitting window. The final count is rechecked below.
            var low = end
            var high = minOf(text.length, start + maxTokens * 12)
            while (low < high) {
                val middle = (low + high + 1) / 2
                if (tokenCount(text.substring(start, middle)) <= maxTokens) low = middle else high = middle - 1
            }
            end = low
            if (end < text.length) {
                val boundary = text.lastIndexOfAny(charArrayOf('\n', ' ', '\t'), end - 1)
                if (boundary > start + (end - start) * 3 / 4) end = boundary + 1
            }
            if (end < text.length && end > start && Character.isHighSurrogate(text[end - 1])) end--
            while (tokenCount(text.substring(start, end)) > maxTokens) end--
            require(end > start)
            passages += TextPassage(start, end, text.substring(start, end))
            if (end == text.length) break
            var overlapStart = end
            if (overlapTokens > 0) {
                // Walk back whole words, bounding the repeated context by real word pieces.
                var candidate = end - 1
                while (candidate > start) {
                    if (text[candidate - 1].isWhitespace() && !text[candidate].isWhitespace()) {
                        if (tokenCount(text.substring(candidate, end)) > overlapTokens) break
                        overlapStart = candidate
                    }
                    candidate--
                }
            }
            start = overlapStart.coerceAtLeast(start + 1)
        }
        return passages
    }
}
