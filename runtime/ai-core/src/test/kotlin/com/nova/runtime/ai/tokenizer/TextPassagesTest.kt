package com.nova.runtime.ai.tokenizer

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TextPassagesTest {
    @Test
    fun longDocumentIsCoveredIncludingTailWithinRealTokenBudget() {
        val tokenizer = MiniLmTokenizer.fromClasspath()
        val text = "Introduction to concrete structures. ".repeat(800) + "Final access code: QUARTZ-921."
        val passages = TextPassages.split(text, { tokenizer.tokenizeToWordPieces(it).size })
        assertTrue(passages.size > 2)
        assertEquals(0, passages.first().start)
        assertEquals(text.length, passages.last().end)
        assertTrue(passages.last().text.contains("QUARTZ-921"))
        passages.forEach { passage ->
            assertEquals(text.substring(passage.start, passage.end), passage.text)
            assertTrue(tokenizer.tokenizeToWordPieces(passage.text).size <= 254)
        }
        passages.zipWithNext().forEach { (a, b) -> assertTrue(b.start <= a.end && b.end > a.end) }
    }

    @Test
    fun punctuationAndSurrogatesDoNotLeaveGaps() {
        val tokenizer = MiniLmTokenizer.fromClasspath()
        val text = "a?!.,😀 ".repeat(200)
        val passages = TextPassages.split(text, { tokenizer.tokenizeToWordPieces(it).size }, maxTokens = 40, overlapTokens = 8)
        assertEquals(text.length, passages.last().end)
        for (passage in passages) {
            assertTrue(!Character.isLowSurrogate(passage.text.first()))
            assertTrue(!Character.isHighSurrogate(passage.text.last()))
            assertTrue(tokenizer.tokenizeToWordPieces(passage.text).size <= 40)
        }
        passages.zipWithNext().forEach { (a, b) -> assertTrue(b.start <= a.end) }
    }
}
