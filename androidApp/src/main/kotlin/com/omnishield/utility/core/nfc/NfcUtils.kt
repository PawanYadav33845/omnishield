package com.omnishield.utility.core.nfc

import android.content.Context
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.nfc.tech.NdefFormatable
import com.omnishield.utility.domain.model.NdefParsedRecord
import com.omnishield.utility.domain.model.NfcTagModel
import com.omnishield.utility.domain.model.NfcWriteType
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.nio.charset.Charset

object NfcUtils {

    private const val VAULT_FILE_NAME = "nfc_tags_vault.json"

    fun isNfcSupported(context: Context): Boolean {
        return NfcAdapter.getDefaultAdapter(context) != null
    }

    fun isNfcEnabled(context: Context): Boolean {
        return NfcAdapter.getDefaultAdapter(context)?.isEnabled == true
    }

    /**
     * Parses an Android NFC Tag into an immutable domain model.
     */
    fun parseTag(tag: Tag): NfcTagModel {
        val idHex = tag.id?.toColonHex() ?: "UNKNOWN_ID"
        val techList = tag.techList.map { it.substringAfterLast('.') }

        val ndef = Ndef.get(tag)
        val isWritable = ndef?.isWritable ?: false
        val maxSize = ndef?.maxSize ?: 0

        val parsedRecords = mutableListOf<NdefParsedRecord>()

        ndef?.cachedNdefMessage?.records?.forEach { record ->
            parsedRecords.add(parseNdefRecord(record))
        }

        return NfcTagModel(
            tagIdHex = idHex,
            technologies = techList,
            isWritable = isWritable,
            maxSizeByte = maxSize,
            records = parsedRecords,
            userLabel = "Tag $idHex"
        )
    }

    private fun parseNdefRecord(record: NdefRecord): NdefParsedRecord {
        val tnf = record.tnf
        val type = record.type
        val payload = record.payload

        var typeStr = "Unknown"
        var displayStr = ""
        var uriStr: String? = null

        when (tnf) {
            NdefRecord.TNF_WELL_KNOWN -> {
                if (type.contentEquals(NdefRecord.RTD_TEXT)) {
                    typeStr = "Well-Known: Text"
                    displayStr = parseTextRecord(payload)
                } else if (type.contentEquals(NdefRecord.RTD_URI)) {
                    typeStr = "Well-Known: URI"
                    uriStr = parseUriRecord(payload)
                    displayStr = uriStr
                } else {
                    typeStr = "Well-Known (0x${type.toColonHex()})"
                    displayStr = String(payload, Charsets.UTF_8)
                }
            }
            NdefRecord.TNF_MIME_MEDIA -> {
                val mime = String(type, Charsets.US_ASCII)
                typeStr = "MIME: $mime"
                displayStr = String(payload, Charsets.UTF_8)
            }
            NdefRecord.TNF_ABSOLUTE_URI -> {
                typeStr = "Absolute URI"
                val uri = String(payload, Charsets.UTF_8)
                uriStr = uri
                displayStr = uri
            }
            NdefRecord.TNF_EXTERNAL_TYPE -> {
                typeStr = "External (${String(type, Charsets.US_ASCII)})"
                displayStr = String(payload, Charsets.UTF_8)
            }
            else -> {
                typeStr = "Raw TNF $tnf"
                displayStr = payload.toColonHex()
            }
        }

        return NdefParsedRecord(
            typeName = typeStr,
            payloadDisplay = displayStr,
            uriString = uriStr,
            rawPayloadHex = payload.toColonHex()
        )
    }

    private fun parseTextRecord(payload: ByteArray): String {
        if (payload.isEmpty()) return ""
        val status = payload[0].toInt()
        val encUtf16 = (status and 0x80) != 0
        val langCodeLen = status and 0x3F
        val charset = if (encUtf16) Charsets.UTF_16 else Charsets.UTF_8
        val textStartIndex = 1 + langCodeLen
        return if (payload.size > textStartIndex) {
            String(payload, textStartIndex, payload.size - textStartIndex, charset)
        } else {
            ""
        }
    }

    private fun parseUriRecord(payload: ByteArray): String {
        if (payload.isEmpty()) return ""
        val prefixCode = payload[0].toInt()
        val prefix = when (prefixCode) {
            0x01 -> "http://www."
            0x02 -> "https://www."
            0x03 -> "http://"
            0x04 -> "https://"
            0x05 -> "tel:"
            0x06 -> "mailto:"
            0x07 -> "ftp://anonymous:anonymous@"
            0x08 -> "ftp://ftp."
            0x09 -> "ftps://"
            0x0A -> "sftp://"
            0x0B -> "smb://"
            0x0C -> "nfs://"
            0x0D -> "ftp://"
            0x0E -> "dav://"
            0x0F -> "news:"
            0x10 -> "telnet://"
            0x11 -> "imap:"
            0x12 -> "rtsp://"
            0x13 -> "urn:"
            0x14 -> "pop:"
            0x15 -> "sip:"
            0x16 -> "sips:"
            0x17 -> "tftp:"
            0x18 -> "btspp://"
            0x19 -> "btl2cap://"
            0x1A -> "btgoep://"
            0x1B -> "tcpobex://"
            0x1C -> "irdaobex://"
            0x1D -> "file://"
            0x1E -> "urn:epc:id:"
            0x1F -> "urn:epc:tag:"
            0x20 -> "urn:epc:pat:"
            0x21 -> "urn:epc:raw:"
            0x22 -> "urn:epc:"
            0x23 -> "urn:nfc:"
            else -> ""
        }
        val remainder = String(payload, 1, payload.size - 1, Charsets.UTF_8)
        return "$prefix$remainder"
    }

    /**
     * Writes an NDEF message to a physical tag.
     */
    fun writeTag(tag: Tag, content: String, type: NfcWriteType): Int {
        val record = when (type) {
            NfcWriteType.TEXT -> createTextRecord(content)
            NfcWriteType.URI -> NdefRecord.createUri(content)
            NfcWriteType.MIME_JSON -> NdefRecord.createMime("application/json", content.toByteArray(Charsets.UTF_8))
        }
        val message = NdefMessage(arrayOf(record))
        val messageSize = message.byteArrayLength

        val ndef = Ndef.get(tag)
        if (ndef != null) {
            ndef.connect()
            if (!ndef.isWritable) {
                ndef.close()
                throw IllegalStateException("NFC Tag is locked / read-only.")
            }
            if (ndef.maxSize < messageSize) {
                ndef.close()
                throw IllegalStateException("Tag capacity (${ndef.maxSize} bytes) is too small for payload ($messageSize bytes).")
            }
            ndef.writeNdefMessage(message)
            ndef.close()
            return messageSize
        } else {
            val formatable = NdefFormatable.get(tag)
                ?: throw IllegalStateException("Tag does not support NDEF formatting.")
            formatable.connect()
            formatable.format(message)
            formatable.close()
            return messageSize
        }
    }

    private fun createTextRecord(payload: String): NdefRecord {
        val lang = "en".toByteArray(Charsets.US_ASCII)
        val textBytes = payload.toByteArray(Charsets.UTF_8)
        val recordPayload = ByteArray(1 + lang.size + textBytes.size)

        recordPayload[0] = lang.size.toByte() // UTF-8 encoding (bit 7 = 0) + lang length
        System.arraycopy(lang, 0, recordPayload, 1, lang.size)
        System.arraycopy(textBytes, 0, recordPayload, 1 + lang.size, textBytes.size)

        return NdefRecord(NdefRecord.TNF_WELL_KNOWN, NdefRecord.RTD_TEXT, ByteArray(0), recordPayload)
    }

    // ---------------------------------------------------------------------------------------------
    // Offline Stored NFC Tag Vault
    // ---------------------------------------------------------------------------------------------

    fun loadStoredTags(context: Context): List<NfcTagModel> {
        val file = File(context.filesDir, VAULT_FILE_NAME)
        if (!file.exists()) return emptyList()

        return try {
            val jsonStr = file.readText()
            val array = JSONArray(jsonStr)
            val list = mutableListOf<NfcTagModel>()

            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val idHex = obj.getString("tagIdHex")
                val isWritable = obj.optBoolean("isWritable", false)
                val maxSize = obj.optInt("maxSizeByte", 0)
                val timestamp = obj.optLong("scannedTimestamp", System.currentTimeMillis())
                val label = obj.optString("userLabel", "Tag $idHex")

                val techArr = obj.optJSONArray("technologies") ?: JSONArray()
                val techList = (0 until techArr.length()).map { techArr.getString(it) }

                val recordsArr = obj.optJSONArray("records") ?: JSONArray()
                val parsedRecords = mutableListOf<NdefParsedRecord>()
                for (r in 0 until recordsArr.length()) {
                    val rObj = recordsArr.getJSONObject(r)
                    parsedRecords.add(
                        NdefParsedRecord(
                            typeName = rObj.getString("typeName"),
                            payloadDisplay = rObj.getString("payloadDisplay"),
                            uriString = rObj.optString("uriString").takeIf { it.isNotEmpty() },
                            rawPayloadHex = rObj.optString("rawPayloadHex")
                        )
                    )
                }

                list.add(
                    NfcTagModel(
                        tagIdHex = idHex,
                        technologies = techList,
                        isWritable = isWritable,
                        maxSizeByte = maxSize,
                        records = parsedRecords,
                        scannedTimestamp = timestamp,
                        userLabel = label
                    )
                )
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun saveTagToVault(context: Context, tag: NfcTagModel) {
        val currentList = loadStoredTags(context).toMutableList()
        // Deduplicate or update
        val existingIndex = currentList.indexOfFirst { it.tagIdHex == tag.tagIdHex }
        if (existingIndex >= 0) {
            currentList[existingIndex] = tag
        } else {
            currentList.add(0, tag)
        }

        val array = JSONArray()
        for (item in currentList) {
            val obj = JSONObject().apply {
                put("tagIdHex", item.tagIdHex)
                put("isWritable", item.isWritable)
                put("maxSizeByte", item.maxSizeByte)
                put("scannedTimestamp", item.scannedTimestamp)
                put("userLabel", item.userLabel)

                val techArr = JSONArray()
                item.technologies.forEach { techArr.put(it) }
                put("technologies", techArr)

                val recArr = JSONArray()
                item.records.forEach { r ->
                    val rObj = JSONObject().apply {
                        put("typeName", r.typeName)
                        put("payloadDisplay", r.payloadDisplay)
                        put("uriString", r.uriString ?: "")
                        put("rawPayloadHex", r.rawPayloadHex)
                    }
                    recArr.put(rObj)
                }
                put("records", recArr)
            }
            array.put(obj)
        }

        File(context.filesDir, VAULT_FILE_NAME).writeText(array.toString(2))
    }

    fun deleteTagFromVault(context: Context, tagIdHex: String) {
        val currentList = loadStoredTags(context).filter { it.tagIdHex != tagIdHex }
        val array = JSONArray()
        for (item in currentList) {
            val obj = JSONObject().apply {
                put("tagIdHex", item.tagIdHex)
                put("isWritable", item.isWritable)
                put("maxSizeByte", item.maxSizeByte)
                put("scannedTimestamp", item.scannedTimestamp)
                put("userLabel", item.userLabel)
            }
            array.put(obj)
        }
        File(context.filesDir, VAULT_FILE_NAME).writeText(array.toString(2))
    }

    private fun ByteArray.toColonHex(): String {
        return joinToString(":") { String.format("%02X", it) }
    }
}
