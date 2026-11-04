package com.omnishield.utility.presentation.network

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CellWifi
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Lan
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omnishield.utility.core.network.DiscoveredDevice
import com.omnishield.utility.core.network.PortRiskLevel
import com.omnishield.utility.core.network.ScannedPort
import com.omnishield.utility.core.qr.QrCodeUtils
import com.omnishield.utility.core.storage.StorageUtils
import com.omnishield.utility.presentation.components.OmniShieldAppBar
import com.omnishield.utility.presentation.theme.SecurityAmber
import com.omnishield.utility.presentation.theme.SecurityCyan
import com.omnishield.utility.presentation.theme.SecurityGreen
import com.omnishield.utility.presentation.theme.SecurityPurple
import com.omnishield.utility.presentation.theme.SecurityRed

@Composable
fun NetworkStudioScreen(
    viewModel: NetworkStudioViewModel,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.setSendFile(it) }
    }

    Scaffold(
        topBar = {
            OmniShieldAppBar(
                title = "Network & Connectivity Studio",
                onNavigateBack = onNavigateBack
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            TabRow(
                selectedTabIndex = state.activeTab,
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
            ) {
                Tab(
                    selected = state.activeTab == 0,
                    onClick = { viewModel.selectTab(0) },
                    text = { Text("LAN Audit", fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Default.Lan, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = state.activeTab == 1,
                    onClick = { viewModel.selectTab(1) },
                    text = { Text("Port Checker", fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = state.activeTab == 2,
                    onClick = { viewModel.selectTab(2) },
                    text = { Text("P2P Transfer", fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
            }

            // Alerts
            if (state.errorMessage != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = SecurityRed.copy(alpha = 0.15f))
                ) {
                    Text(
                        text = state.errorMessage ?: "",
                        color = SecurityRed,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            if (state.statusMessage != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = SecurityGreen.copy(alpha = 0.15f))
                ) {
                    Text(
                        text = state.statusMessage ?: "",
                        color = SecurityGreen,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            when (state.activeTab) {
                0 -> LanAuditTabContent(
                    state = state,
                    onStartScan = { viewModel.startLanScan() },
                    onRefreshIp = { viewModel.refreshLocalIp() }
                )
                1 -> PortCheckerTabContent(
                    state = state,
                    onHostChange = { viewModel.setPortTargetHost(it) },
                    onStartScan = { viewModel.startPortScan() }
                )
                2 -> P2pTransferTabContent(
                    state = state,
                    onModeChange = { viewModel.setP2pMode(it) },
                    onPickFile = { filePicker.launch(arrayOf("*/*")) },
                    onStartServer = { viewModel.startP2pServer() },
                    onHostChange = { viewModel.setReceiveHost(it) },
                    onPortChange = { viewModel.setReceivePort(it) },
                    onPinChange = { viewModel.setReceivePin(it) },
                    onPasteCode = { viewModel.parseQrPayload(it) },
                    onStartReceive = { viewModel.startP2pReceive() },
                    onShareFile = { file ->
                        val uri = StorageUtils.getShareableUri(context, file)
                        val mime = StorageUtils.getMimeType(context, uri)
                        StorageUtils.shareFile(context, uri, mime, "Share Received File")
                    }
                )
            }
        }
    }
}

@Composable
private fun LanAuditTabContent(
    state: NetworkStudioState,
    onStartScan: () -> Unit,
    onRefreshIp: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Wifi, contentDescription = null, tint = SecurityCyan, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Local Wi-Fi Subnet", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                Text("IP: ${state.localIp}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                            }
                        }
                        IconButton(onClick = onRefreshIp) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = MaterialTheme.colorScheme.primary)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (state.isScanningLan) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Probing 254 subnet hosts...", style = MaterialTheme.typography.bodySmall)
                                Text("${(state.lanScanProgress * 100).toInt()}%", fontWeight = FontWeight.Bold, color = SecurityCyan)
                            }
                            LinearProgressIndicator(
                                progress = { state.lanScanProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = SecurityCyan
                            )
                        }
                    } else {
                        Button(
                            onClick = onStartScan,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Speed, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Scan Subnet for Active Devices")
                        }
                    }
                }
            }
        }

        if (state.discoveredDevices.isNotEmpty()) {
            item {
                Text(
                    text = "Discovered Devices (${state.discoveredDevices.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            items(state.discoveredDevices) { device ->
                DiscoveredDeviceCard(device = device)
            }
        }
    }
}

@Composable
private fun DiscoveredDeviceCard(device: DiscoveredDevice) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(if (device.isCurrentDevice) SecurityGreen.copy(alpha = 0.15f) else SecurityCyan.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when {
                        device.isCurrentDevice -> Icons.Default.PhoneAndroid
                        device.hostName.contains("router", ignoreCase = true) -> Icons.Default.Router
                        else -> Icons.Default.Computer
                    },
                    contentDescription = null,
                    tint = if (device.isCurrentDevice) SecurityGreen else SecurityCyan,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(device.ipAddress, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                    if (device.isCurrentDevice) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(SecurityGreen.copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("THIS DEVICE", color = SecurityGreen, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(device.hostName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                Text(
                    text = "MAC: ${device.macAddress} • ${device.vendorHint}",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.8f),
                    fontFamily = FontFamily.Monospace
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "${device.latencyMs} ms",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SecurityGreen
                )
            }
        }
    }
}

@Composable
private fun PortCheckerTabContent(
    state: NetworkStudioState,
    onHostChange: (String) -> Unit,
    onStartScan: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Target Host or IP", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)

                    OutlinedTextField(
                        value = state.portTargetHost,
                        onValueChange = onHostChange,
                        label = { Text("Hostname / IP Address") },
                        placeholder = { Text("127.0.0.1 or local gateway") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { onHostChange("127.0.0.1") },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Localhost", fontSize = 11.sp)
                        }
                        OutlinedButton(
                            onClick = {
                                val gateway = state.localIp.substringBeforeLast('.') + ".1"
                                onHostChange(gateway)
                            },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Gateway Router", fontSize = 11.sp)
                        }
                    }

                    if (state.isScanningPorts) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Probing sensitive service ports...", style = MaterialTheme.typography.bodySmall)
                                Text("${(state.portScanProgress * 100).toInt()}%", fontWeight = FontWeight.Bold, color = SecurityAmber)
                            }
                            LinearProgressIndicator(
                                progress = { state.portScanProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = SecurityAmber
                            )
                        }
                    } else {
                        Button(
                            onClick = onStartScan,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Scan Sensitive Service Ports")
                        }
                    }
                }
            }
        }

        if (state.openPorts.isNotEmpty()) {
            item {
                Text(
                    text = "Open Ports Detected (${state.openPorts.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            items(state.openPorts) { port ->
                ScannedPortCard(port = port)
            }
        }
    }
}

@Composable
private fun ScannedPortCard(port: ScannedPort) {
    val riskColor = when (port.riskLevel) {
        PortRiskLevel.CRITICAL -> SecurityRed
        PortRiskLevel.HIGH -> SecurityRed
        PortRiskLevel.MEDIUM -> SecurityAmber
        PortRiskLevel.LOW -> SecurityGreen
        PortRiskLevel.INFO -> SecurityCyan
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Port ${port.port}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(port.serviceName, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(riskColor.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(port.riskLevel.name, color = riskColor, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(3.dp))
                Text(port.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
            }
        }
    }
}

@Composable
private fun P2pTransferTabContent(
    state: NetworkStudioState,
    onModeChange: (Int) -> Unit,
    onPickFile: () -> Unit,
    onStartServer: () -> Unit,
    onHostChange: (String) -> Unit,
    onPortChange: (String) -> Unit,
    onPinChange: (String) -> Unit,
    onPasteCode: (String) -> Unit,
    onStartReceive: () -> Unit,
    onShareFile: (java.io.File) -> Unit
) {
    val context = LocalContext.current
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Zero-Cloud Architecture Banner
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = SecurityCyan.copy(alpha = 0.08f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, SecurityCyan.copy(alpha = 0.25f))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Wifi, contentDescription = null, tint = SecurityCyan, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Zero-Cloud Wireless P2P Transfer", fontWeight = FontWeight.Bold, color = SecurityCyan, fontSize = 13.sp)
                        Text(
                            "Beam large files directly between devices on local Wi-Fi or portable hotspot. 100% offline with on-the-fly AES-256-GCM encryption. Zero servers, zero tracking.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = { onModeChange(0) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (state.p2pMode == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Send File", color = if (state.p2pMode == 0) Color.White else MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Button(
                    onClick = { onModeChange(1) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (state.p2pMode == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Receive File", color = if (state.p2pMode == 1) Color.White else MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        if (state.p2pMode == 0) {
            // SENDER MODE
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(state.sendFileName ?: "No File Selected", fontWeight = FontWeight.Bold, maxLines = 1)
                            Text(state.sendFileSizeFormatted ?: "Select any file to beam over Wi-Fi", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                        }
                        Button(onClick = onPickFile, shape = RoundedCornerShape(12.dp)) {
                            Icon(Icons.Default.FileOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Pick File")
                        }
                    }
                }
            }

            item {
                Button(
                    onClick = onStartServer,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isServingP2p && state.sendFileUri != null,
                    shape = RoundedCornerShape(14.dp)
                ) {
                    if (state.isServingP2p) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Waiting for Receiver Connection...")
                    } else {
                        Text("Start Encrypted P2P Server")
                    }
                }
            }

            if (state.serverInfo != null) {
                item {
                    val info = state.serverInfo
                    val qrBitmap = remember(info.qrPayload) {
                        QrCodeUtils.generateQrBitmap(info.qrPayload, 500).getOrNull()
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("P2P Receiver Instructions", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            Text("Receiver connects to IP & Port with 6-digit PIN", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)

                            Spacer(modifier = Modifier.height(14.dp))

                            if (qrBitmap != null) {
                                Image(
                                    bitmap = qrBitmap.asImageBitmap(),
                                    contentDescription = "P2P QR Code",
                                    modifier = Modifier
                                        .size(180.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                )
                                Spacer(modifier = Modifier.height(14.dp))
                            }

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("IP Address", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                    Text(info.ipAddress, fontWeight = FontWeight.Bold)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Port", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                    Text(info.port.toString(), fontWeight = FontWeight.Bold)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Transfer PIN", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                    Text(info.pinCode, fontWeight = FontWeight.Black, color = SecurityCyan, fontSize = 18.sp)
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("P2P Connection Code", info.qrPayload))
                                    Toast.makeText(context, "Connection code copied to clipboard", Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Copy Connection Code", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        } else {
            // RECEIVER MODE
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Connect to Sender Peer", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)

                            OutlinedButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clipText = clipboard.primaryClip?.getItemAt(0)?.text?.toString() ?: ""
                                    if (clipText.isNotBlank()) {
                                        onPasteCode(clipText) // triggers parse via parseQrPayload
                                        Toast.makeText(context, "Connection code pasted", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Clipboard is empty", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Paste Code", fontSize = 11.sp)
                            }
                        }

                        OutlinedTextField(
                            value = state.receiveHostIp,
                            onValueChange = onHostChange,
                            label = { Text("Sender Host IP") },
                            placeholder = { Text("e.g. 192.168.1.45") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = state.receivePort,
                                onValueChange = onPortChange,
                                label = { Text("Port") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = state.receivePin,
                                onValueChange = onPinChange,
                                label = { Text("6-Digit PIN") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )
                        }

                        Button(
                            onClick = onStartReceive,
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !state.isReceivingP2p && state.receiveHostIp.isNotBlank() && state.receivePort.isNotBlank(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (state.isReceivingP2p) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Downloading & Decrypting...")
                            } else {
                                Text("Connect, Decrypt & Receive")
                            }
                        }
                    }
                }
            }

            if (state.receivedFile != null) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = SecurityGreen.copy(alpha = 0.12f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("File Received & Decrypted", fontWeight = FontWeight.Bold, color = SecurityGreen)
                                Text(state.receivedFile.name, style = MaterialTheme.typography.bodySmall)
                            }
                            Button(onClick = { onShareFile(state.receivedFile) }, shape = RoundedCornerShape(12.dp)) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Share")
                            }
                        }
                    }
                }
            }
        }
    }
}
