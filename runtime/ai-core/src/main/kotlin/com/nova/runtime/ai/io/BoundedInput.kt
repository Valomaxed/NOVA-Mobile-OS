package com.nova.runtime.ai.io

import java.io.ByteArrayOutputStream
import java.io.InputStream

/** Returns complete bytes or null if oversized. Consumes at most maxBytes + 1 bytes; caller owns the stream. */
fun InputStream.readBoundedBytes(maxBytes: Int): ByteArray? {
    require(maxBytes >= 0) { "Byte limit must be nonnegative" }
    val output = ByteArrayOutputStream(minOf(maxBytes, DEFAULT_BUFFER_SIZE))
    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
    var total = 0
    while (total < maxBytes) {
        val read = read(buffer, 0, minOf(buffer.size, maxBytes - total))
        if (read < 0) return output.toByteArray()
        if (read == 0) {
            val next = read()
            if (next < 0) return output.toByteArray()
            output.write(next)
            total++
        } else {
            output.write(buffer, 0, read)
            total += read
        }
    }
    return if (read() == -1) output.toByteArray() else null
}
