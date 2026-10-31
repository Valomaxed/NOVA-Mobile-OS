package com.nova.runtime.ai.io

import java.io.ByteArrayInputStream
import kotlin.test.Test
import kotlin.test.assertEquals

class DocumentTextReaderTest {
    @Test
    fun readsPastFirstShortReadAndRespectsBound() {
        val text = "A paragraph with Unicode café. ".repeat(2000) + "TAIL-921"
        val stream = object : ByteArrayInputStream(text.toByteArray()) {
            override fun read(buffer: ByteArray, offset: Int, length: Int): Int = super.read(buffer, offset, minOf(length, 7))
        }
        assertEquals(text, readDocumentText(stream, 128_000))
        assertEquals(text.take(100), readDocumentText(text.byteInputStream(), 100))
    }

    @Test
    fun utf16AndUtf8BomPreserveReadableCharacters() {
        val text = "Café: Δοκιμή and हिन्दी"
        for (charset in listOf(Charsets.UTF_16LE, Charsets.UTF_16BE, Charsets.UTF_8)) {
            val bom = when (charset) {
                Charsets.UTF_16LE -> byteArrayOf(0xff.toByte(), 0xfe.toByte())
                Charsets.UTF_16BE -> byteArrayOf(0xfe.toByte(), 0xff.toByte())
                else -> byteArrayOf(0xef.toByte(), 0xbb.toByte(), 0xbf.toByte())
            }
            assertEquals(text, readDocumentText((bom + text.toByteArray(charset)).inputStream(), 100))
        }
    }
}
