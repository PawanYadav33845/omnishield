package com.omnishield.utility.presentation.hex

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omnishield.utility.core.hex.HexInspectorUtils
import com.omnishield.utility.core.storage.StorageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class HexViewerState(
    val selectedUri: Uri? = null,
    val fileName: String? = null,
    val fileSizeFormatted: String? = null,
    val detectedFormat: String? = null,
    val hexDump: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val maxBytes: Int = 8192
)

class HexViewerViewModel(
    private val context: Context
) : ViewModel() {

    private val _state = MutableStateFlow(HexViewerState())
    val state: StateFlow<HexViewerState> = _state.asStateFlow()

    fun onFileSelected(uri: Uri) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null, selectedUri = uri) }
            try {
                val fileName = StorageUtils.getFileName(context, uri)
                val size = StorageUtils.getFileSize(context, uri)
                val sizeFormatted = StorageUtils.formatBytes(size)

                val (dump, format) = withContext(Dispatchers.IO) {
                    val lines = context.contentResolver.openInputStream(uri)?.use { stream ->
                        HexInspectorUtils.generateHexDump(stream, _state.value.maxBytes)
                    } ?: emptyList()

                    val dumpText = lines.joinToString("\n") { line ->
                        "${line.offsetHex}  ${line.hexBytes.padEnd(49, ' ')}  |${line.asciiText}|"
                    }

                    // Extract magic header from first line hex bytes
                    val detected = if (lines.isNotEmpty()) {
                        detectMagicHeaderFromHex(lines.first().hexBytes)
                    } else {
                        "Empty File"
                    }

                    Pair(dumpText, detected)
                }

                _state.update {
                    it.copy(
                        fileName = fileName,
                        fileSizeFormatted = sizeFormatted,
                        hexDump = dump,
                        detectedFormat = format,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Failed to inspect file: ${e.localizedMessage ?: "Unknown error"}"
                    )
                }
            }
        }
    }

    private fun detectMagicHeaderFromHex(firstLineHex: String): String {
        val clean = firstLineHex.replace(" ", "").uppercase()
        return when {
            clean.startsWith("89504E47") -> "PNG Image (Portable Network Graphics)"
            clean.startsWith("FFD8FF") -> "JPEG / JFIF Image"
            clean.startsWith("25504446") -> "PDF Document (Adobe Portable Document Format)"
            clean.startsWith("504B0304") || clean.startsWith("504B0506") -> "ZIP Archive / APK / Office OpenXML"
            clean.startsWith("7F454C46") -> "ELF Executable (Linux/Android Binary)"
            clean.startsWith("52494646") -> "RIFF Container (WEBP / WAV / AVI)"
            clean.startsWith("47494638") -> "GIF Graphics Image"
            else -> "Binary Data Stream"
        }
    }

    fun clearError() {
        _state.update { it.copy(errorMessage = null) }
    }
}
