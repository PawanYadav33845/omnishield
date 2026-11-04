package com.omnishield.utility.presentation.crypto

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omnishield.utility.core.crypto.CryptoUtils
import com.omnishield.utility.core.storage.StorageUtils
import com.omnishield.utility.domain.model.BiometricStatus
import com.omnishield.utility.domain.model.CryptoMode
import com.omnishield.utility.domain.model.CryptoProgress
import com.omnishield.utility.domain.model.CryptoResult
import com.omnishield.utility.domain.repository.ICryptoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CryptoUiState(
    val mode: CryptoMode = CryptoMode.ENCRYPT,
    val selectedFileUri: Uri? = null,
    val selectedFileName: String = "",
    val selectedFileSizeFormatted: String = "",
    val passphrase: String = "",
    val confirmPassphrase: String = "",
    val isPasswordVisible: Boolean = false,
    val passwordStrength: Float = 0f,
    val passwordStrengthLabel: String = "Empty",
    val isProcessing: Boolean = false,
    val progress: CryptoProgress = CryptoProgress(),
    val result: CryptoResult? = null,
    val biometricStatus: BiometricStatus = BiometricStatus.NotAvailable
)

class CryptoViewModel(
    private val repository: ICryptoRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CryptoUiState())
    val uiState = _uiState.asStateFlow()

    fun setMode(mode: CryptoMode) {
        _uiState.update {
            it.copy(
                mode = mode,
                result = null
            )
        }
    }

    fun onFileSelected(context: Context, uri: Uri) {
        val name = StorageUtils.getFileName(context, uri)
        val size = StorageUtils.getFileSize(context, uri)
        val isEncrypted = name.endsWith(".shield", ignoreCase = true)

        viewModelScope.launch {
            val detectedEncrypted = repository.isEncryptedOmniShieldFile(uri)
            val autoMode = if (isEncrypted || detectedEncrypted) CryptoMode.DECRYPT else _uiState.value.mode

            _uiState.update {
                it.copy(
                    selectedFileUri = uri,
                    selectedFileName = name,
                    selectedFileSizeFormatted = StorageUtils.formatBytes(size),
                    mode = autoMode,
                    result = null
                )
            }
        }
    }

    fun onPassphraseChanged(text: String) {
        val (strength, label) = evaluatePasswordStrength(text)
        _uiState.update {
            it.copy(
                passphrase = text,
                passwordStrength = strength,
                passwordStrengthLabel = label
            )
        }
    }

    fun onConfirmPassphraseChanged(text: String) {
        _uiState.update { it.copy(confirmPassphrase = text) }
    }

    fun togglePasswordVisibility() {
        _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }

    fun generateAndSetStrongPassword() {
        val strong = CryptoUtils.generateStrongPassphrase(length = 20, includeSpecial = true)
        val (strength, label) = evaluatePasswordStrength(strong)
        _uiState.update {
            it.copy(
                passphrase = strong,
                confirmPassphrase = strong,
                passwordStrength = strength,
                passwordStrengthLabel = label,
                isPasswordVisible = true
            )
        }
    }

    fun startCryptoOperation(destinationUri: Uri? = null) {
        val state = _uiState.value
        val fileUri = state.selectedFileUri ?: return

        if (state.passphrase.isBlank()) {
            _uiState.update {
                it.copy(result = CryptoResult.Failure("Passphrase cannot be empty."))
            }
            return
        }

        if (state.mode == CryptoMode.ENCRYPT && state.passphrase != state.confirmPassphrase) {
            _uiState.update {
                it.copy(result = CryptoResult.Failure("Passphrases do not match."))
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, result = null) }

            repository.executeCryptoOperation(
                inputUri = fileUri,
                outputDestinationUri = destinationUri,
                passphrase = state.passphrase.toCharArray(),
                mode = state.mode
            ).collect { (progress, result) ->
                _uiState.update {
                    it.copy(
                        progress = progress,
                        result = result,
                        isProcessing = result == null
                    )
                }
            }
        }
    }

    fun onBiometricSuccess() {
        _uiState.update { it.copy(biometricStatus = BiometricStatus.Authenticated()) }
    }

    fun clear() {
        _uiState.update {
            it.copy(
                selectedFileUri = null,
                selectedFileName = "",
                selectedFileSizeFormatted = "",
                passphrase = "",
                confirmPassphrase = "",
                result = null,
                isProcessing = false
            )
        }
    }

    private fun evaluatePasswordStrength(password: String): Pair<Float, String> {
        if (password.isEmpty()) return 0f to "Empty"
        var score = 0
        if (password.length >= 8) score += 25
        if (password.length >= 12) score += 15
        if (password.any { it.isUpperCase() }) score += 20
        if (password.any { it.isLowerCase() }) score += 10
        if (password.any { it.isDigit() }) score += 15
        if (password.any { !it.isLetterOrDigit() }) score += 15

        val pct = (score.coerceAtMost(100) / 100f)
        val label = when {
            pct < 0.4f -> "Weak"
            pct < 0.7f -> "Moderate"
            pct < 0.9f -> "Strong"
            else -> "Fortified"
        }
        return pct to label
    }
}
