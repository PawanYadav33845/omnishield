package com.omnishield.utility.domain.model

import android.net.Uri

data class GpsData(
    val latitude: Double,
    val longitude: Double,
    val altitudeMeters: Double?,
    val formattedCoordinates: String,
    val mapsDeepLink: String
)

data class ExifMetadata(
    val fileName: String,
    val fileSizeFormatted: String,
    val cameraMake: String?,
    val cameraModel: String?,
    val software: String?,
    val dateTimeOriginal: String?,
    val imageWidth: Int?,
    val imageHeight: Int?,
    val orientation: String?,
    val iso: String?,
    val exposureTime: String?,
    val fNumber: String?,
    val focalLength: String?,
    val flash: String?,
    val whiteBalance: String?,
    val gpsData: GpsData?,
    val rawTags: Map<String, String>
)

data class FileHashReport(
    val fileName: String,
    val fileSizeBytes: Long,
    val md5: String,
    val sha1: String,
    val sha256: String,
    val durationMs: Long
)

sealed interface SanitizerResult {
    data class Success(
        val outputUri: Uri,
        val sanitizedFileName: String,
        val originalSizeBytes: Long,
        val sanitizedSizeBytes: Long,
        val strippedTagsCount: Int
    ) : SanitizerResult

    data class Failure(
        val errorMessage: String,
        val cause: Throwable? = null
    ) : SanitizerResult
}
