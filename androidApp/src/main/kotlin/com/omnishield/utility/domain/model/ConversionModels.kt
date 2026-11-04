package com.omnishield.utility.domain.model

import android.net.Uri

enum class ImageFormat(val extension: String, val mimeType: String, val displayName: String) {
    JPEG("jpg", "image/jpeg", "JPEG (.jpg)"),
    PNG("png", "image/png", "PNG (.png)"),
    WEBP_LOSSY("webp", "image/webp", "WebP Lossy (.webp)"),
    WEBP_LOSSLESS("webp", "image/webp", "WebP Lossless (.webp)")
}

enum class DataFormat(val extension: String, val mimeType: String, val displayName: String) {
    JSON("json", "application/json", "JSON (.json)"),
    XML("xml", "application/xml", "XML (.xml)"),
    YAML("yaml", "application/x-yaml", "YAML (.yaml)"),
    CSV("csv", "text/csv", "CSV (.csv)")
}

data class ConvertedItem(
    val outputUri: Uri,
    val outputFileName: String,
    val originalSizeBytes: Long,
    val convertedSizeBytes: Long,
    val mimeType: String
)

data class BatchConversionProgress(
    val currentItemIndex: Int = 0,
    val totalItems: Int = 0,
    val currentItemName: String = "",
    val overallPercentage: Float = 0f
)

sealed interface ConversionResult {
    data class Success(
        val convertedItems: List<ConvertedItem>,
        val durationMs: Long
    ) : ConversionResult

    data class Failure(
        val errorMessage: String,
        val cause: Throwable? = null
    ) : ConversionResult
}

data class AudioContainerInfo(
    val fileName: String,
    val mimeType: String,
    val durationMs: Long,
    val formattedDuration: String,
    val bitrateKbps: Int,
    val sampleRateHz: Int,
    val channels: Int,
    val title: String?,
    val artist: String?,
    val album: String?,
    val genre: String?,
    val sizeBytes: Long
)
