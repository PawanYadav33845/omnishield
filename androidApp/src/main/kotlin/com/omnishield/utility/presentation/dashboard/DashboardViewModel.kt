package com.omnishield.utility.presentation.dashboard

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class DashboardState(
    val offlineProtectionActive: Boolean = true,
    val hardwareCryptoAvailable: Boolean = true,
    val scopedStorageActive: Boolean = true,
    val processedFilesCount: Int = 0
)

class DashboardViewModel : ViewModel() {
    private val _state = MutableStateFlow(DashboardState())
    val state = _state.asStateFlow()
}
