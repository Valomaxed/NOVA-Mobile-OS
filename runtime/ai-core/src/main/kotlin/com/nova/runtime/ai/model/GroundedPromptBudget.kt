package com.nova.runtime.ai.model

/** Fits source text using the model tokenizer; instructions and answer space are never trimmed. */
object GroundedPromptBudget {
    const val CONTROL_TOKEN_RESERVE = 64

    fun fit(
        context: String,
        buildPrompt: (String) -> String,
        tokenCount: (String) -> Int,
        contextTokens: Int,
        outputTokens: Int,
    ): String? {
        require(contextTokens > 0 && outputTokens > 0)
        val inputBudget = contextTokens - outputTokens - CONTROL_TOKEN_RESERVE
        if (inputBudget <= 0 || context.isBlank() || tokenCount(buildPrompt("")) > inputBudget) return null
        val full = buildPrompt(context)
        if (tokenCount(full) <= inputBudget) return full
        var low = 0
        var high = context.length
        var best: String? = null
        while (low <= high) {
            val middle = low + (high - low) / 2
            var end = middle
            if (end > 0 && end < context.length && Character.isHighSurrogate(context[end - 1])) end--
            val source = context.substring(0, end).trimEnd()
            val prompt = buildPrompt(source)
            if (tokenCount(prompt) <= inputBudget) {
                if (source.isNotBlank()) best = prompt
                low = middle + 1
            } else {
                high = middle - 1
            }
        }
        // Always validate the final result: tokenization at boundaries need not be monotonic.
        return best?.takeIf { tokenCount(it) <= inputBudget }
    }
}
