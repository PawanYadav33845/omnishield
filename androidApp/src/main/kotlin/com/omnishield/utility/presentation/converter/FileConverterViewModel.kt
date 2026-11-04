package com.omnishield.utility.presentation.converter

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omnishield.utility.core.storage.StorageUtils
import com.omnishield.utility.domain.model.AudioContainerInfo
import com.omnishield.utility.domain.model.BatchConversionProgress
import com.omnishield.utility.domain.model.ConversionResult
import com.omnishield.utility.domain.model.DataFormat
import com.omnishield.utility.domain.model.ImageFormat
import com.omnishield.utility.domain.repository.IFileConverterRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class ConverterTab {
    IMAGES,
    LOSSLESS_ZIP,
    DATA_DOCS,
    AUDIO_MEDIA
}

data class ConverterUiState(
    val currentTab: ConverterTab = ConverterTab.IMAGES,
    // Images
    val selectedImageUris: List<Uri> = emptyList(),
    val targetImageFormat: ImageFormat = ImageFormat.JPEG,
    val imageQuality: Int = 90,
    val targetSizePresetKb: Int? = null,
    val dimensionScalePercent: Int = 100, // 100%, 75%, 50%, 25%
    val isConvertingImages: Boolean = false,
    val imageProgress: BatchConversionProgress = BatchConversionProgress(),
    val imageConversionResult: ConversionResult? = null,
    // Lossless ZIP
    val selectedLosslessUri: Uri? = null,
    val selectedLosslessFileName: String = "",
    val isCompressingLossless: Boolean = false,
    val compressedLosslessUri: Uri? = null,
    val losslessError: String? = null,
    // Data
    val selectedDataUri: Uri? = null,
    val selectedDataFileName: String = "",
    val sourceDataFormat: DataFormat = DataFormat.JSON,
    val targetDataFormat: DataFormat = DataFormat.XML,
    val isConvertingData: Boolean = false,
    val convertedDataUri: Uri? = null,
    val dataError: String? = null,
    // Audio
    val selectedAudioUri: Uri? = null,
    val audioMetadata: AudioContainerInfo? = null,
    val isLoadingAudio: Boolean = false,
    val audioError: String? = null
)

class FileConverterViewModel(
    private val repository: IFileConverterRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ConverterUiState())
    val uiState = _uiState.asStateFlow()

    fun setTab(tab: ConverterTab) {
        _uiState.update { it.copy(currentTab = tab) }
    }

    // --- Image Section ---
    fun onImagesSelected(uris: List<Uri>) {
        _uiState.update {
            it.copy(
                selectedImageUris = uris,
                imageConversionResult = null
            )
        }
    }

    fun setTargetImageFormat(format: ImageFormat) {
        _uiState.update { it.copy(targetImageFormat = format) }
    }

    fun setImageQuality(quality: Int) {
        _uiState.update { it.copy(imageQuality = quality, targetSizePresetKb = null) }
    }

    fun setTargetSizePreset(kb: Int?) {
        _uiState.update { it.copy(targetSizePresetKb = kb) }
    }

    fun setDimensionScale(scale: Int) {
        _uiState.update { it.copy(dimensionScalePercent = scale) }
    }

    fun startImageConversion() {
        val uris = _uiState.value.selectedImageUris
        if (uris.isEmpty()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isConvertingImages = true, imageConversionResult = null) }
            repository.convertImages(
                inputUris = uris,
                targetFormat = _uiState.value.targetImageFormat,
                quality = _uiState.value.imageQuality
            ).collect { (progress, result) ->
                _uiState.update {
                    it.copy(
                        imageProgress = progress,
                        imageConversionResult = result,
                        isConvertingImages = result == null
                    )
                }
            }
        }
    }

    fun clearImages() {
        _uiState.update {
            it.copy(
                selectedImageUris = emptyList(),
                imageConversionResult = null,
                isConvertingImages = false
            )
        }
    }

    // --- Lossless File Compression (ZIP DEFLATE) ---
    fun onLosslessFileSelected(context: Context, uri: Uri) {
        val fileName = StorageUtils.getFileName(context, uri)
        _uiState.update {
            it.copy(
                selectedLosslessUri = uri,
                selectedLosslessFileName = fileName,
                compressedLosslessUri = null,
                losslessError = null
            )
        }
    }

    fun startLosslessCompression() {
        val uri = _uiState.value.selectedLosslessUri ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isCompressingLossless = true, losslessError = null) }
            val result = repository.compressFileLossless(uri)
            result.onSuccess { outputUri ->
                _uiState.update {
                    it.copy(
                        isCompressingLossless = false,
                        compressedLosslessUri = outputUri
                    )
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isCompressingLossless = false,
                        losslessError = err.localizedMessage ?: "Lossless compression failed."
                    )
                }
            }
        }
    }

    // --- Data Section ---
    fun onDataFileSelected(context: Context, uri: Uri) {
        val fileName = StorageUtils.getFileName(context, uri)
        val inferredSource = when (fileName.substringAfterLast('.', "").lowercase()) {
            "xml" -> DataFormat.XML
            "yaml", "yml" -> DataFormat.YAML
            "csv" -> DataFormat.CSV
            else -> DataFormat.JSON
        }
        val defaultTarget = if (inferredSource == DataFormat.JSON) DataFormat.XML else DataFormat.JSON

        _uiState.update {
            it.copy(
                selectedDataUri = uri,
                selectedDataFileName = fileName,
                sourceDataFormat = inferredSource,
                targetDataFormat = defaultTarget,
                convertedDataUri = null,
                dataError = null
            )
        }
    }

    fun setSourceDataFormat(format: DataFormat) {
        _uiState.update { it.copy(sourceDataFormat = format) }
    }

    fun setTargetDataFormat(format: DataFormat) {
        _uiState.update { it.copy(targetDataFormat = format) }
    }

    fun startDataConversion() {
        val uri = _uiState.value.selectedDataUri ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isConvertingData = true, dataError = null) }
            val result = repository.convertData(
                inputUri = uri,
                sourceFormat = _uiState.value.sourceDataFormat,
                targetFormat = _uiState.value.targetDataFormat
            )
            result.onSuccess { convertedUri ->
                _uiState.update {
                    it.copy(
                        isConvertingData = false,
                        convertedDataUri = convertedUri
                    )
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isConvertingData = false,
                        dataError = err.localizedMessage ?: "Conversion failed."
                    )
                }
            }
        }
    }

    // --- Audio Section ---
    fun onAudioSelected(uri: Uri) {
        _uiState.update {
            it.copy(
                selectedAudioUri = uri,
                isLoadingAudio = true,
                audioMetadata = null,
                audioError = null
            )
        }
        viewModelScope.launch {
            repository.inspectAudio(uri)
                .onSuccess { info ->
                    _uiState.update { it.copy(isLoadingAudio = false, audioMetadata = info) }
                }
                .onFailure { err ->
                    _uiState.update {
                        it.copy(isLoadingAudio = false, audioError = err.localizedMessage ?: "Cannot read audio stream.")
                    }
                }
        }
    }
}
