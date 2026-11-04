package com.omnishield.utility.core.storage

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.text.DecimalFormat

object StorageUtils {

    private const val SHARES_DIR = "omnishield_shares"
    private const val BUFFER_SIZE = 64 * 1024

    /**
     * Resolves the real display name of a file from a SAF content URI.
     */
    fun getFileName(context: Context, uri: Uri): String {
        var name = "unknown_file"
        if (uri.scheme == "content") {
            val cursor = context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index >= 0) {
                        name = it.getString(index) ?: name
                    }
                }
            }
        } else if (uri.scheme == "file") {
            name = uri.lastPathSegment ?: name
        }
        return name
    }

    /**
     * Resolves file size in bytes from a SAF content URI.
     */
    fun getFileSize(context: Context, uri: Uri): Long {
        var size = 0L
        if (uri.scheme == "content") {
            val cursor = context.contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val index = it.getColumnIndex(OpenableColumns.SIZE)
                    if (index >= 0) {
                        size = it.getLong(index)
                    }
                }
            }
        }
        return size
    }

    /**
     * Resolves MIME type from a content URI or file extension fallback.
     */
    fun getMimeType(context: Context, uri: Uri): String {
        return context.contentResolver.getType(uri) ?: when (uri.path?.substringAfterLast('.', "")?.lowercase()) {
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "webp" -> "image/webp"
            "json" -> "application/json"
            "xml" -> "application/xml"
            "yaml", "yml" -> "application/x-yaml"
            "csv" -> "text/csv"
            "pdf" -> "application/pdf"
            "shield" -> "application/octet-stream"
            else -> "application/octet-stream"
        }
    }

    /**
     * Creates a temporary file in the app's internal cache designated for secure sharing.
     */
    fun createShareableFile(context: Context, fileName: String): File {
        val shareDir = File(context.cacheDir, SHARES_DIR)
        if (!shareDir.exists()) {
            shareDir.mkdirs()
        }
        return File(shareDir, fileName)
    }

    /**
     * Generates a secure FileProvider content:// URI with read grant permissions.
     */
    fun getShareableUri(context: Context, file: File): Uri {
        val authority = "${context.packageName}.fileprovider"
        return FileProvider.getUriForFile(context, authority, file)
    }

    /**
     * Launches the system sharing sheet for a processed file.
     */
    fun shareFile(context: Context, fileUri: Uri, mimeType: String, chooserTitle: String = "Share Protected File") {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, fileUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(intent, chooserTitle).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }

    /**
     * Streams data from input to output with optional progress callback.
     */
    fun copyStream(
        inputStream: InputStream,
        outputStream: OutputStream,
        totalBytes: Long = -1L,
        onProgress: ((bytesCopied: Long, totalBytes: Long) -> Unit)? = null
    ) {
        val buffer = ByteArray(BUFFER_SIZE)
        var bytesRead: Int
        var totalCopied = 0L

        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
            outputStream.write(buffer, 0, bytesRead)
            totalCopied += bytesRead
            onProgress?.invoke(totalCopied, totalBytes)
        }
        outputStream.flush()
    }

    fun formatBytes(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
        return "${DecimalFormat("#,##0.#").format(bytes / Math.pow(1024.0, digitGroups.toDouble()))} ${units[digitGroups]}"
    }
}
