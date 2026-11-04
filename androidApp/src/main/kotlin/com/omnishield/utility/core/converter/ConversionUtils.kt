package com.omnishield.utility.core.converter

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import com.omnishield.utility.domain.model.AudioContainerInfo
import com.omnishield.utility.domain.model.ImageFormat
import org.json.JSONArray
import org.json.JSONObject
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.io.StringReader
import java.io.StringWriter
import java.text.DecimalFormat
import java.util.zip.Deflater
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Universal File Conversion Engine for:
 * 1. Images: PNG, JPEG, WebP (lossy/lossless), HEIC -> JPEG/PNG
 * 2. Data / Structured formats: JSON <-> XML <-> YAML, CSV <-> JSON
 * 3. Media: Audio container metadata inspection
 */
object ConversionUtils {

    // ---------------------------------------------------------------------------------------------
    // Image Conversion Engine
    // ---------------------------------------------------------------------------------------------

    /**
     * Converts an image stream to the desired target format with custom compression quality.
     */
    fun convertImage(
        inputStream: InputStream,
        outputStream: OutputStream,
        targetFormat: ImageFormat,
        quality: Int = 90
    ) {
        val bitmap = BitmapFactory.decodeStream(inputStream)
            ?: throw IllegalArgumentException("Failed to decode image. Format may be unsupported or corrupted.")

        val compressFormat = when (targetFormat) {
            ImageFormat.JPEG -> Bitmap.CompressFormat.JPEG
            ImageFormat.PNG -> Bitmap.CompressFormat.PNG
            ImageFormat.WEBP_LOSSY -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    Bitmap.CompressFormat.WEBP_LOSSY
                } else {
                    @Suppress("DEPRECATION")
                    Bitmap.CompressFormat.WEBP
                }
            }
            ImageFormat.WEBP_LOSSLESS -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    Bitmap.CompressFormat.WEBP_LOSSLESS
                } else {
                    @Suppress("DEPRECATION")
                    Bitmap.CompressFormat.WEBP
                }
            }
        }

        try {
            val clampedQuality = quality.coerceIn(1, 100)
            val success = bitmap.compress(compressFormat, clampedQuality, outputStream)
            if (!success) {
                throw RuntimeException("Bitmap compression failed for format ${targetFormat.displayName}")
            }
            outputStream.flush()
        } finally {
            bitmap.recycle()
        }
    }

    /**
     * Intelligently compresses an image to fit strictly under a given target size in bytes.
     * Uses adaptive quality stepped compression followed by proportional downscaling if necessary.
     */
    fun compressImageToTargetSize(
        inputStream: InputStream,
        outputStream: OutputStream,
        targetSizeBytes: Long,
        targetFormat: ImageFormat = ImageFormat.JPEG
    ): Long {
        val originalBytes = inputStream.readBytes()
        var currentBitmap = BitmapFactory.decodeByteArray(originalBytes, 0, originalBytes.size)
            ?: throw IllegalArgumentException("Failed to decode image bitmap.")

        val compressFormat = when (targetFormat) {
            ImageFormat.JPEG -> Bitmap.CompressFormat.JPEG
            ImageFormat.PNG -> Bitmap.CompressFormat.PNG
            ImageFormat.WEBP_LOSSY -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) Bitmap.CompressFormat.WEBP_LOSSY else @Suppress("DEPRECATION") Bitmap.CompressFormat.WEBP
            ImageFormat.WEBP_LOSSLESS -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) Bitmap.CompressFormat.WEBP_LOSSLESS else @Suppress("DEPRECATION") Bitmap.CompressFormat.WEBP
        }

        // Binary search for optimal quality step to minimize compression iterations
        var lowQuality = 10
        var highQuality = 95
        var bestQuality = 85
        var baos = ByteArrayOutputStream()

        while (lowQuality <= highQuality) {
            val midQuality = (lowQuality + highQuality) / 2
            baos.reset()
            currentBitmap.compress(compressFormat, midQuality, baos)
            if (baos.size() <= targetSizeBytes) {
                bestQuality = midQuality
                lowQuality = midQuality + 5 // Try higher quality
            } else {
                highQuality = midQuality - 5
            }
        }

        // Apply best quality result
        baos.reset()
        currentBitmap.compress(compressFormat, bestQuality, baos)

        // Downscale dimensions only if lowest quality still exceeds budget
        while (baos.size() > targetSizeBytes && (currentBitmap.width > 240 && currentBitmap.height > 240)) {
            val newWidth = (currentBitmap.width * 0.75f).toInt()
            val newHeight = (currentBitmap.height * 0.75f).toInt()
            val scaled = Bitmap.createScaledBitmap(currentBitmap, newWidth, newHeight, true)
            if (scaled != currentBitmap) {
                currentBitmap.recycle()
                currentBitmap = scaled
            }
            baos.reset()
            currentBitmap.compress(compressFormat, bestQuality, baos)
        }

        val resultBytes = baos.toByteArray()
        outputStream.write(resultBytes)
        outputStream.flush()
        currentBitmap.recycle()
        return resultBytes.size.toLong()
    }

    /**
     * Resizes an image by dimension scale factor with bilinear filtering to maintain sharp detail.
     */
    fun resizeImageDimensions(
        inputStream: InputStream,
        outputStream: OutputStream,
        scaleFactor: Float,
        targetFormat: ImageFormat = ImageFormat.JPEG,
        quality: Int = 90
    ) {
        val originalBitmap = BitmapFactory.decodeStream(inputStream)
            ?: throw IllegalArgumentException("Failed to decode image.")

        val targetWidth = (originalBitmap.width * scaleFactor).toInt().coerceAtLeast(1)
        val targetHeight = (originalBitmap.height * scaleFactor).toInt().coerceAtLeast(1)

        val scaledBitmap = Bitmap.createScaledBitmap(originalBitmap, targetWidth, targetHeight, true)

        val compressFormat = when (targetFormat) {
            ImageFormat.JPEG -> Bitmap.CompressFormat.JPEG
            ImageFormat.PNG -> Bitmap.CompressFormat.PNG
            ImageFormat.WEBP_LOSSY -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) Bitmap.CompressFormat.WEBP_LOSSY else @Suppress("DEPRECATION") Bitmap.CompressFormat.WEBP
            ImageFormat.WEBP_LOSSLESS -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) Bitmap.CompressFormat.WEBP_LOSSLESS else @Suppress("DEPRECATION") Bitmap.CompressFormat.WEBP
        }

        try {
            scaledBitmap.compress(compressFormat, quality.coerceIn(1, 100), outputStream)
            outputStream.flush()
        } finally {
            if (scaledBitmap != originalBitmap) {
                scaledBitmap.recycle()
            }
            originalBitmap.recycle()
        }
    }

    /**
     * Performs lossless file compression by wrapping the input stream in a high-efficiency ZIP archive using Deflater.BEST_COMPRESSION.
     */
    fun compressLosslessZip(
        inputStream: InputStream,
        outputStream: OutputStream,
        entryFileName: String
    ): Long {
        val zipOut = ZipOutputStream(outputStream).apply {
            setLevel(Deflater.BEST_COMPRESSION)
        }
        val entry = ZipEntry(entryFileName)
        zipOut.putNextEntry(entry)

        val buffer = ByteArray(64 * 1024)
        var bytesRead: Int
        var total = 0L

        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
            zipOut.write(buffer, 0, bytesRead)
            total += bytesRead
        }

        zipOut.closeEntry()
        zipOut.finish()
        zipOut.flush()
        return total
    }

    // ---------------------------------------------------------------------------------------------
    // Data Format Conversions (JSON <-> XML <-> YAML <-> CSV)
    // ---------------------------------------------------------------------------------------------

    /**
     * Converts a JSON string to pretty-printed XML.
     */
    fun jsonToXml(jsonStr: String, rootElement: String = "root"): String {
        val trimmed = jsonStr.trim()
        val sb = StringBuilder()
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        sb.append("<$rootElement>\n")

        if (trimmed.startsWith("{")) {
            val obj = JSONObject(trimmed)
            appendJsonObjectToXml(obj, sb, 1)
        } else if (trimmed.startsWith("[")) {
            val arr = JSONArray(trimmed)
            appendJsonArrayToXml(arr, "item", sb, 1)
        } else {
            throw IllegalArgumentException("Invalid JSON: Root must be an object or array.")
        }

        sb.append("</$rootElement>")
        return sb.toString()
    }

    private fun appendJsonObjectToXml(obj: JSONObject, sb: StringBuilder, indentLevel: Int) {
        val indent = "  ".repeat(indentLevel)
        val keys = obj.keys()
        while (keys.hasNext()) {
            val rawKey = keys.next()
            val validKey = sanitizeXmlTag(rawKey)
            val value = obj.get(rawKey)
            when (value) {
                is JSONObject -> {
                    sb.append("$indent<$validKey>\n")
                    appendJsonObjectToXml(value, sb, indentLevel + 1)
                    sb.append("$indent</$validKey>\n")
                }
                is JSONArray -> {
                    sb.append("$indent<$validKey>\n")
                    appendJsonArrayToXml(value, "item", sb, indentLevel + 1)
                    sb.append("$indent</$validKey>\n")
                }
                JSONObject.NULL -> {
                    sb.append("$indent<$validKey/>\n")
                }
                else -> {
                    val escaped = escapeXml(value.toString())
                    sb.append("$indent<$validKey>$escaped</$validKey>\n")
                }
            }
        }
    }

    private fun appendJsonArrayToXml(arr: JSONArray, itemTag: String, sb: StringBuilder, indentLevel: Int) {
        val indent = "  ".repeat(indentLevel)
        for (i in 0 until arr.length()) {
            val item = arr.get(i)
            when (item) {
                is JSONObject -> {
                    sb.append("$indent<$itemTag>\n")
                    appendJsonObjectToXml(item, sb, indentLevel + 1)
                    sb.append("$indent</$itemTag>\n")
                }
                is JSONArray -> {
                    sb.append("$indent<$itemTag>\n")
                    appendJsonArrayToXml(item, "nested", sb, indentLevel + 1)
                    sb.append("$indent</$itemTag>\n")
                }
                else -> {
                    val escaped = escapeXml(item.toString())
                    sb.append("$indent<$itemTag>$escaped</$itemTag>\n")
                }
            }
        }
    }

    /**
     * Converts an XML string into a JSON string using XmlPullParser.
     */
    fun xmlToJson(xmlStr: String): String {
        val factory = XmlPullParserFactory.newInstance()
        factory.isNamespaceAware = false
        val parser = factory.newPullParser()
        parser.setInput(StringReader(xmlStr))

        var eventType = parser.eventType
        val rootObj = JSONObject()
        var currentTag = ""
        val tagStack = java.util.ArrayDeque<JSONObject>()
        tagStack.push(rootObj)

        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    val tagName = parser.name
                    val newObj = JSONObject()

                    // Include attributes if present
                    for (i in 0 until parser.attributeCount) {
                        newObj.put("@${parser.getAttributeName(i)}", parser.getAttributeValue(i))
                    }

                    val parent = tagStack.peek()
                    if (parent != null) {
                        if (parent.has(tagName)) {
                            val existing = parent.get(tagName)
                            if (existing is JSONArray) {
                                existing.put(newObj)
                            } else {
                                val arr = JSONArray()
                                arr.put(existing)
                                arr.put(newObj)
                                parent.put(tagName, arr)
                            }
                        } else {
                            parent.put(tagName, newObj)
                        }
                    }
                    tagStack.push(newObj)
                    currentTag = tagName
                }
                XmlPullParser.TEXT -> {
                    val text = parser.text?.trim()
                    if (!text.isNullOrEmpty()) {
                        val current = tagStack.peek()
                        if (current != null && current.length() == 0) {
                            // If it has no children yet, assign value
                            val parent = if (tagStack.size > 1) {
                                val popped = tagStack.pop()
                                val p = tagStack.peek()
                                tagStack.push(popped)
                                p
                            } else null

                            if (parent != null && parent.has(currentTag)) {
                                parent.put(currentTag, text)
                            }
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (tagStack.size > 1) {
                        tagStack.pop()
                    }
                }
            }
            eventType = parser.next()
        }

        return rootObj.toString(2)
    }

    /**
     * Converts JSON to YAML format.
     */
    fun jsonToYaml(jsonStr: String): String {
        val trimmed = jsonStr.trim()
        val writer = StringWriter()
        if (trimmed.startsWith("{")) {
            val obj = JSONObject(trimmed)
            writeJsonObjectToYaml(obj, writer, 0)
        } else if (trimmed.startsWith("[")) {
            val arr = JSONArray(trimmed)
            writeJsonArrayToYaml(arr, writer, 0)
        } else {
            throw IllegalArgumentException("Invalid JSON for YAML conversion.")
        }
        return writer.toString()
    }

    private fun writeJsonObjectToYaml(obj: JSONObject, writer: StringWriter, indent: Int) {
        val prefix = "  ".repeat(indent)
        val keys = obj.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val value = obj.get(key)
            when (value) {
                is JSONObject -> {
                    writer.write("$prefix$key:\n")
                    writeJsonObjectToYaml(value, writer, indent + 1)
                }
                is JSONArray -> {
                    writer.write("$prefix$key:\n")
                    writeJsonArrayToYaml(value, writer, indent + 1)
                }
                else -> {
                    val yamlVal = formatYamlScalar(value.toString())
                    writer.write("$prefix$key: $yamlVal\n")
                }
            }
        }
    }

    private fun writeJsonArrayToYaml(arr: JSONArray, writer: StringWriter, indent: Int) {
        val prefix = "  ".repeat(indent)
        for (i in 0 until arr.length()) {
            val item = arr.get(i)
            when (item) {
                is JSONObject -> {
                    writer.write("$prefix-\n")
                    writeJsonObjectToYaml(item, writer, indent + 1)
                }
                is JSONArray -> {
                    writer.write("$prefix-\n")
                    writeJsonArrayToYaml(item, writer, indent + 1)
                }
                else -> {
                    val yamlVal = formatYamlScalar(item.toString())
                    writer.write("$prefix- $yamlVal\n")
                }
            }
        }
    }

    /**
     * Converts basic YAML to JSON format.
     */
    fun yamlToJson(yamlStr: String): String {
        val root = JSONObject()
        val lines = yamlStr.lines().filter { it.isNotBlank() && !it.trimStart().startsWith("#") }
        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.contains(":")) {
                val parts = trimmed.split(":", limit = 2)
                val key = parts[0].trim().removePrefix("-").trim()
                val value = parts[1].trim().removeSurrounding("\"").removeSurrounding("'")
                if (key.isNotEmpty()) {
                    root.put(key, value)
                }
            }
        }
        return root.toString(2)
    }

    /**
     * Converts CSV to a JSON Array of objects.
     */
    fun csvToJson(csvStr: String): String {
        val lines = csvStr.lines().filter { it.isNotBlank() }
        if (lines.isEmpty()) return "[]"

        val headers = parseCsvLine(lines[0])
        val jsonArray = JSONArray()

        for (i in 1 until lines.size) {
            val values = parseCsvLine(lines[i])
            val obj = JSONObject()
            for (h in headers.indices) {
                val headerName = headers[h]
                val valStr = if (h < values.size) values[h] else ""
                obj.put(headerName, valStr)
            }
            jsonArray.put(obj)
        }

        return jsonArray.toString(2)
    }

    /**
     * Converts a JSON Array of objects into CSV format.
     */
    fun jsonToCsv(jsonStr: String): String {
        val trimmed = jsonStr.trim()
        val array = if (trimmed.startsWith("[")) {
            JSONArray(trimmed)
        } else if (trimmed.startsWith("{")) {
            val obj = JSONObject(trimmed)
            JSONArray().put(obj)
        } else {
            throw IllegalArgumentException("Expected JSON array or object for CSV conversion.")
        }

        if (array.length() == 0) return ""

        val headers = linkedSetOf<String>()
        for (i in 0 until array.length()) {
            val item = array.optJSONObject(i)
            item?.keys()?.forEach { headers.add(it) }
        }

        val sb = StringBuilder()
        sb.append(headers.joinToString(",") { escapeCsv(it) }).append("\n")

        for (i in 0 until array.length()) {
            val item = array.optJSONObject(i) ?: JSONObject()
            val row = headers.map { header ->
                val value = item.opt(header)?.toString() ?: ""
                escapeCsv(value)
            }
            sb.append(row.joinToString(",")).append("\n")
        }

        return sb.toString()
    }

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val sb = java.lang.StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            if (c == '\"') {
                if (inQuotes && i + 1 < line.length && line[i + 1] == '\"') {
                    sb.append('\"')
                    i++
                } else {
                    inQuotes = !inQuotes
                }
            } else if (c == ',' && !inQuotes) {
                result.add(sb.toString().trim())
                sb.setLength(0)
            } else {
                sb.append(c)
            }
            i++
        }
        result.add(sb.toString().trim())
        return result
    }

    private fun escapeCsv(value: String): String {
        return if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else {
            value
        }
    }

    private fun sanitizeXmlTag(tag: String): String {
        val cleaned = tag.replace("[^a-zA-Z0-9_.-]".toRegex(), "_")
        return if (cleaned.isEmpty() || !cleaned[0].isLetter() && cleaned[0] != '_') {
            "_$cleaned"
        } else {
            cleaned
        }
    }

    private fun escapeXml(str: String): String {
        return str.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }

    private fun formatYamlScalar(scalar: String): String {
        return if (scalar.contains(":") || scalar.contains("\n") || scalar.startsWith("-") || scalar.contains("#")) {
            "\"${scalar.replace("\"", "\\\"")}\""
        } else {
            scalar
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Audio / Media Container Inspector
    // ---------------------------------------------------------------------------------------------

    /**
     * Inspects media files to extract audio container metadata.
     */
    fun extractAudioMetadata(context: Context, uri: Uri, fileName: String, fileSizeBytes: Long): AudioContainerInfo {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)

            val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
            val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
            val album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
            val genre = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE)
            val mimeType = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE) ?: "audio/*"

            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val durationMs = durationStr?.toLongOrNull() ?: 0L

            val bitrateStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)
            val bitrateKbps = (bitrateStr?.toIntOrNull() ?: 0) / 1000

            val sampleRateHz = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_SAMPLERATE)?.toIntOrNull() ?: 44100
            } else {
                44100
            }

            val channels = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_NUM_TRACKS)?.toIntOrNull() ?: 2
            } else {
                2
            }

            val minutes = (durationMs / 1000) / 60
            val seconds = (durationMs / 1000) % 60
            val formattedDuration = String.format("%02d:%02d", minutes, seconds)

            return AudioContainerInfo(
                fileName = fileName,
                mimeType = mimeType,
                durationMs = durationMs,
                formattedDuration = formattedDuration,
                bitrateKbps = bitrateKbps,
                sampleRateHz = sampleRateHz,
                channels = channels,
                title = title,
                artist = artist,
                album = album,
                genre = genre,
                sizeBytes = fileSizeBytes
            )
        } finally {
            retriever.release()
        }
    }
}
