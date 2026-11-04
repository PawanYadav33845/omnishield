package com.omnishield.utility.domain.model

enum class NfcWriteType {
    TEXT,
    URI,
    MIME_JSON
}

data class NdefParsedRecord(
    val typeName: String,
    val payloadDisplay: String,
    val uriString: String? = null,
    val rawPayloadHex: String = ""
)

data class NfcTagModel(
    val tagIdHex: String,
    val technologies: List<String>,
    val isWritable: Boolean,
    val maxSizeByte: Int,
    val records: List<NdefParsedRecord>,
    val scannedTimestamp: Long = System.currentTimeMillis(),
    val userLabel: String = "Scanned Tag"
)

sealed interface NfcScanState {
    data object Idle : NfcScanState
    data object Listening : NfcScanState
    data class TagDetected(val tag: NfcTagModel) : NfcScanState
    data class Error(val message: String) : NfcScanState
}

sealed interface NfcWriteState {
    data object Idle : NfcWriteState
    data class ReadyToTap(val payload: String, val type: NfcWriteType) : NfcWriteState
    data class Success(val tagIdHex: String, val bytesWritten: Int) : NfcWriteState
    data class Error(val message: String) : NfcWriteState
}
