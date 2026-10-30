package com.embedhub.app

import com.embedhub.app.core.checksum.ChecksumUtils
import com.embedhub.app.core.model.ChecksumType
import org.junit.Assert.*
import org.junit.Test

class ChecksumUtilsTest {

    @Test
    fun testXorChecksum() {
        val data = "HELLO".toByteArray(Charsets.UTF_8)
        val xor = ChecksumUtils.calculateXor(data)

        var expected: Byte = 0
        for (b in data) {
            expected = (expected.toInt() xor b.toInt()).toByte()
        }
        assertEquals(expected, xor)
    }

    @Test
    fun testCrc8ChecksumDeterministic() {
        val data = "123456789".toByteArray(Charsets.UTF_8)
        val crc = ChecksumUtils.calculateCrc8(data)
        assertNotEquals(0.toByte(), crc)

        val crcAgain = ChecksumUtils.calculateCrc8(data)
        assertEquals(crc, crcAgain)
    }

    @Test
    fun testFramingAndUnframingWithXor() {
        val payload = "SPEED:85".toByteArray(Charsets.UTF_8)
        val framed = ChecksumUtils.framePayload(
            payload = payload,
            checksumType = ChecksumType.XOR,
            startMarker = "$",
            endMarker = "#"
        )

        assertEquals('$'.code.toByte(), framed.first())
        assertEquals('#'.code.toByte(), framed.last())

        val unframed = ChecksumUtils.unframePayload(
            data = framed,
            checksumType = ChecksumType.XOR,
            startMarker = "$",
            endMarker = "#"
        )

        assertNotNull(unframed)
        assertArrayEquals(payload, unframed)
    }

    @Test
    fun testUnframingFailsOnCorruptChecksum() {
        val payload = "CMD:FORWARD".toByteArray(Charsets.UTF_8)
        val framed = ChecksumUtils.framePayload(
            payload = payload,
            checksumType = ChecksumType.CRC8,
            startMarker = "[",
            endMarker = "]"
        )

        // Corrupt checksum byte (penultimate byte before endMarker)
        val corrupted = framed.copyOf()
        val chkIndex = corrupted.size - 2
        corrupted[chkIndex] = (corrupted[chkIndex] + 1).toByte()

        val result = ChecksumUtils.unframePayload(
            data = corrupted,
            checksumType = ChecksumType.CRC8,
            startMarker = "[",
            endMarker = "]"
        )

        assertNull("Checksum verification must fail for corrupted payload", result)
    }
}
