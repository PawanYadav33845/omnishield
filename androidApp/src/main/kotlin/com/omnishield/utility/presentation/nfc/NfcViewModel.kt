package com.omnishield.utility.presentation.nfc

import android.content.Context
import android.nfc.Tag
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omnishield.utility.core.nfc.NfcUtils
import com.omnishield.utility.domain.model.NfcScanState
import com.omnishield.utility.domain.model.NfcTagModel
import com.omnishield.utility.domain.model.NfcWriteState
import com.omnishield.utility.domain.model.NfcWriteType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class NfcTab {
    READ_TAG,
    WRITE_TAG,
    SAVED_VAULT
}

data class NfcUiState(
    val currentTab: NfcTab = NfcTab.READ_TAG,
    val isSupported: Boolean = true,
    val isEnabled: Boolean = true,
    val scanState: NfcScanState = NfcScanState.Idle,
    val writeState: NfcWriteState = NfcWriteState.Idle,
    val writeType: NfcWriteType = NfcWriteType.TEXT,
    val writeInputContent: String = "",
    val savedTags: List<NfcTagModel> = emptyList()
)

class NfcViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(NfcUiState())
    val uiState = _uiState.asStateFlow()

    fun checkHardware(context: Context) {
        val supported = NfcUtils.isNfcSupported(context)
        val enabled = NfcUtils.isNfcEnabled(context)
        val vaultTags = NfcUtils.loadStoredTags(context)
        _uiState.update {
            it.copy(
                isSupported = supported,
                isEnabled = enabled,
                savedTags = vaultTags
            )
        }
    }

    fun setTab(tab: NfcTab) {
        _uiState.update { it.copy(currentTab = tab) }
    }

    fun startListening() {
        _uiState.update { it.copy(scanState = NfcScanState.Listening) }
    }

    fun onTagDiscovered(tag: Tag) {
        try {
            if (_uiState.value.writeState is NfcWriteState.ReadyToTap) {
                // In write mode
                val ready = _uiState.value.writeState as NfcWriteState.ReadyToTap
                val writtenBytes = NfcUtils.writeTag(tag, ready.payload, ready.type)
                val idHex = tag.id?.joinToString(":") { String.format("%02X", it) } ?: "TAG"
                _uiState.update { it.copy(writeState = NfcWriteState.Success(idHex, writtenBytes)) }
            } else {
                // In read mode
                val parsed = NfcUtils.parseTag(tag)
                _uiState.update { it.copy(scanState = NfcScanState.TagDetected(parsed)) }
            }
        } catch (e: Exception) {
            if (_uiState.value.writeState is NfcWriteState.ReadyToTap) {
                _uiState.update { it.copy(writeState = NfcWriteState.Error(e.localizedMessage ?: "Tag write failed.")) }
            } else {
                _uiState.update { it.copy(scanState = NfcScanState.Error(e.localizedMessage ?: "Tag read error.")) }
            }
        }
    }

    fun prepareWrite(type: NfcWriteType, content: String) {
        if (content.isBlank()) return
        _uiState.update {
            it.copy(
                writeType = type,
                writeInputContent = content,
                writeState = NfcWriteState.ReadyToTap(content, type)
            )
        }
    }

    fun cancelWrite() {
        _uiState.update { it.copy(writeState = NfcWriteState.Idle) }
    }

    fun saveTag(context: Context, tag: NfcTagModel) {
        NfcUtils.saveTagToVault(context, tag)
        val updated = NfcUtils.loadStoredTags(context)
        _uiState.update { it.copy(savedTags = updated) }
    }

    fun deleteTag(context: Context, idHex: String) {
        NfcUtils.deleteTagFromVault(context, idHex)
        val updated = NfcUtils.loadStoredTags(context)
        _uiState.update { it.copy(savedTags = updated) }
    }
}
