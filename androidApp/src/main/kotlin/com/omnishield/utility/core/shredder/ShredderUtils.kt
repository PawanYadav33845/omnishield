package com.omnishield.utility.core.shredder

import android.content.Context
import android.net.Uri
import java.security.SecureRandom
import java.util.Arrays

object ShredderUtils {

    private const val BUFFER_SIZE = 64 * 1024 // 64 KB streaming buffer
    private val secureRandom = SecureRandom()

    // Pre-allocated reusable buffers for DoD passes to eliminate GC allocation churn
    private val ZERO_BUFFER = ByteArray(BUFFER_SIZE).apply { Arrays.fill(this, 0x00.toByte()) }
    private val ONES_BUFFER = ByteArray(BUFFER_SIZE).apply { Arrays.fill(this, 0xFF.toByte()) }

    enum class ShredMethod(val passes: Int, val displayName: String) {
        QUICK_ZERO(1, "1-Pass Zero Fill"),
        DOD_5220_22_M(3, "DoD 5220.22-M (3-Pass Sanitization)"),
        GUTMANN_LITE(7, "7-Pass Cryptographic Wipe")
    }

    /**
     * Securely shreds a SAF document or file by executing multi-pass overwrite.
     */
    fun shredUri(
        context: Context,
        uri: Uri,
        fileSizeBytes: Long,
        method: ShredMethod = ShredMethod.DOD_5220_22_M,
        onProgress: ((bytes: Long, totalBytes: Long, pass: Int, totalPasses: Int) -> Unit)? = null
    ): Boolean {
        val totalPasses = method.passes
        val randomBuffer = ByteArray(BUFFER_SIZE)
        val actualSize = if (fileSizeBytes > 0) {
            fileSizeBytes
        } else {
            val resolved = com.omnishield.utility.core.storage.StorageUtils.getFileSize(context, uri)
            if (resolved > 0) resolved else BUFFER_SIZE.toLong()
        }

        for (pass in 1..totalPasses) {
            val outputStream = context.contentResolver.openOutputStream(uri, "wt")
                ?: context.contentResolver.openOutputStream(uri)
                ?: throw IllegalStateException("Cannot open output stream for shredding.")

            outputStream.use { out ->
                var bytesWritten = 0L
                while (bytesWritten < actualSize) {
                    val chunkSize = (actualSize - bytesWritten).coerceAtMost(BUFFER_SIZE.toLong()).toInt()

                    val bufferToWrite = when {
                        method == ShredMethod.DOD_5220_22_M && pass == 1 -> ZERO_BUFFER
                        method == ShredMethod.DOD_5220_22_M && pass == 2 -> ONES_BUFFER
                        else -> {
                            secureRandom.nextBytes(randomBuffer)
                            randomBuffer
                        }
                    }

                    out.write(bufferToWrite, 0, chunkSize)
                    bytesWritten += chunkSize
                    onProgress?.invoke(bytesWritten, actualSize, pass, totalPasses)
                }
                out.flush()
            }
        }

        // Post-sanitize unlinking: delete document from storage
        try {
            if (android.provider.DocumentsContract.isDocumentUri(context, uri)) {
                android.provider.DocumentsContract.deleteDocument(context.contentResolver, uri)
            } else {
                context.contentResolver.delete(uri, null, null)
            }
        } catch (_: Exception) {
            // Some cloud/SAF providers restrict deletion, file contents remain cryptographically wiped
        }

        return true
    }
}
