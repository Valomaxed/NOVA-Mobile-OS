package com.nova.runtime.ai.io

import java.io.InputStream
import java.io.PushbackInputStream

/** Reads through short stream reads and detects common Unicode BOMs without unbounded allocation. */
fun readDocumentText(input: InputStream, maxChars: Int): String {
    require(maxChars > 0)
    val stream = PushbackInputStream(input, 3)
    val prefix = ByteArray(3)
    var count = 0
    while (count < prefix.size) {
        val next = stream.read()
        if (next < 0) break
        prefix[count++] = next.toByte()
    }
    val (charset, skip) = when {
        count >= 3 && prefix[0] == 0xef.toByte() && prefix[1] == 0xbb.toByte() && prefix[2] == 0xbf.toByte() -> Charsets.UTF_8 to 3
        count >= 2 && prefix[0] == 0xff.toByte() && prefix[1] == 0xfe.toByte() -> Charsets.UTF_16LE to 2
        count >= 2 && prefix[0] == 0xfe.toByte() && prefix[1] == 0xff.toByte() -> Charsets.UTF_16BE to 2
        else -> Charsets.UTF_8 to 0
    }
    if (count > skip) stream.unread(prefix, skip, count - skip)
    val result = StringBuilder(minOf(maxChars, 8192))
    val reader = stream.reader(charset)
    val buffer = CharArray(8192)
    while (result.length < maxChars) {
        val read = reader.read(buffer, 0, minOf(buffer.size, maxChars - result.length))
        if (read < 0) break
        if (read > 0) result.append(buffer, 0, read)
    }
    return result.toString()
}
