package com.nova.runtime.storage.search

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DocumentPassageSelectorTest {
    @Test
    fun encodedUnicodeAndParagraphsSurviveNormalization() {
        val normalized = DocumentContentNormalizer.toPlainText("<p>Caf&#233; &amp; tea</p><p>Δοκιμή &#x1F600;</p>")
        assertTrue(normalized.contains("Café & tea"))
        assertTrue(normalized.contains("Δοκιμή 😀"))
        assertFalse(DocumentContentNormalizer.isGroundedAnswerable("Binary corruption ".repeat(5) + "�".repeat(50)))
    }

    @Test
    fun findsLateFactInsteadOfReturningTheIntroduction() {
        val content = "Introduction and unrelated background material.\n".repeat(900) +
            "Warranty policy\nThe QUARTZ motor has a seven-year warranty covering replacement bearings. " + "Details follow. ".repeat(80)
        val selected = DocumentPassageSelector.select(content, "What is the warranty for the QUARTZ motor?").orEmpty()
        assertTrue(selected.contains("seven-year warranty"))
        assertTrue(selected.length > 400)
        assertTrue(selected.length <= DocumentPassageSelector.DEFAULT_CHARS)
    }

    @Test
    fun semanticOffsetCanFindPassageWithoutLexicalOverlap() {
        val body = "Unrelated introduction. ".repeat(600) + "The actuator is covered for seven years. "
        val selected = DocumentPassageSelector.select(body, "repair guarantee", preferredStart = body.length - 40).orEmpty()
        assertTrue(selected.contains("seven years"))
    }

    @Test
    fun genericAnswersPreserveParagraphsAndDoNotBecomeMenus() {
        val body = "Engineering overview.\n\n" + "The method requires checking support conditions before calculating reactions. ".repeat(12)
        val answer = DocumentDateIntelligence.formatAnswer(
            query = "Explain the engineering method", snippet = body, sourceFileName = "notes.txt", sourceModifiedAt = null,
        )
        assertFalse(answer.startsWith("Menu:"))
        assertTrue(answer.contains("\n\n"))
        assertTrue(answer.length > 400)
    }
}
