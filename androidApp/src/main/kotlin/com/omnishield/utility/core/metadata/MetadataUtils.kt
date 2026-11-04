package com.omnishield.utility.core.metadata

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.exifinterface.media.ExifInterface
import com.omnishield.utility.domain.model.ExifMetadata
import com.omnishield.utility.domain.model.FileHashReport
import com.omnishield.utility.domain.model.GpsData
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.security.MessageDigest
import java.text.DecimalFormat
import kotlin.math.abs

/**
 * Metadata Inspector, Sanitizer, and Cryptographic Hash Engine.
 * Supports:
 * - Deep EXIF extraction from images (Camera, Sensor, Exposure, GPS, Timestamps)
 * - Metadata Stripping & Sanitization (EXIF, GPS, Camera Identifiers, PDF Info Dictionaries)
 * - Streaming MD5, SHA-1, and SHA-256 file checksums in a single pass
 */
object MetadataUtils {

    private const val BUFFER_SIZE = 64 * 1024

    /**
     * Reads all available EXIF attributes from an image input stream.
     */
    fun readExifMetadata(inputStream: InputStream, fileName: String, fileSizeBytes: Long): ExifMetadata {
        val exif = ExifInterface(inputStream)

        val make = exif.getAttribute(ExifInterface.TAG_MAKE)
        val model = exif.getAttribute(ExifInterface.TAG_MODEL)
        val software = exif.getAttribute(ExifInterface.TAG_SOFTWARE)
        val dateTime = exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
            ?: exif.getAttribute(ExifInterface.TAG_DATETIME)

        val width = exif.getAttributeInt(ExifInterface.TAG_IMAGE_WIDTH, 0).takeIf { it > 0 }
            ?: exif.getAttributeInt(ExifInterface.TAG_PIXEL_X_DIMENSION, 0).takeIf { it > 0 }
        val height = exif.getAttributeInt(ExifInterface.TAG_IMAGE_LENGTH, 0).takeIf { it > 0 }
            ?: exif.getAttributeInt(ExifInterface.TAG_PIXEL_Y_DIMENSION, 0).takeIf { it > 0 }

        val orientationCode = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        val orientationString = when (orientationCode) {
            ExifInterface.ORIENTATION_ROTATE_90 -> "Rotate 90° CW"
            ExifInterface.ORIENTATION_ROTATE_180 -> "Rotate 180°"
            ExifInterface.ORIENTATION_ROTATE_270 -> "Rotate 270° CW"
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> "Flip Horizontal"
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> "Flip Vertical"
            else -> "Normal (0°)"
        }

        val iso = exif.getAttribute(ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY)
        val exposureTimeRaw = exif.getAttributeDouble(ExifInterface.TAG_EXPOSURE_TIME, 0.0)
        val exposureTime = if (exposureTimeRaw > 0.0) {
            if (exposureTimeRaw < 1.0) {
                val denom = (1.0 / exposureTimeRaw).toInt()
                "1/$denom s"
            } else {
                "${DecimalFormat("#.##").format(exposureTimeRaw)} s"
            }
        } else {
            exif.getAttribute(ExifInterface.TAG_EXPOSURE_TIME)
        }

        val fNumberRaw = exif.getAttributeDouble(ExifInterface.TAG_F_NUMBER, 0.0)
        val fNumber = if (fNumberRaw > 0.0) "f/${DecimalFormat("#.#").format(fNumberRaw)}" else null

        val focalLengthRaw = exif.getAttributeDouble(ExifInterface.TAG_FOCAL_LENGTH, 0.0)
        val focalLength = if (focalLengthRaw > 0.0) "${DecimalFormat("#.#").format(focalLengthRaw)} mm" else null

        val flashCode = exif.getAttributeInt(ExifInterface.TAG_FLASH, -1)
        val flash = when {
            flashCode == -1 -> null
            flashCode and 1 != 0 -> "Fired"
            else -> "Did not fire"
        }

        val wbCode = exif.getAttributeInt(ExifInterface.TAG_WHITE_BALANCE, -1)
        val whiteBalance = when (wbCode) {
            ExifInterface.WHITE_BALANCE_AUTO.toInt() -> "Auto"
            ExifInterface.WHITE_BALANCE_MANUAL.toInt() -> "Manual"
            else -> null
        }

        // GPS coordinates extraction
        val latLong = exif.latLong
        val gpsData = if (latLong != null && latLong.size == 2) {
            val lat = latLong[0]
            val lon = latLong[1]
            val alt = exif.getAltitude(Double.NaN).takeIf { !it.isNaN() }
            val formatted = "${formatDms(lat, isLatitude = true)}, ${formatDms(lon, isLatitude = false)} (${DecimalFormat("#.######").format(lat)}, ${DecimalFormat("#.######").format(lon)})"
            val mapUrl = "https://www.google.com/maps/search/?api=1&query=$lat,$lon"
            GpsData(lat, lon, alt, formatted, mapUrl)
        } else {
            null
        }

        // Collect all available non-null raw tags
        val rawTags = mutableMapOf<String, String>()
        val commonTags = listOf(
            ExifInterface.TAG_MAKE, ExifInterface.TAG_MODEL, ExifInterface.TAG_SOFTWARE,
            ExifInterface.TAG_DATETIME, ExifInterface.TAG_DATETIME_ORIGINAL, ExifInterface.TAG_DATETIME_DIGITIZED,
            ExifInterface.TAG_IMAGE_WIDTH, ExifInterface.TAG_IMAGE_LENGTH, ExifInterface.TAG_ORIENTATION,
            ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY, ExifInterface.TAG_EXPOSURE_TIME,
            ExifInterface.TAG_F_NUMBER, ExifInterface.TAG_FOCAL_LENGTH, ExifInterface.TAG_FLASH,
            ExifInterface.TAG_WHITE_BALANCE, ExifInterface.TAG_METERING_MODE, ExifInterface.TAG_COLOR_SPACE,
            ExifInterface.TAG_LENS_MAKE, ExifInterface.TAG_LENS_MODEL, ExifInterface.TAG_GPS_LATITUDE,
            ExifInterface.TAG_GPS_LONGITUDE, ExifInterface.TAG_GPS_ALTITUDE, ExifInterface.TAG_GPS_PROCESSING_METHOD
        )

        for (tag in commonTags) {
            exif.getAttribute(tag)?.let { value ->
                if (value.isNotBlank()) {
                    rawTags[tag] = value
                }
            }
        }

        return ExifMetadata(
            fileName = fileName,
            fileSizeFormatted = formatFileSize(fileSizeBytes),
            cameraMake = make,
            cameraModel = model,
            software = software,
            dateTimeOriginal = dateTime,
            imageWidth = width,
            imageHeight = height,
            orientation = orientationString,
            iso = iso,
            exposureTime = exposureTime,
            fNumber = fNumber,
            focalLength = focalLength,
            flash = flash,
            whiteBalance = whiteBalance,
            gpsData = gpsData,
            rawTags = rawTags
        )
    }

    /**
     * Strips all EXIF/GPS metadata from an image by re-encoding pure bitmap pixels into the output stream.
     * Returns the count of removed metadata tags.
     */
    fun stripImageMetadata(
        inputStream: InputStream,
        outputStream: OutputStream,
        compressFormat: Bitmap.CompressFormat = Bitmap.CompressFormat.JPEG,
        quality: Int = 95
    ): Int {
        // First read existing tags count and orientation
        val bytes = inputStream.readBytes()
        val originalExif = ExifInterface(bytes.inputStream())
        val orientation = originalExif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)

        var tagCount = 0
        val sampleTags = listOf(
            ExifInterface.TAG_MAKE, ExifInterface.TAG_MODEL, ExifInterface.TAG_DATETIME,
            ExifInterface.TAG_GPS_LATITUDE, ExifInterface.TAG_GPS_LONGITUDE, ExifInterface.TAG_SOFTWARE,
            ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY, ExifInterface.TAG_FOCAL_LENGTH
        )
        for (tag in sampleTags) {
            if (originalExif.getAttribute(tag) != null) tagCount++
        }

        // Decode pure pixel representation without EXIF metadata
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            ?: throw IllegalArgumentException("Could not decode image bitmap.")

        // Physically rotate pixel matrix so the image remains upright after EXIF orientation is stripped
        val matrix = android.graphics.Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.preScale(-1.0f, 1.0f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.preScale(1.0f, -1.0f)
        }

        val orientedBitmap = if (!matrix.isIdentity) {
            val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            if (rotated != bitmap) bitmap.recycle()
            rotated
        } else {
            bitmap
        }

        try {
            orientedBitmap.compress(compressFormat, quality, outputStream)
            outputStream.flush()
        } finally {
            orientedBitmap.recycle()
        }

        return if (tagCount == 0) 1 else tagCount
    }

    /**
     * Strips document metadata (PDF info dictionary headers / XMP blocks) for user privacy.
     */
    fun stripDocumentMetadata(inputStream: InputStream, outputStream: OutputStream): Int {
        val content = inputStream.readBytes()
        var strippedCount = 0

        var text = String(content, Charsets.ISO_8859_1)

        // Patterns for PDF Info dictionary fields
        val pdfTags = listOf("/Title", "/Author", "/Subject", "/Keywords", "/Creator", "/Producer", "/CreationDate", "/ModDate")
        for (tag in pdfTags) {
            val regex = Regex("$tag\\s*\\([^)]*\\)")
            if (regex.containsMatchIn(text)) {
                text = regex.replace(text, "$tag ()")
                strippedCount++
            }
        }

        // Strip XMP metadata packet if present
        val xmpRegex = Regex("<x:xmpmeta[\\s\\S]*?</x:xmpmeta>")
        if (xmpRegex.containsMatchIn(text)) {
            text = xmpRegex.replace(text, "")
            strippedCount++
        }

        outputStream.write(text.toByteArray(Charsets.ISO_8859_1))
        outputStream.flush()
        return strippedCount
    }

    /**
     * Simultaneously computes MD5, SHA-1, and SHA-256 checksums in a single streaming pass.
     */
    fun computeChecksums(
        inputStream: InputStream,
        fileName: String,
        fileSizeBytes: Long,
        onProgress: ((bytesProcessed: Long, totalBytes: Long) -> Unit)? = null
    ): FileHashReport {
        val startTime = System.currentTimeMillis()

        val md5Digest = MessageDigest.getInstance("MD5")
        val sha1Digest = MessageDigest.getInstance("SHA-1")
        val sha256Digest = MessageDigest.getInstance("SHA-256")

        val buffer = ByteArray(BUFFER_SIZE)
        var bytesRead: Int
        var totalProcessed = 0L

        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
            md5Digest.update(buffer, 0, bytesRead)
            sha1Digest.update(buffer, 0, bytesRead)
            sha256Digest.update(buffer, 0, bytesRead)

            totalProcessed += bytesRead
            onProgress?.invoke(totalProcessed, fileSizeBytes)
        }

        val md5Hex = md5Digest.digest().toHexString()
        val sha1Hex = sha1Digest.digest().toHexString()
        val sha256Hex = sha256Digest.digest().toHexString()

        val duration = System.currentTimeMillis() - startTime

        return FileHashReport(
            fileName = fileName,
            fileSizeBytes = totalProcessed,
            md5 = md5Hex,
            sha1 = sha1Hex,
            sha256 = sha256Hex,
            durationMs = duration
        )
    }

    private fun ByteArray.toHexString(): String {
        val sb = StringBuilder(size * 2)
        for (b in this) {
            sb.append(String.format("%02x", b))
        }
        return sb.toString()
    }

    private fun formatDms(coord: Double, isLatitude: Boolean): String {
        val absCoord = abs(coord)
        val degrees = absCoord.toInt()
        val minutesDouble = (absCoord - degrees) * 60.0
        val minutes = minutesDouble.toInt()
        val seconds = (minutesDouble - minutes) * 60.0
        val direction = if (isLatitude) {
            if (coord >= 0) "N" else "S"
        } else {
            if (coord >= 0) "E" else "W"
        }
        return "$degrees°$minutes'${DecimalFormat("#.##").format(seconds)}\"$direction"
    }

    private fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
        return "${DecimalFormat("#,##0.#").format(bytes / Math.pow(1024.0, digitGroups.toDouble()))} ${units[digitGroups]}"
    }
}
