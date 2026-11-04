package com.omnishield.utility.presentation.metadata

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omnishield.utility.core.storage.StorageUtils
import com.omnishield.utility.domain.model.ExifMetadata
import com.omnishield.utility.domain.model.FileHashReport
import com.omnishield.utility.domain.model.SanitizerResult
import com.omnishield.utility.domain.repository.IMetadataRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class MetadataTab {
    EXIF_SANITIZER,
    HASH_VERIFIER
}

data class MetadataUiState(
    val currentTab: MetadataTab = MetadataTab.EXIF_SANITIZER,
    // EXIF
    val selectedImageUri: Uri? = null,
    val selectedImageName: String = "",
    val exifMetadata: ExifMetadata? = null,
    val isLoadingExif: Boolean = false,
    val isSanitizing: Boolean = false,
    val sanitizeResult: SanitizerResult? = null,
    val exifError: String? = null,
    // Hash
    val selectedHashFileUri: Uri? = null,
    val selectedHashFileName: String = "",
    val isComputingHash: Boolean = false,
    val hashProgress: Float = 0f,
    val hashReport: FileHashReport? = null,
    val verificationHashInput: String = "",
    val hashMatchStatus: Boolean? = null // true: match, false: mismatch, null: not checked
)

class MetadataViewModel(
    private val repository: IMetadataRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MetadataUiState())
    val uiState = _uiState.asStateFlow()

    fun setTab(tab: MetadataTab) {
        _uiState.update { it.copy(currentTab = tab) }
    }

    // --- EXIF / Sanitizer ---
    fun onImageSelected(context: Context, uri: Uri) {
        val fileName = StorageUtils.getFileName(context, uri)
        _uiState.update {
            it.copy(
                selectedImageUri = uri,
                selectedImageName = fileName,
                isLoadingExif = true,
                exifMetadata = null,
                sanitizeResult = null,
                exifError = null
            )
        }
        viewModelScope.launch {
            repository.inspectExif(uri)
                .onSuccess { exif ->
                    _uiState.update { it.copy(isLoadingExif = false, exifMetadata = exif) }
                }
                .onFailure { err ->
                    _uiState.update {
                        it.copy(
                            isLoadingExif = false,
                            exifError = err.localizedMessage ?: "Failed to read EXIF."
                        )
                    }
                }
        }
    }

    fun sanitizeSelectedFile() {
        val uri = _uiState.value.selectedImageUri ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isSanitizing = true, sanitizeResult = null) }
            val result = repository.sanitizeMetadata(uri)
            _uiState.update { it.copy(isSanitizing = false, sanitizeResult = result) }
        }
    }

    // --- Checksum / Hash ---
    fun onHashFileSelected(context: Context, uri: Uri) {
        val fileName = StorageUtils.getFileName(context, uri)
        _uiState.update {
            it.copy(
                selectedHashFileUri = uri,
                selectedHashFileName = fileName,
                isComputingHash = true,
                hashProgress = 0f,
                hashReport = null,
                hashMatchStatus = null
            )
        }
        viewModelScope.launch {
            repository.computeChecksums(uri).collect { (progress, report) ->
                _uiState.update {
                    it.copy(
                        hashProgress = progress,
                        hashReport = report,
                        isComputingHash = report == null
                    )
                }
            }
        }
    }

    fun onVerificationHashChanged(input: String) {
        val trimmed = input.trim().lowercase()
        val report = _uiState.value.hashReport
        val match = if (trimmed.isEmpty() || report == null) {
            null
        } else {
            trimmed == report.md5.lowercase() ||
                    trimmed == report.sha1.lowercase() ||
                    trimmed == report.sha256.lowercase()
        }
        _uiState.update {
            it.copy(
                verificationHashInput = input,
                hashMatchStatus = match
            )
        }
    }
}
