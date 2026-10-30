package com.embedhub.app.core.checksum

import com.embedhub.app.core.model.ChecksumType

object ChecksumUtils {

    /**
     * Calculates 8-bit XOR checksum of the given byte array.
     */
    fun calculateXor(data: ByteArray, offset: Int = 0, length: Int = data.size): Byte {
        var xor: Byte = 0
        for (i in offset until (offset + length)) {
            xor = (xor.toInt() xor data[i].toInt()).toByte()
        }
        return xor
    }

    /**
     * Calculates CRC-8 (polynomial 0x07, init 0x00)
     */
    fun calculateCrc8(data: ByteArray, offset: Int = 0, length: Int = data.size): Byte {
        var crc = 0x00
        for (i in offset until (offset + length)) {
            crc = crc xor (data[i].toInt() and 0xFF)
            for (j in 0 until 8) {
                crc = if ((crc and 0x80) != 0) {
                    ((crc shl 1) xor 0x07) and 0xFF
                } else {
                    (crc shl 1) and 0xFF
                }
            }
        }
        return crc.toByte()
    }

    /**
     * Frames payload with optional start marker, length prefix, checksum, and end marker.
     */
    fun framePayload(
        payload: ByteArray,
        checksumType: ChecksumType = ChecksumType.NONE,
        startMarker: String = "",
        endMarker: String = ""
    ): ByteArray {
        val startBytes = startMarker.toByteArray(Charsets.UTF_8)
        val endBytes = endMarker.toByteArray(Charsets.UTF_8)

        val checksumBytes = when (checksumType) {
            ChecksumType.NONE -> byteArrayOf()
            ChecksumType.XOR -> byteArrayOf(calculateXor(payload))
            ChecksumType.CRC8 -> byteArrayOf(calculateCrc8(payload))
        }

        val totalSize = startBytes.size + payload.size + checksumBytes.size + endBytes.size
        val result = ByteArray(totalSize)
        var offset = 0

        System.arraycopy(startBytes, 0, result, offset, startBytes.size)
        offset += startBytes.size

        System.arraycopy(payload, 0, result, offset, payload.size)
        offset += payload.size

        System.arraycopy(checksumBytes, 0, result, offset, checksumBytes.size)
        offset += checksumBytes.size

        System.arraycopy(endBytes, 0, result, offset, endBytes.size)

        return result
    }

    /**
     * Validates frame and strips markers and checksum if valid.
     * Returns the payload or null if checksum validation failed.
     */
    fun unframePayload(
        data: ByteArray,
        checksumType: ChecksumType,
        startMarker: String = "",
        endMarker: String = ""
    ): ByteArray? {
        val startBytes = startMarker.toByteArray(Charsets.UTF_8)
        val endBytes = endMarker.toByteArray(Charsets.UTF_8)
        val checksumLen = if (checksumType == ChecksumType.NONE) 0 else 1

        if (data.size < (startBytes.size + endBytes.size + checksumLen)) {
            return null
        }

        // Verify start marker
        for (i in startBytes.indices) {
            if (data[i] != startBytes[i]) return null
        }

        // Verify end marker
        val endOffset = data.size - endBytes.size
        for (i in endBytes.indices) {
            if (data[endOffset + i] != endBytes[i]) return null
        }

        val payloadStart = startBytes.size
        val payloadLength = data.size - startBytes.size - endBytes.size - checksumLen
        if (payloadLength < 0) return null

        val payload = ByteArray(payloadLength)
        System.arraycopy(data, payloadStart, payload, 0, payloadLength)

        if (checksumType != ChecksumType.NONE) {
            val expectedChecksum = data[payloadStart + payloadLength]
            val actualChecksum = when (checksumType) {
                ChecksumType.XOR -> calculateXor(payload)
                ChecksumType.CRC8 -> calculateCrc8(payload)
                ChecksumType.NONE -> 0
            }
            if (expectedChecksum != actualChecksum) {
                return null
            }
        }

        return payload
    }
}
