package com.omnishield.utility.presentation.ir

import android.content.Context
import androidx.lifecycle.ViewModel
import com.omnishield.utility.core.ir.IrBlasterUtils
import com.omnishield.utility.domain.model.IrCommand
import com.omnishield.utility.domain.model.IrDeviceProfile
import com.omnishield.utility.domain.model.IrHardwareStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class IrUiState(
    val hardwareStatus: IrHardwareStatus = IrHardwareStatus(hasEmitter = false),
    val availableProfiles: List<IrDeviceProfile> = emptyList(),
    val selectedProfileIndex: Int = 0,
    val lastCommandTransmitted: String? = null,
    val isSimulatedMode: Boolean = false,
    val customProntoInput: String = "",
    val customTransmitStatus: String? = null
)

class IrViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(IrUiState())
    val uiState = _uiState.asStateFlow()

    fun init(context: Context) {
        val status = IrBlasterUtils.getHardwareStatus(context)
        val profiles = IrBlasterUtils.getDefaultProfiles()
        _uiState.update {
            it.copy(
                hardwareStatus = status,
                availableProfiles = profiles,
                isSimulatedMode = !status.hasEmitter
            )
        }
    }

    fun selectProfile(index: Int) {
        _uiState.update { it.copy(selectedProfileIndex = index, lastCommandTransmitted = null) }
    }

    fun transmitCommand(context: Context, command: IrCommand) {
        val success = IrBlasterUtils.transmit(context, command.frequencyHz, command.pattern)
        _uiState.update {
            it.copy(
                lastCommandTransmitted = "${command.name} (${command.frequencyHz / 1000} kHz)" + if (!success) " [Simulated]" else " [Fired!]"
            )
        }
    }

    fun onCustomProntoChanged(text: String) {
        _uiState.update { it.copy(customProntoInput = text, customTransmitStatus = null) }
    }

    fun transmitCustomPronto(context: Context) {
        try {
            val (freq, pattern) = IrBlasterUtils.parseProntoHex(_uiState.value.customProntoInput)
            val success = IrBlasterUtils.transmit(context, freq, pattern)
            _uiState.update {
                it.copy(
                    customTransmitStatus = "Transmitted ${pattern.size} pulses at ${freq / 1000} kHz" + if (!success) " (Simulated on hardware without IR blaster)" else ""
                )
            }
        } catch (e: Exception) {
            _uiState.update {
                it.copy(customTransmitStatus = "Error: ${e.localizedMessage}")
            }
        }
    }
}
