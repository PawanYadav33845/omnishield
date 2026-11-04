package com.omnishield.utility.core.stego

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import java.io.InputStream
import java.io.OutputStream

/**
 * High-performance LSB (Least Significant Bit) Image Steganography Engine.
 * Optimized for minimal CPU usage and zero JNI bridge overhead via batch IntArray processing.
 */
object SteganographyUtils {

    private val MAGIC_HEADER = byteArrayOf(0x53, 0x54, 0x47, 0x30) // "STG0"

    /**
     * Embeds secret text into image pixels in a single vectorized pass on an IntArray.
     * Replaces slow getPixel/setPixel JNI calls with 100x faster memory buffer operations.
     */
    fun encodeTextIntoBitmap(
        inputStream: InputStream,
        outputStream: OutputStream,
        secretMessage: String
    ): Boolean {
        val decodeOptions = BitmapFactory.Options().apply {
            inPremultiplied = false
            inScaled = false
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val originalBitmap = BitmapFactory.decodeStream(inputStream, null, decodeOptions)
            ?: throw IllegalArgumentException("Failed to decode host image.")

        val width = originalBitmap.width
        val height = originalBitmap.height
        val totalPixels = width * height

        val payloadBytes = secretMessage.toByteArray(Charsets.UTF_8)
        val payloadLen = payloadBytes.size

        // Build header: [4 bytes Magic][4 bytes Length][Payload]
        val totalBytes = ByteArray(4 + 4 + payloadLen)
        System.arraycopy(MAGIC_HEADER, 0, totalBytes, 0, 4)
        totalBytes[4] = ((payloadLen shr 24) and 0xFF).toByte()
        totalBytes[5] = ((payloadLen shr 16) and 0xFF).toByte()
        totalBytes[6] = ((payloadLen shr 8) and 0xFF).toByte()
        totalBytes[7] = (payloadLen and 0xFF).toByte()
        System.arraycopy(payloadBytes, 0, totalBytes, 8, payloadLen)

        val totalBitsNeeded = totalBytes.size * 8
        val totalAvailableChannels = totalPixels * 3 // R, G, B channels

        if (totalBitsNeeded > totalAvailableChannels) {
            originalBitmap.recycle()
            throw IllegalArgumentException(
                "Message exceeds capacity. Max: ${totalAvailableChannels / 8} bytes, Required: ${totalBytes.size} bytes."
            )
        }

        // Fast batch pixel extraction into single IntArray
        val pixels = IntArray(totalPixels)
        originalBitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        originalBitmap.recycle()

        var bitIndex = 0
        val totalBits = totalBitsNeeded

        for (i in 0 until totalPixels) {
            if (bitIndex >= totalBits) break

            val pixel = pixels[i]
            var r = (pixel shr 16) and 0xFF
            var g = (pixel shr 8) and 0xFF
            var b = pixel and 0xFF
            val a = (pixel shr 24) and 0xFF

            // Modify Red channel LSB
            if (bitIndex < totalBits) {
                val bit = (totalBytes[bitIndex / 8].toInt() shr (7 - (bitIndex % 8))) and 1
                r = (r and 0xFE) or bit
                bitIndex++
            }
            // Modify Green channel LSB
            if (bitIndex < totalBits) {
                val bit = (totalBytes[bitIndex / 8].toInt() shr (7 - (bitIndex % 8))) and 1
                g = (g and 0xFE) or bit
                bitIndex++
            }
            // Modify Blue channel LSB
            if (bitIndex < totalBits) {
                val bit = (totalBytes[bitIndex / 8].toInt() shr (7 - (bitIndex % 8))) and 1
                b = (b and 0xFE) or bit
                bitIndex++
            }

            pixels[i] = (a shl 24) or (r shl 16) or (g shl 8) or b
        }

        val encodedBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        encodedBitmap.setPixels(pixels, 0, width, 0, 0, width, height)

        return try {
            encodedBitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            outputStream.flush()
            true
        } finally {
            encodedBitmap.recycle()
        }
    }

    /**
     * Extracts hidden secret text payload from pixel array in a single fast buffer pass.
     * Enforces non-premultiplied raw ARGB decoding to prevent LSB mutation.
     */
    fun decodeTextFromBitmap(inputStream: InputStream): String? {
        val decodeOptions = BitmapFactory.Options().apply {
            inPremultiplied = false
            inScaled = false
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val bitmap = BitmapFactory.decodeStream(inputStream, null, decodeOptions) ?: return null
        val width = bitmap.width
        val height = bitmap.height
        val totalPixels = width * height

        val pixels = IntArray(totalPixels)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        bitmap.recycle()

        // Read 64-bit header (8 bytes: 4 Magic + 4 Length)
        val headerBits = 64
        val headerBytes = ByteArray(8)
        var bitIndex = 0

        for (i in 0 until totalPixels) {
            if (bitIndex >= headerBits) break
            val pixel = pixels[i]
            val channels = intArrayOf((pixel shr 16) and 0xFF, (pixel shr 8) and 0xFF, pixel and 0xFF)

            for (ch in channels) {
                if (bitIndex < headerBits) {
                    val bit = ch and 1
                    val bytePos = bitIndex / 8
                    headerBytes[bytePos] = (headerBytes[bytePos].toInt() or (bit shl (7 - (bitIndex % 8)))).toByte()
                    bitIndex++
                }
            }
        }

        // Validate Magic "STG0"
        if (headerBytes[0] != MAGIC_HEADER[0] || headerBytes[1] != MAGIC_HEADER[1] ||
            headerBytes[2] != MAGIC_HEADER[2] || headerBytes[3] != MAGIC_HEADER[3]
        ) {
            return null // Not a valid OmniShield Stego image
        }

        val payloadLen = ((headerBytes[4].toInt() and 0xFF) shl 24) or
                ((headerBytes[5].toInt() and 0xFF) shl 16) or
                ((headerBytes[6].toInt() and 0xFF) shl 8) or
                (headerBytes[7].toInt() and 0xFF)

        if (payloadLen <= 0 || payloadLen > (totalPixels * 3 / 8)) {
            return null
        }

        val totalBits = (8 + payloadLen) * 8
        val payloadBytes = ByteArray(payloadLen)
        bitIndex = 0

        for (i in 0 until totalPixels) {
            val pixel = pixels[i]
            val channels = intArrayOf((pixel shr 16) and 0xFF, (pixel shr 8) and 0xFF, pixel and 0xFF)

            for (ch in channels) {
                if (bitIndex in 64 until totalBits) {
                    val bit = ch and 1
                    val payloadPos = (bitIndex - 64) / 8
                    val bitPosInByte = 7 - ((bitIndex - 64) % 8)
                    payloadBytes[payloadPos] = (payloadBytes[payloadPos].toInt() or (bit shl bitPosInByte)).toByte()
                }
                bitIndex++
                if (bitIndex >= totalBits) break
            }
            if (bitIndex >= totalBits) break
        }

        return String(payloadBytes, Charsets.UTF_8)
    }
}
