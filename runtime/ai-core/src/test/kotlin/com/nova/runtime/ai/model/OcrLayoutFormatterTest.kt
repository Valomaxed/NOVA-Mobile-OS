package com.nova.runtime.ai.model

import kotlin.test.Test
import kotlin.test.assertEquals

class OcrLayoutFormatterTest {
    @Test
    fun weekdayColumnsBecomeReadableRows() {
        val lines = listOf(
            OcrLineBox("Monday", 100, 0, 190, 20),
            OcrLineBox("Math", 100, 40, 190, 60),
            OcrLineBox("Tuesday", 300, 0, 400, 20),
            OcrLineBox("Physics", 300, 40, 400, 60),
            OcrLineBox("09:00", 0, 40, 90, 60),
        )
        assertEquals(" | Monday | Tuesday\n09:00 | Math | Physics", OcrLayoutFormatter.format(lines, "column soup"))
        assertEquals(" | Monday | Tuesday\n09:00 |  | Physics", OcrLayoutFormatter.format(lines.filter { it.text != "Math" }, "column soup"))
    }

    @Test
    fun ordinaryProseKeepsTheRecognizersReadingOrder() {
        val text = "A multi-column article with ordinary paragraphs."
        assertEquals(text, OcrLayoutFormatter.format(listOf(OcrLineBox(text, 0, 0, 500, 20)), text))
    }
}
