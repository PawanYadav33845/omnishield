package com.omnishield.utility.data.repository

import android.content.Context
import android.net.Uri
import com.omnishield.utility.core.converter.ConversionUtils
import com.omnishield.utility.core.storage.StorageUtils
import com.omnishield.utility.domain.model.AudioContainerInfo
import com.omnishield.utility.domain.model.BatchConversionProgress
import com.omnishield.utility.domain.model.ConversionResult
import com.omnishield.utility.domain.model.ConvertedItem
import com.omnishield.utility.domain.model.DataFormat
import com.omnishield.utility.domain.model.ImageFormat
import com.omnishield.utility.domain.repository.IFileConverterRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.FileOutputStream

class FileConverterRepository(
    private val context: Context
) : IFileConverterRepository {

    override fun convertImages(
        inputUris: List<Uri>,
        targetFormat: ImageFormat,
        quality: Int
    ): Flow<Pair<BatchConversionProgress, ConversionResult?>> = flow {
        val total = inputUris.size
        val startTime = System.currentTimeMillis()
        val convertedList = mutableListOf<ConvertedItem>()

        for (index in inputUris.indices) {
            val uri = inputUris[index]
            val originalName = StorageUtils.getFileName(context, uri)
            val originalSize = StorageUtils.getFileSize(context, uri)
            val baseName = originalName.substringBeforeLast('.')
            val newFileName = "${baseName}_converted.${targetFormat.extension}"

            val progress = BatchConversionProgress(
                currentItemIndex = index + 1,
                totalItems = total,
                currentItemName = originalName,
                overallPercentage = (index.toFloat() / total.toFloat())
            )
            emit(progress to null)

            val shareFile = StorageUtils.createShareableFile(context, newFileName)
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: throw IllegalStateException("Cannot open input stream for $originalName")

            val outputStream = FileOutputStream(shareFile)

            try {
                inputStream.use { inStream ->
                    outputStream.use { outStream ->
                        ConversionUtils.convertImage(inStream, outStream, targetFormat, quality)
                    }
                }
                val shareUri = StorageUtils.getShareableUri(context, shareFile)
                convertedList.add(
                    ConvertedItem(
                        outputUri = shareUri,
                        outputFileName = newFileName,
                        originalSizeBytes = originalSize,
                        convertedSizeBytes = shareFile.length(),
                        mimeType = targetFormat.mimeType
                    )
                )
            } catch (e: Exception) {
                emit(
                    progress to ConversionResult.Failure(
                        errorMessage = "Error converting $originalName: ${e.localizedMessage}",
                        cause = e
                    )
                )
                return@flow
            }
        }

        val totalDuration = System.currentTimeMillis() - startTime
        val finalProgress = BatchConversionProgress(
            currentItemIndex = total,
            totalItems = total,
            currentItemName = "Completed",
            overallPercentage = 1f
        )
        emit(finalProgress to ConversionResult.Success(convertedList, totalDuration))
    }.flowOn(Dispatchers.IO)

    override suspend fun convertData(
        inputUri: Uri,
        sourceFormat: DataFormat,
        targetFormat: DataFormat
    ): Result<Uri> = withContext(Dispatchers.IO) {
        try {
            val inputName = StorageUtils.getFileName(context, inputUri)
            val rawText = context.contentResolver.openInputStream(inputUri)?.use {
                it.bufferedReader().readText()
            } ?: return@withContext Result.failure(IllegalStateException("Unable to read data file."))

            val convertedText = when (sourceFormat to targetFormat) {
                DataFormat.JSON to DataFormat.XML -> ConversionUtils.jsonToXml(rawText)
                DataFormat.XML to DataFormat.JSON -> ConversionUtils.xmlToJson(rawText)
                DataFormat.JSON to DataFormat.YAML -> ConversionUtils.jsonToYaml(rawText)
                DataFormat.YAML to DataFormat.JSON -> ConversionUtils.yamlToJson(rawText)
                DataFormat.CSV to DataFormat.JSON -> ConversionUtils.csvToJson(rawText)
                DataFormat.JSON to DataFormat.CSV -> ConversionUtils.jsonToCsv(rawText)
                DataFormat.CSV to DataFormat.XML -> {
                    val intermediateJson = ConversionUtils.csvToJson(rawText)
                    ConversionUtils.jsonToXml(intermediateJson)
                }
                DataFormat.XML to DataFormat.CSV -> {
                    val intermediateJson = ConversionUtils.xmlToJson(rawText)
                    ConversionUtils.jsonToCsv(intermediateJson)
                }
                else -> throw UnsupportedOperationException("Conversion from ${sourceFormat.name} to ${targetFormat.name} is not directly supported.")
            }

            val baseName = inputName.substringBeforeLast('.')
            val newFileName = "${baseName}_converted.${targetFormat.extension}"
            val targetFile = StorageUtils.createShareableFile(context, newFileName)
            targetFile.writeText(convertedText)

            val shareUri = StorageUtils.getShareableUri(context, targetFile)
            Result.success(shareUri)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun inspectAudio(inputUri: Uri): Result<AudioContainerInfo> = withContext(Dispatchers.IO) {
        try {
            val name = StorageUtils.getFileName(context, inputUri)
            val size = StorageUtils.getFileSize(context, inputUri)
            val info = ConversionUtils.extractAudioMetadata(context, inputUri, name, size)
            Result.success(info)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun compressFileLossless(inputUri: Uri): Result<Uri> = withContext(Dispatchers.IO) {
        try {
            val fileName = StorageUtils.getFileName(context, inputUri)
            val zipFileName = "$fileName.zip"
            val targetFile = StorageUtils.createShareableFile(context, zipFileName)
            val inputStream = context.contentResolver.openInputStream(inputUri)
                ?: return@withContext Result.failure(IllegalStateException("Cannot open input stream."))

            val outputStream = FileOutputStream(targetFile)
            inputStream.use { inStream ->
                outputStream.use { outStream ->
                    ConversionUtils.compressLosslessZip(inStream, outStream, fileName)
                }
            }

            val shareUri = StorageUtils.getShareableUri(context, targetFile)
            Result.success(shareUri)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
