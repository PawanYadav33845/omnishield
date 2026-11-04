package com.omnishield.utility.presentation.tokens

import androidx.lifecycle.ViewModel
import com.omnishield.utility.core.tokens.JwtParsedReport
import com.omnishield.utility.core.tokens.TokenInspectorUtils
import com.omnishield.utility.core.tokens.X509ParsedReport
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class TokenInspectorState(
    val activeTab: Int = 0, // 0: JWT Inspector, 1: X.509 Certificate
    val jwtInputText: String = "",
    val jwtReport: JwtParsedReport? = null,
    val certInputText: String = "",
    val certReport: X509ParsedReport? = null,
    val errorMessage: String? = null
)

class TokenInspectorViewModel : ViewModel() {

    private val _state = MutableStateFlow(TokenInspectorState())
    val state: StateFlow<TokenInspectorState> = _state.asStateFlow()

    fun selectTab(tab: Int) {
        _state.update { it.copy(activeTab = tab, errorMessage = null) }
    }

    fun setJwtInput(input: String) {
        _state.update { it.copy(jwtInputText = input) }
        parseJwt(input)
    }

    fun parseJwt(input: String) {
        val trimmed = input.trim()
        if (trimmed.isBlank()) {
            _state.update { it.copy(jwtReport = null, errorMessage = null) }
            return
        }

        // Avoid showing premature error messages while user is typing initial characters
        if (trimmed.length < 10 || !trimmed.contains('.')) {
            _state.update { it.copy(jwtReport = null, errorMessage = null) }
            return
        }

        val result = TokenInspectorUtils.parseJwt(trimmed)
        if (result.isSuccess) {
            _state.update { it.copy(jwtReport = result.getOrNull(), errorMessage = null) }
        } else {
            _state.update { it.copy(jwtReport = null, errorMessage = "Invalid JWT token: ${result.exceptionOrNull()?.message}") }
        }
    }

    fun setCertInput(input: String) {
        _state.update { it.copy(certInputText = input) }
        parseCert(input)
    }

    fun parseCert(input: String) {
        val trimmed = input.trim()
        if (trimmed.isBlank()) {
            _state.update { it.copy(certReport = null, errorMessage = null) }
            return
        }

        // Avoid premature error while pasting/typing header
        if (trimmed.length < 20) {
            _state.update { it.copy(certReport = null, errorMessage = null) }
            return
        }

        val result = TokenInspectorUtils.parseX509Certificate(trimmed)
        if (result.isSuccess) {
            _state.update { it.copy(certReport = result.getOrNull(), errorMessage = null) }
        } else {
            _state.update { it.copy(certReport = null, errorMessage = "Failed to parse X.509 certificate: ${result.exceptionOrNull()?.message}") }
        }
    }

    fun clear() {
        _state.update { TokenInspectorState() }
    }
}
