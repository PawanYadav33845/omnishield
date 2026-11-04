package com.omnishield.utility.presentation.network

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omnishield.utility.core.network.DiscoveredDevice
import com.omnishield.utility.core.network.LanScannerUtils
import com.omnishield.utility.core.network.P2pServerInfo
import com.omnishield.utility.core.network.P2pTransferUtils
import com.omnishield.utility.core.network.PortScannerUtils
import com.omnishield.utility.core.network.ScannedPort
import com.omnishield.utility.core.storage.StorageUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

data class NetworkStudioState(
    val activeTab: Int = 0, // 0: LAN Audit, 1: Port Checker, 2: P2P Transfer
    // LAN Scanner
    val localIp: String = "Detecting...",
    val isScanningLan: Boolean = false,
    val lanScanProgress: Float = 0f,
    val discoveredDevices: List<DiscoveredDevice> = emptyList(),
    // Port Scanner
    val portTargetHost: String = "127.0.0.1",
    val isScanningPorts: Boolean = false,
    val portScanProgress: Float = 0f,
    val openPorts: List<ScannedPort> = emptyList(),
    // P2P Transfer
    val p2pMode: Int = 0, // 0: Send, 1: Receive
    val sendFileUri: Uri? = null,
    val sendFileName: String? = null,
    val sendFileSizeFormatted: String? = null,
    val serverInfo: P2pServerInfo? = null,
    val isServingP2p: Boolean = false,
    val p2pSendProgress: Float = 0f,
    val receiveHostIp: String = "",
    val receivePort: String = "",
    val receivePin: String = "",
    val isReceivingP2p: Boolean = false,
    val p2pReceiveProgress: Float = 0f,
    val receivedFile: File? = null,
    // Common
    val errorMessage: String? = null,
    val statusMessage: String? = null
)

class NetworkStudioViewModel(
    private val context: Context
) : ViewModel() {

    private val _state = MutableStateFlow(NetworkStudioState())
    val state: StateFlow<NetworkStudioState> = _state.asStateFlow()

    init {
        refreshLocalIp()
    }

    fun selectTab(tab: Int) {
        _state.update { it.copy(activeTab = tab, errorMessage = null, statusMessage = null) }
    }

    fun refreshLocalIp() {
        val ip = LanScannerUtils.getLocalIpAddress() ?: "Not Connected"
        _state.update { it.copy(localIp = ip) }
    }

    // LAN Scanner Actions
    fun startLanScan() {
        val currentIp = LanScannerUtils.getLocalIpAddress()
        if (currentIp == null) {
            _state.update {
                it.copy(
                    localIp = "Not Connected",
                    isScanningLan = false,
                    errorMessage = "Cannot scan LAN: Device is not connected to a local Wi-Fi network or hotspot."
                )
            }
            return
        }

        viewModelScope.launch {
            _state.update {
                it.copy(
                    localIp = currentIp,
                    isScanningLan = true,
                    lanScanProgress = 0f,
                    discoveredDevices = emptyList(),
                    errorMessage = null,
                    statusMessage = "Scanning subnet (${currentIp.substringBeforeLast('.')}.0/24)..."
                )
            }
            try {
                val devices = LanScannerUtils.scanSubnet { scanned, total, _ ->
                    val prog = scanned.toFloat() / total.toFloat()
                    _state.update { it.copy(lanScanProgress = prog) }
                }
                _state.update {
                    it.copy(
                        isScanningLan = false,
                        lanScanProgress = 1f,
                        discoveredDevices = devices,
                        statusMessage = "Discovered ${devices.size} device(s) on local subnet"
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(isScanningLan = false, errorMessage = e.localizedMessage) }
            }
        }
    }

    // Port Scanner Actions
    fun setPortTargetHost(host: String) {
        _state.update { it.copy(portTargetHost = host) }
    }

    fun startPortScan() {
        val target = _state.value.portTargetHost.trim()
        if (target.isBlank()) {
            _state.update { it.copy(errorMessage = "Please enter target host or IP.") }
            return
        }

        viewModelScope.launch {
            _state.update {
                it.copy(
                    isScanningPorts = true,
                    portScanProgress = 0f,
                    openPorts = emptyList(),
                    errorMessage = null,
                    statusMessage = "Scanning sensitive ports on $target..."
                )
            }
            try {
                val ports = PortScannerUtils.scanPorts(target) { scanned, total, _ ->
                    val prog = scanned.toFloat() / total.toFloat()
                    _state.update { it.copy(portScanProgress = prog) }
                }
                _state.update {
                    it.copy(
                        isScanningPorts = false,
                        portScanProgress = 1f,
                        openPorts = ports,
                        statusMessage = "Scan complete. Found ${ports.size} open port(s)."
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(isScanningPorts = false, errorMessage = e.localizedMessage) }
            }
        }
    }

    // P2P Transfer Actions
    fun setP2pMode(mode: Int) {
        _state.update { it.copy(p2pMode = mode, errorMessage = null, statusMessage = null) }
    }

    fun setSendFile(uri: Uri) {
        val name = StorageUtils.getFileName(context, uri)
        val size = StorageUtils.getFileSize(context, uri)
        _state.update {
            it.copy(
                sendFileUri = uri,
                sendFileName = name,
                sendFileSizeFormatted = StorageUtils.formatBytes(size),
                serverInfo = null
            )
        }
    }

    fun startP2pServer() {
        val uri = _state.value.sendFileUri ?: run {
            _state.update { it.copy(errorMessage = "Please select a file to transfer.") }
            return
        }
        val pin = P2pTransferUtils.generatePin()

        viewModelScope.launch {
            _state.update { it.copy(isServingP2p = true, errorMessage = null, statusMessage = "Starting encrypted socket server...") }
            val result = P2pTransferUtils.serveFile(
                context = context,
                fileUri = uri,
                pin = pin,
                onBound = { info ->
                    _state.update { it.copy(serverInfo = info, statusMessage = "Server listening. Connect receiver peer.") }
                },
                onProgress = { sent, total ->
                    val prog = if (total > 0) sent.toFloat() / total.toFloat() else 0f
                    _state.update { it.copy(p2pSendProgress = prog) }
                }
            )

            if (result.isSuccess) {
                _state.update {
                    it.copy(
                        isServingP2p = false,
                        p2pSendProgress = 1f,
                        statusMessage = "P2P Transfer completed successfully!"
                    )
                }
            } else {
                _state.update {
                    it.copy(
                        isServingP2p = false,
                        errorMessage = "Transfer failed: ${result.exceptionOrNull()?.message}"
                    )
                }
            }
        }
    }

    fun setReceiveHost(ip: String) {
        _state.update { it.copy(receiveHostIp = ip) }
    }

    fun setReceivePort(port: String) {
        _state.update { it.copy(receivePort = port) }
    }

    fun setReceivePin(pin: String) {
        _state.update { it.copy(receivePin = pin) }
    }

    fun parseQrPayload(payload: String) {
        val clean = payload.trim().removePrefix("OMNIP2P:")
        val parts = clean.split(":")
        if (parts.size >= 3) {
            _state.update {
                it.copy(
                    receiveHostIp = parts[0].trim(),
                    receivePort = parts[1].trim(),
                    receivePin = parts[2].trim(),
                    statusMessage = "Peer details loaded: ${parts[0]}:${parts[1]}"
                )
            }
        } else if (parts.size == 2) {
            _state.update {
                it.copy(
                    receiveHostIp = parts[0].trim(),
                    receivePort = parts[1].trim(),
                    statusMessage = "Host and Port loaded: ${parts[0]}:${parts[1]}"
                )
            }
        } else if (clean.matches(Regex("^\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}$"))) {
            _state.update {
                it.copy(
                    receiveHostIp = clean,
                    statusMessage = "Host IP loaded: $clean"
                )
            }
        }
    }

    fun startP2pReceive() {
        val host = _state.value.receiveHostIp.trim()
        val port = _state.value.receivePort.toIntOrNull() ?: run {
            _state.update { it.copy(errorMessage = "Invalid server port.") }
            return
        }
        val pin = _state.value.receivePin.trim()
        if (pin.length < 4) {
            _state.update { it.copy(errorMessage = "Enter valid 6-digit transfer PIN.") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isReceivingP2p = true, errorMessage = null, statusMessage = "Connecting to peer $host:$port...") }
            val result = P2pTransferUtils.receiveFile(
                context = context,
                hostIp = host,
                port = port,
                pin = pin,
                onMetadataReceived = { fileName, totalBytes ->
                    _state.update { it.copy(statusMessage = "Receiving $fileName (${StorageUtils.formatBytes(totalBytes)})...") }
                },
                onProgress = { rec, total ->
                    val prog = if (total > 0) rec.toFloat() / total.toFloat() else 0f
                    _state.update { it.copy(p2pReceiveProgress = prog) }
                }
            )

            if (result.isSuccess) {
                val file = result.getOrThrow()
                _state.update {
                    it.copy(
                        isReceivingP2p = false,
                        receivedFile = file,
                        statusMessage = "File received & decrypted successfully: ${file.name}"
                    )
                }
            } else {
                _state.update {
                    it.copy(
                        isReceivingP2p = false,
                        errorMessage = "Download failed: ${result.exceptionOrNull()?.message}"
                    )
                }
            }
        }
    }

    fun clearMessages() {
        _state.update { it.copy(errorMessage = null, statusMessage = null) }
    }
}
