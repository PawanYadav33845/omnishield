package com.omnishield.utility.domain.model

import android.net.Uri

enum class CryptoMode {
    ENCRYPT,
    DECRYPT
}

data class CryptoProgress(
    val bytesProcessed: Long = 0L,
    val totalBytes: Long = 0L,
    val percentage: Float = 0f,
    val isIndeterminate: Boolean = false
)

sealed interface CryptoResult {
    data class Success(
        val outputUri: Uri,
        val outputFileName: String,
        val inputSizeBytes: Long,
        val outputSizeBytes: Long,
        val durationMs: Long,
        val mode: CryptoMode
    ) : CryptoResult

    data class Failure(
        val errorMessage: String,
        val cause: Throwable? = null
    ) : CryptoResult
}

sealed interface BiometricStatus {
    data object NotAvailable : BiometricStatus
    data object Available : BiometricStatus
    data object Authenticating : BiometricStatus
    data class Authenticated(val cipherToken: String? = null) : BiometricStatus
    data class Error(val message: String) : BiometricStatus
}
