package com.omnishield.utility.data.repository

import android.content.Context
import android.net.Uri
import com.omnishield.utility.core.crypto.CryptoUtils
import com.omnishield.utility.core.storage.StorageUtils
import com.omnishield.utility.domain.model.CryptoMode
import com.omnishield.utility.domain.model.CryptoProgress
import com.omnishield.utility.domain.model.CryptoResult
import com.omnishield.utility.domain.repository.ICryptoRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream

class CryptoRepository(
    private val context: Context
) : ICryptoRepository {

    override fun executeCryptoOperation(
        inputUri: Uri,
        outputDestinationUri: Uri?,
        passphrase: CharArray,
        mode: CryptoMode
    ): Flow<Pair<CryptoProgress, CryptoResult?>> = channelFlow {
        val startTime = System.currentTimeMillis()
        val originalName = StorageUtils.getFileName(context, inputUri)
        val originalSize = StorageUtils.getFileSize(context, inputUri)

        val outputName = when (mode) {
            CryptoMode.ENCRYPT -> "$originalName.shield"
            CryptoMode.DECRYPT -> {
                if (originalName.endsWith(".shield", ignoreCase = true)) {
                    originalName.substringBeforeLast(".shield")
                } else {
                    "${originalName}_decrypted"
                }
            }
        }

        var tempFile: File? = null

        try {
            val outputStream: OutputStream = if (outputDestinationUri != null) {
                context.contentResolver.openOutputStream(outputDestinationUri)
                    ?: throw IllegalStateException("Cannot open output stream for destination URI.")
            } else {
                val file = StorageUtils.createShareableFile(context, outputName)
                tempFile = file
                FileOutputStream(file)
            }

            val inputStream: InputStream = context.contentResolver.openInputStream(inputUri)
                ?: throw IllegalStateException("Cannot open input stream for $originalName")

            inputStream.use { inStream ->
                outputStream.use { outStream ->
                    when (mode) {
                        CryptoMode.ENCRYPT -> {
                            CryptoUtils.encryptStream(
                                inputStream = inStream,
                                outputStream = outStream,
                                passphrase = passphrase,
                                totalBytes = originalSize
                            ) { processed, total ->
                                val pct = if (total > 0) (processed.toFloat() / total.toFloat()).coerceIn(0f, 1f) else 0f
                                trySend(CryptoProgress(processed, total, pct) to null)
                            }
                        }
                        CryptoMode.DECRYPT -> {
                            CryptoUtils.decryptStream(
                                inputStream = inStream,
                                outputStream = outStream,
                                passphrase = passphrase,
                                totalBytes = originalSize
                            ) { processed, total ->
                                val pct = if (total > 0) (processed.toFloat() / total.toFloat()).coerceIn(0f, 1f) else 0f
                                trySend(CryptoProgress(processed, total, pct) to null)
                            }
                        }
                    }
                }
            }

            val finalOutputUri = outputDestinationUri ?: StorageUtils.getShareableUri(context, tempFile!!)
            val finalOutputSize = tempFile?.length() ?: StorageUtils.getFileSize(context, outputDestinationUri!!)

            val duration = System.currentTimeMillis() - startTime
            val successResult = CryptoResult.Success(
                outputUri = finalOutputUri,
                outputFileName = outputName,
                inputSizeBytes = originalSize,
                outputSizeBytes = finalOutputSize,
                durationMs = duration,
                mode = mode
            )

            send(CryptoProgress(originalSize, originalSize, 1f) to successResult)

        } catch (e: Exception) {
            tempFile?.let {
                try { it.delete() } catch (_: Exception) {}
            }
            val failure = CryptoResult.Failure(
                errorMessage = e.localizedMessage ?: "Unknown cryptographic error occurred.",
                cause = e
            )
            send(CryptoProgress(0, originalSize, 0f) to failure)
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun isEncryptedOmniShieldFile(inputUri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(inputUri)?.use {
                CryptoUtils.isOmniShieldEncrypted(it)
            } ?: false
        } catch (_: Exception) {
            false
        }
    }
}
