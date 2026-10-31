package com.nova.runtime.ai.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GroundedPromptBudgetTest {
    private val wrap: (String) -> String = { "Question: explain\nSource:\n$it\nAnswer:" }
    private val count: (String) -> Int = { it.length }

    @Test
    fun reservesAnswerSpaceAndKeepsInstructionsForBothModelSizes() {
        val source = "Inspect the bearings and align the motor. ".repeat(400)
        val small = assertNotNull(GroundedPromptBudget.fit(source, wrap, count, 2048, 1024))
        val large = assertNotNull(GroundedPromptBudget.fit(source, wrap, count, 4096, 1024))
        assertTrue(large.length > small.length)
        for ((prompt, limit) in listOf(small to 2048, large to 4096)) {
            assertTrue(count(prompt) + 1024 + GroundedPromptBudget.CONTROL_TOKEN_RESERVE <= limit)
            assertTrue(prompt.startsWith("Question: explain\nSource:\n"))
            assertTrue(prompt.endsWith("\nAnswer:"))
        }
    }

    @Test
    fun keepsShortSourceAndRejectsInstructionsThatCannotFit() {
        assertEquals(wrap("Short source"), GroundedPromptBudget.fit("Short source", wrap, count, 2048, 1024))
        assertNull(GroundedPromptBudget.fit("text", wrap, count, 100, 90))
        assertNull(GroundedPromptBudget.fit("", wrap, count, 2048, 1024))
    }

    @Test
    fun trimmingDoesNotSplitSurrogatePair() {
        val result = assertNotNull(GroundedPromptBudget.fit("😀".repeat(300), { it }, count, 301, 100))
        assertTrue(!Character.isHighSurrogate(result.last()))
        assertTrue(result.length + 164 <= 301)
    }
}
