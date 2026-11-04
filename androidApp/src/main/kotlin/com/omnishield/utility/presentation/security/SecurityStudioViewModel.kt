package com.omnishield.utility.presentation.security

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omnishield.utility.core.diagnostics.AppPrivacyRisk
import com.omnishield.utility.core.diagnostics.PermissionAuditorUtils
import com.omnishield.utility.core.shredder.ShredderUtils
import com.omnishield.utility.core.stego.SteganographyUtils
import com.omnishield.utility.core.storage.StorageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

data class SecurityStudioState(
    val activeTab: Int = 0, // 0: Shredder, 1: Steganography, 2: Privacy Auditor
    // Shredder
    val shredderFileUri: Uri? = null,
    val shredderFileName: String? = null,
    val shredderFileSizeFormatted: String? = null,
    val shredderFileSizeBytes: Long = 0L,
    val shredderProgress: Float = 0f,
    val isShredding: Boolean = false,
    val shredderComplete: Boolean = false,
    // Steganography
    val stegoMode: Int = 0, // 0: Embed, 1: Extract
    val stegoCarrierUri: Uri? = null,
    val stegoSecretText: String = "",
    val stegoOutputImage: File? = null,
    val stegoExtractedText: String? = null,
    val isStegoProcessing: Boolean = false,
    // Privacy Auditor
    val appReports: List<AppPrivacyRisk> = emptyList(),
    val isScanningApps: Boolean = false,
    val searchQuery: String = "",
    // Common
    val errorMessage: String? = null,
    val statusMessage: String? = null
)

class SecurityStudioViewModel(
    private val context: Context
) : ViewModel() {

    private val _state = MutableStateFlow(SecurityStudioState())
    val state: StateFlow<SecurityStudioState> = _state.asStateFlow()

    init {
        refreshAppAudit()
    }

    fun selectTab(tab: Int) {
        _state.update { it.copy(activeTab = tab, errorMessage = null, statusMessage = null) }
        if (tab == 2 && _state.value.appReports.isEmpty()) {
            refreshAppAudit()
        }
    }

    // Shredder Actions
    fun setShredderFile(uri: Uri) {
        val fileName = StorageUtils.getFileName(context, uri)
        val sizeBytes = StorageUtils.getFileSize(context, uri)
        val formattedSize = StorageUtils.formatBytes(sizeBytes)

        _state.update {
            it.copy(
                shredderFileUri = uri,
                shredderFileName = fileName,
                shredderFileSizeFormatted = formattedSize,
                shredderFileSizeBytes = sizeBytes,
                shredderProgress = 0f,
                shredderComplete = false,
                statusMessage = null,
                errorMessage = null
            )
        }
    }

    fun executeShred() {
        val uri = _state.value.shredderFileUri ?: run {
            _state.update { it.copy(errorMessage = "Please select a file to securely erase.") }
            return
        }
        val sizeBytes = _state.value.shredderFileSizeBytes

        viewModelScope.launch {
            _state.update { it.copy(isShredding = true, errorMessage = null, statusMessage = "Executing DoD 5220.22-M 3-pass sanitize...") }
            try {
                val success = withContext(Dispatchers.IO) {
                    ShredderUtils.shredUri(
                        context = context,
                        uri = uri,
                        fileSizeBytes = sizeBytes,
                        method = ShredderUtils.ShredMethod.DOD_5220_22_M
                    ) { bytes, total, pass, totalPasses ->
                        val singlePassProgress = if (total > 0) bytes.toFloat() / total.toFloat() else 0f
                        val overallProgress = ((pass - 1).toFloat() + singlePassProgress) / totalPasses.toFloat()
                        _state.update { it.copy(shredderProgress = overallProgress.coerceIn(0f, 1f)) }
                    }
                }

                if (success) {
                    _state.update {
                        it.copy(
                            isShredding = false,
                            shredderComplete = true,
                            shredderProgress = 1.0f,
                            statusMessage = "Cryptographic shred complete: File overwritten 3 times and sanitized."
                        )
                    }
                } else {
                    _state.update {
                        it.copy(
                            isShredding = false,
                            errorMessage = "Shredding could not complete."
                        )
                    }
                }
            } catch (e: Exception) {
                _state.update { it.copy(isShredding = false, errorMessage = e.localizedMessage) }
            }
        }
    }

    // Steganography Actions
    fun setStegoMode(mode: Int) {
        _state.update { it.copy(stegoMode = mode, errorMessage = null, statusMessage = null) }
    }

    fun setStegoCarrier(uri: Uri) {
        _state.update { it.copy(stegoCarrierUri = uri, stegoOutputImage = null, stegoExtractedText = null) }
    }

    fun setStegoSecret(text: String) {
        _state.update { it.copy(stegoSecretText = text) }
    }

    fun executeStegoEmbed() {
        val uri = _state.value.stegoCarrierUri ?: run {
            _state.update { it.copy(errorMessage = "Please select a carrier image.") }
            return
        }
        val secret = _state.value.stegoSecretText
        if (secret.isBlank()) {
            _state.update { it.copy(errorMessage = "Secret text cannot be blank.") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isStegoProcessing = true, errorMessage = null, statusMessage = "Embedding secret payload into pixel LSBs...") }
            try {
                val cacheDir = File(context.cacheDir, "stego").apply { mkdirs() }
                val outFile = File(cacheDir, "Stego_${System.currentTimeMillis()}.png")

                val success = withContext(Dispatchers.IO) {
                    val inStream = context.contentResolver.openInputStream(uri)
                        ?: throw IllegalArgumentException("Cannot open carrier image stream.")
                    val outStream = FileOutputStream(outFile)
                    inStream.use { inp ->
                        outStream.use { out ->
                            SteganographyUtils.encodeTextIntoBitmap(inp, out, secret)
                        }
                    }
                }

                if (success) {
                    _state.update {
                        it.copy(
                            isStegoProcessing = false,
                            stegoOutputImage = outFile,
                            statusMessage = "Secret payload securely concealed in lossless PNG!"
                        )
                    }
                } else {
                    _state.update {
                        it.copy(
                            isStegoProcessing = false,
                            errorMessage = "Failed to encode secret in image."
                        )
                    }
                }
            } catch (e: Exception) {
                _state.update { it.copy(isStegoProcessing = false, errorMessage = e.localizedMessage) }
            }
        }
    }

    fun executeStegoExtract() {
        val uri = _state.value.stegoCarrierUri ?: run {
            _state.update { it.copy(errorMessage = "Please select an image to extract hidden text from.") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isStegoProcessing = true, errorMessage = null, statusMessage = "Reading pixel LSBs...") }
            try {
                val secret = withContext(Dispatchers.IO) {
                    val inStream = context.contentResolver.openInputStream(uri)
                        ?: throw IllegalArgumentException("Cannot open image stream.")
                    inStream.use { inp ->
                        SteganographyUtils.decodeTextFromBitmap(inp)
                    }
                }

                if (secret != null) {
                    _state.update {
                        it.copy(
                            isStegoProcessing = false,
                            stegoExtractedText = secret,
                            statusMessage = "Hidden secret extracted successfully!"
                        )
                    }
                } else {
                    _state.update {
                        it.copy(
                            isStegoProcessing = false,
                            errorMessage = "Extraction failed: No valid OmniShield stego marker or corrupted payload."
                        )
                    }
                }
            } catch (e: Exception) {
                _state.update { it.copy(isStegoProcessing = false, errorMessage = e.localizedMessage) }
            }
        }
    }

    // Privacy Auditor Actions
    fun setSearchQuery(query: String) {
        _state.update { it.copy(searchQuery = query) }
    }

    fun refreshAppAudit() {
        viewModelScope.launch {
            _state.update { it.copy(isScanningApps = true) }
            try {
                val reports = withContext(Dispatchers.IO) {
                    PermissionAuditorUtils.auditInstalledApps(context)
                }
                _state.update { it.copy(appReports = reports, isScanningApps = false) }
            } catch (e: Exception) {
                _state.update { it.copy(isScanningApps = false, errorMessage = "Audit failed: ${e.message}") }
            }
        }
    }

    fun clearMessages() {
        _state.update { it.copy(errorMessage = null, statusMessage = null) }
    }
}
