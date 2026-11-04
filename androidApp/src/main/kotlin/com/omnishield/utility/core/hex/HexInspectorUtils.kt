package com.omnishield.utility.core.hex

import java.io.InputStream

data class HexLine(
    val offsetHex: String,
    val hexBytes: String,
    val asciiText: String
)

/**
 * Ultra-high-performance Hexadecimal Inspector.
 * Uses direct lookup tables and pre-allocated StringBuilder buffers to achieve
 * sub-millisecond dumps with minimal GC overhead.
 */
object HexInspectorUtils {

    private val HEX_CHARS = "0123456789ABCDEF".toCharArray()

    fun generateHexDump(inputStream: InputStream, maxBytes: Int = 16384): List<HexLine> {
        val bytes = ByteArray(maxBytes)
        val read = inputStream.read(bytes)
        if (read <= 0) return emptyList()

        val expectedLines = (read + 15) / 16
        val lines = ArrayList<HexLine>(expectedLines)
        var offset = 0

        val hexChars = CharArray(49) // 16 bytes * 3 chars + 1 extra space in middle
        val asciiChars = CharArray(16)

        while (offset < read) {
            val count = (read - offset).coerceAtMost(16)
            val offsetHex = String.format("%08X", offset)

            var hexPos = 0
            for (i in 0 until 16) {
                if (i < count) {
                    val b = bytes[offset + i].toInt() and 0xFF
                    hexChars[hexPos++] = HEX_CHARS[b ushr 4]
                    hexChars[hexPos++] = HEX_CHARS[b and 0x0F]
                    hexChars[hexPos++] = ' '
                    if (i == 7) hexChars[hexPos++] = ' '

                    val c = b.toChar()
                    asciiChars[i] = if (b in 32..126) c else '.'
                } else {
                    hexChars[hexPos++] = ' '
                    hexChars[hexPos++] = ' '
                    hexChars[hexPos++] = ' '
                    if (i == 7) hexChars[hexPos++] = ' '
                    asciiChars[i] = ' '
                }
            }

            lines.add(
                HexLine(
                    offsetHex = offsetHex,
                    hexBytes = String(hexChars, 0, hexPos).trimEnd(),
                    asciiText = String(asciiChars, 0, count)
                )
            )

            offset += 16
        }

        return lines
    }
}
