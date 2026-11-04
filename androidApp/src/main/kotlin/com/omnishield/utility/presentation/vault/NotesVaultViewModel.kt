package com.omnishield.utility.presentation.vault

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omnishield.utility.core.vault.NoteCategory
import com.omnishield.utility.core.vault.NotesVaultUtils
import com.omnishield.utility.core.vault.SecretNote
import com.omnishield.utility.core.vault.VaultUnlockResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class NotesVaultState(
    val isConfigured: Boolean = false,
    val isUnlocked: Boolean = false,
    val isDecoyMode: Boolean = false,
    val activePin: String = "",
    val notes: List<SecretNote> = emptyList(),
    // Setup state
    val showSetupDialog: Boolean = false,
    val setupMasterPin: String = "",
    val setupDuressPin: String = "",
    // New Note state
    val showAddNoteDialog: Boolean = false,
    val newNoteTitle: String = "",
    val newNoteContent: String = "",
    val newNoteCategory: NoteCategory = NoteCategory.PERSONAL,
    // Common
    val errorMessage: String? = null,
    val statusMessage: String? = null
)

class NotesVaultViewModel(
    private val context: Context
) : ViewModel() {

    private val _state = MutableStateFlow(NotesVaultState())
    val state: StateFlow<NotesVaultState> = _state.asStateFlow()

    init {
        checkVaultStatus()
    }

    fun checkVaultStatus() {
        val configured = NotesVaultUtils.isConfigured(context)
        _state.update { it.copy(isConfigured = configured, showSetupDialog = !configured) }
    }

    // PIN Authentication
    fun unlockWithPin(pin: String) {
        if (pin.length < 4) {
            _state.update { it.copy(errorMessage = "PIN must be at least 4 digits.") }
            return
        }

        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                NotesVaultUtils.unlockVault(context, pin)
            }

            when (result) {
                is VaultUnlockResult.Success -> {
                    _state.update {
                        it.copy(
                            isUnlocked = true,
                            isDecoyMode = result.isDecoy,
                            activePin = pin,
                            notes = result.notes,
                            errorMessage = null,
                            statusMessage = if (result.isDecoy) "Vault Unlocked" else "Secure Vault Unlocked"
                        )
                    }
                }
                is VaultUnlockResult.IncorrectPin -> {
                    _state.update { it.copy(errorMessage = "Incorrect PIN code.") }
                }
                is VaultUnlockResult.NotConfigured -> {
                    _state.update { it.copy(showSetupDialog = true, errorMessage = "Vault not yet configured.") }
                }
            }
        }
    }

    fun lockVault() {
        _state.update {
            it.copy(
                isUnlocked = false,
                isDecoyMode = false,
                activePin = "",
                notes = emptyList(),
                statusMessage = "Vault locked and memory cleared."
            )
        }
    }

    // Setup Actions
    fun setSetupMasterPin(pin: String) {
        _state.update { it.copy(setupMasterPin = pin) }
    }

    fun setSetupDuressPin(pin: String) {
        _state.update { it.copy(setupDuressPin = pin) }
    }

    fun executeSetup() {
        val master = _state.value.setupMasterPin.trim()
        val duress = _state.value.setupDuressPin.trim()

        if (master.length < 4 || duress.length < 4) {
            _state.update { it.copy(errorMessage = "Both PINs must be at least 4 digits.") }
            return
        }
        if (master == duress) {
            _state.update { it.copy(errorMessage = "Duress Decoy PIN must be different from Master PIN.") }
            return
        }

        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                NotesVaultUtils.setupVault(context, master, duress)
            }
            _state.update {
                it.copy(
                    isConfigured = true,
                    showSetupDialog = false,
                    setupMasterPin = "",
                    setupDuressPin = "",
                    statusMessage = "Master PIN and Duress Decoy PIN configured!"
                )
            }
        }
    }

    fun resetVault() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                NotesVaultUtils.resetVault(context)
            }
            _state.update {
                NotesVaultState(
                    isConfigured = false,
                    showSetupDialog = true,
                    statusMessage = "Vault wiped. You can now configure fresh PINs."
                )
            }
        }
    }

    // Note Actions
    fun openAddNote() {
        _state.update {
            it.copy(
                showAddNoteDialog = true,
                newNoteTitle = "",
                newNoteContent = "",
                newNoteCategory = NoteCategory.PERSONAL
            )
        }
    }

    fun closeAddNote() {
        _state.update { it.copy(showAddNoteDialog = false) }
    }

    fun setNewNoteTitle(title: String) {
        _state.update { it.copy(newNoteTitle = title) }
    }

    fun setNewNoteContent(content: String) {
        _state.update { it.copy(newNoteContent = content) }
    }

    fun setNewNoteCategory(category: NoteCategory) {
        _state.update { it.copy(newNoteCategory = category) }
    }

    fun saveNote() {
        val title = _state.value.newNoteTitle.trim()
        val content = _state.value.newNoteContent.trim()
        val category = _state.value.newNoteCategory
        val pin = _state.value.activePin

        if (title.isBlank() || content.isBlank()) {
            _state.update { it.copy(errorMessage = "Title and content cannot be blank.") }
            return
        }

        val newNote = SecretNote(title = title, content = content, category = category)
        val updatedNotes = listOf(newNote) + _state.value.notes

        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                NotesVaultUtils.saveVaultNotes(context, pin, updatedNotes, _state.value.isDecoyMode)
            }
            _state.update {
                it.copy(
                    notes = updatedNotes,
                    showAddNoteDialog = false,
                    statusMessage = "Note securely encrypted and saved."
                )
            }
        }
    }

    fun deleteNote(id: String) {
        val updatedNotes = _state.value.notes.filter { it.id != id }
        val pin = _state.value.activePin

        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                NotesVaultUtils.saveVaultNotes(context, pin, updatedNotes, _state.value.isDecoyMode)
            }
            _state.update { it.copy(notes = updatedNotes, statusMessage = "Note deleted.") }
        }
    }

    fun clearMessages() {
        _state.update { it.copy(errorMessage = null, statusMessage = null) }
    }
}
