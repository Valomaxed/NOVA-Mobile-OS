package com.nova.runtime.ai.io

import java.io.ByteArrayInputStream
import java.io.InputStream
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class BoundedInputTest {
    @Test
    fun exactLimit_preservesBytesAndRejectsOversizedInput() {
        val bytes = byteArrayOf(1, 2, 3)
        assertContentEquals(bytes, ByteArrayInputStream(bytes).readBoundedBytes(3))
        assertContentEquals(bytes, ByteArrayInputStream(bytes).readBoundedBytes(4))
        assertNull(ByteArrayInputStream(bytes).readBoundedBytes(2))
    }

    @Test
    fun oversizedStream_isNeverFullyConsumed() {
        var consumed = 0
        val stream =
            object : InputStream() {
                override fun read(): Int {
                    consumed++
                    return 7
                }

                override fun read(
                    buffer: ByteArray,
                    offset: Int,
                    length: Int,
                ): Int {
                    buffer.fill(7, offset, offset + length)
                    consumed += length
                    return length
                }
            }
        assertNull(stream.readBoundedBytes(100))
        assertEquals(101, consumed)
    }

    @Test
    fun zeroLimit_andZeroLengthReadsDoNotLoop() {
        assertContentEquals(byteArrayOf(), ByteArrayInputStream(byteArrayOf()).readBoundedBytes(0))
        assertNull(ByteArrayInputStream(byteArrayOf(1)).readBoundedBytes(0))
        val stream =
            object : ByteArrayInputStream(byteArrayOf(4, 5)) {
                override fun read(
                    buffer: ByteArray,
                    offset: Int,
                    length: Int,
                ) = 0
            }
        assertContentEquals(byteArrayOf(4, 5), stream.readBoundedBytes(3))
        assertFailsWith<IllegalArgumentException> { ByteArrayInputStream(byteArrayOf()).readBoundedBytes(-1) }
    }
}
