package com.omnishield.utility.presentation.hardware

import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omnishield.utility.core.diagnostics.BatteryDiagnosticsUtils
import com.omnishield.utility.core.diagnostics.BatteryReport
import com.omnishield.utility.core.qr.QrCodeUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

data class HardwareStudioState(
    val activeTab: Int = 0, // 0: Battery Diagnostics, 1: Offline QR Generator
    // Battery
    val batteryReport: BatteryReport? = null,
    val isReadingBattery: Boolean = false,
    // QR Generator
    val qrContent: String = "OmniShield Air-Gapped Key",
    val qrErrorCorrection: QrCodeUtils.QrErrorCorrection = QrCodeUtils.QrErrorCorrection.HIGH,
    val qrBitmap: Bitmap? = null,
    val qrSavedFile: File? = null,
    val isGeneratingQr: Boolean = false,
    // Common
    val errorMessage: String? = null,
    val statusMessage: String? = null
)

class HardwareStudioViewModel(
    private val context: Context
) : ViewModel() {

    private val _state = MutableStateFlow(HardwareStudioState())
    val state: StateFlow<HardwareStudioState> = _state.asStateFlow()

    init {
        refreshBattery()
        generateQr()
    }

    fun selectTab(tab: Int) {
        _state.update { it.copy(activeTab = tab, errorMessage = null, statusMessage = null) }
        if (tab == 0) refreshBattery()
    }

    fun refreshBattery() {
        viewModelScope.launch {
            _state.update { it.copy(isReadingBattery = true) }
            val report = withContext(Dispatchers.IO) {
                BatteryDiagnosticsUtils.getBatteryReport(context)
            }
            _state.update { it.copy(batteryReport = report, isReadingBattery = false) }
        }
    }

    fun setQrContent(content: String) {
        _state.update { it.copy(qrContent = content) }
    }

    fun setQrErrorCorrection(ec: QrCodeUtils.QrErrorCorrection) {
        _state.update { it.copy(qrErrorCorrection = ec) }
        generateQr()
    }

    fun generateQr() {
        val content = _state.value.qrContent
        if (content.isBlank()) return

        viewModelScope.launch {
            _state.update { it.copy(isGeneratingQr = true, errorMessage = null) }
            val result = withContext(Dispatchers.Default) {
                QrCodeUtils.generateQrBitmap(
                    content = content,
                    dimensionPx = 650,
                    errorCorrection = _state.value.qrErrorCorrection
                )
            }

            if (result.isSuccess) {
                val bitmap = result.getOrThrow()
                val cacheDir = File(context.cacheDir, "qr_vault").apply { mkdirs() }
                val outFile = File(cacheDir, "QR_${System.currentTimeMillis()}.png")
                withContext(Dispatchers.IO) {
                    FileOutputStream(outFile).use { out ->
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                    }
                }

                _state.update {
                    it.copy(
                        isGeneratingQr = false,
                        qrBitmap = bitmap,
                        qrSavedFile = outFile,
                        statusMessage = "QR Code rendered offline"
                    )
                }
            } else {
                _state.update {
                    it.copy(
                        isGeneratingQr = false,
                        errorMessage = "QR generation error: ${result.exceptionOrNull()?.message}"
                    )
                }
            }
        }
    }

    fun clearMessages() {
        _state.update { it.copy(errorMessage = null, statusMessage = null) }
    }
}
