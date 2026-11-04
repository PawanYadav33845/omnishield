package com.omnishield.utility.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.omnishield.utility.core.metadata.MetadataUtils
import com.omnishield.utility.core.storage.StorageUtils
import com.omnishield.utility.domain.model.ExifMetadata
import com.omnishield.utility.domain.model.FileHashReport
import com.omnishield.utility.domain.model.SanitizerResult
import com.omnishield.utility.domain.repository.IMetadataRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.FileOutputStream

class MetadataRepository(
    private val context: Context
) : IMetadataRepository {

    override suspend fun inspectExif(imageUri: Uri): Result<ExifMetadata> = withContext(Dispatchers.IO) {
        try {
            val fileName = StorageUtils.getFileName(context, imageUri)
            val size = StorageUtils.getFileSize(context, imageUri)
            val inStream = context.contentResolver.openInputStream(imageUri)
                ?: return@withContext Result.failure(IllegalStateException("Cannot open stream for image."))

            val metadata = inStream.use {
                MetadataUtils.readExifMetadata(it, fileName, size)
            }
            Result.success(metadata)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun sanitizeMetadata(inputUri: Uri): SanitizerResult = withContext(Dispatchers.IO) {
        try {
            val originalName = StorageUtils.getFileName(context, inputUri)
            val originalSize = StorageUtils.getFileSize(context, inputUri)
            val extension = originalName.substringAfterLast('.', "").lowercase()
            val baseName = originalName.substringBeforeLast('.')
            val cleanName = "${baseName}_sanitized.$extension"

            val targetFile = StorageUtils.createShareableFile(context, cleanName)
            val inputStream = context.contentResolver.openInputStream(inputUri)
                ?: return@withContext SanitizerResult.Failure("Failed to open input stream.")

            val outputStream = FileOutputStream(targetFile)

            val strippedTags = inputStream.use { inStream ->
                outputStream.use { outStream ->
                    when (extension) {
                        "jpg", "jpeg" -> MetadataUtils.stripImageMetadata(inStream, outStream, Bitmap.CompressFormat.JPEG)
                        "png" -> MetadataUtils.stripImageMetadata(inStream, outStream, Bitmap.CompressFormat.PNG)
                        "webp" -> MetadataUtils.stripImageMetadata(inStream, outStream, Bitmap.CompressFormat.WEBP)
                        "pdf" -> MetadataUtils.stripDocumentMetadata(inStream, outStream)
                        else -> {
                            // Default stream copy for other documents
                            StorageUtils.copyStream(inStream, outStream)
                            0
                        }
                    }
                }
            }

            val shareUri = StorageUtils.getShareableUri(context, targetFile)
            SanitizerResult.Success(
                outputUri = shareUri,
                sanitizedFileName = cleanName,
                originalSizeBytes = originalSize,
                sanitizedSizeBytes = targetFile.length(),
                strippedTagsCount = strippedTags
            )
        } catch (e: Exception) {
            SanitizerResult.Failure(e.localizedMessage ?: "Sanitization failed.", e)
        }
    }

    override fun computeChecksums(inputUri: Uri): Flow<Pair<Float, FileHashReport?>> = channelFlow {
        val fileName = StorageUtils.getFileName(context, inputUri)
        val size = StorageUtils.getFileSize(context, inputUri)

        val inStream = context.contentResolver.openInputStream(inputUri)
            ?: throw IllegalStateException("Cannot open input stream for hashing.")

        val report = inStream.use { stream ->
            MetadataUtils.computeChecksums(stream, fileName, size) { processed, total ->
                val progress = if (total > 0) (processed.toFloat() / total.toFloat()).coerceIn(0f, 1f) else 0.5f
                trySend(progress to null)
            }
        }

        send(1f to report)
    }.flowOn(Dispatchers.IO)
}
