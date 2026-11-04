package com.omnishield.utility.presentation.hardware

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.DeviceThermostat
import androidx.compose.material.icons.filled.ElectricMeter
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omnishield.utility.core.diagnostics.BatteryReport
import com.omnishield.utility.core.qr.QrCodeUtils
import com.omnishield.utility.core.storage.StorageUtils
import com.omnishield.utility.presentation.components.OmniShieldAppBar
import com.omnishield.utility.presentation.theme.SecurityAmber
import com.omnishield.utility.presentation.theme.SecurityCyan
import com.omnishield.utility.presentation.theme.SecurityGreen
import com.omnishield.utility.presentation.theme.SecurityRed

@Composable
fun HardwareStudioScreen(
    viewModel: HardwareStudioViewModel,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            OmniShieldAppBar(
                title = "Hardware & Radios Studio",
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
                    text = { Text("Battery & Thermals", fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Default.BatteryChargingFull, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = state.activeTab == 1,
                    onClick = { viewModel.selectTab(1) },
                    text = { Text("Air-Gapped QR Vault", fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Default.QrCode2, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
            }

            when (state.activeTab) {
                0 -> BatteryDiagnosticsTabContent(
                    state = state,
                    onRefresh = { viewModel.refreshBattery() }
                )
                1 -> QrVaultTabContent(
                    state = state,
                    onContentChange = { viewModel.setQrContent(it) },
                    onEcChange = { viewModel.setQrErrorCorrection(it) },
                    onGenerate = { viewModel.generateQr() },
                    onShareQr = { file ->
                        val uri = StorageUtils.getShareableUri(context, file)
                        StorageUtils.shareFile(context, uri, "image/png", "Share QR Code")
                    }
                )
            }
        }
    }
}

@Composable
private fun BatteryDiagnosticsTabContent(
    state: HardwareStudioState,
    onRefresh: () -> Unit
) {
    val report = state.batteryReport

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            val batteryLevel = report?.percentage ?: 0
            val batteryColor = when {
                batteryLevel > 50 -> SecurityGreen
                batteryLevel > 20 -> SecurityAmber
                else -> SecurityRed
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Real-Time Battery State", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(
                                if (report?.isCharging == true) "Charging via ${report.powerSource}" else "Discharging on Battery",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                        IconButton(onClick = onRefresh) {
                            if (state.isReadingBattery) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp))
                            } else {
                                Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "$batteryLevel%",
                                fontSize = 44.sp,
                                fontWeight = FontWeight.Black,
                                color = batteryColor
                            )
                            Text(
                                text = "Battery Health: ${report?.healthStatus ?: "Optimal (Good)"}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = SecurityGreen
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(batteryColor.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.BatteryChargingFull,
                                contentDescription = null,
                                tint = batteryColor,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                }
            }
        }

        item {
            Text("Diagnostic Metrics", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricCard(
                    modifier = Modifier.weight(1f),
                    title = "Temperature",
                    value = "${report?.temperatureCelsius ?: "25.0"} °C",
                    icon = Icons.Default.DeviceThermostat,
                    tint = SecurityCyan
                )
                MetricCard(
                    modifier = Modifier.weight(1f),
                    title = "Voltage",
                    value = report?.voltageVolts ?: "3.85 V",
                    icon = Icons.Default.ElectricMeter,
                    tint = SecurityAmber
                )
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricCard(
                    modifier = Modifier.weight(1f),
                    title = "Power Source",
                    value = report?.powerSource ?: "Battery Discharging",
                    icon = Icons.Default.Bolt,
                    tint = MaterialTheme.colorScheme.primary
                )
                MetricCard(
                    modifier = Modifier.weight(1f),
                    title = "Technology",
                    value = report?.technology ?: "Li-ion",
                    icon = Icons.Default.Memory,
                    tint = SecurityGreen
                )
            }
        }
    }
}

@Composable
private fun MetricCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = title, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
            Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun QrVaultTabContent(
    state: HardwareStudioState,
    onContentChange: (String) -> Unit,
    onEcChange: (QrCodeUtils.QrErrorCorrection) -> Unit,
    onGenerate: () -> Unit,
    onShareQr: (java.io.File) -> Unit
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
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = SecurityCyan.copy(alpha = 0.1f))
            ) {
                Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = SecurityCyan, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        "Air-Gapped: Generates QR codes directly inside device memory without communicating with any external API or network.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            OutlinedTextField(
                value = state.qrContent,
                onValueChange = onContentChange,
                label = { Text("Payload to Encode") },
                placeholder = { Text("Enter text, URL, Wi-Fi config, or secret key") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                minLines = 2
            )
        }

        item {
            Text("Error Correction Level:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(4.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                QrCodeUtils.QrErrorCorrection.entries.forEach { ec ->
                    val isSelected = state.qrErrorCorrection == ec
                    OutlinedButton(
                        onClick = { onEcChange(ec) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(ec.name.first().toString(), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            Button(
                onClick = onGenerate,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Render Offline QR")
            }
        }

        if (state.qrBitmap != null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Image(
                            bitmap = state.qrBitmap.asImageBitmap(),
                            contentDescription = "Air-Gapped QR Code",
                            modifier = Modifier
                                .size(240.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        if (state.qrSavedFile != null) {
                            Button(
                                onClick = { onShareQr(state.qrSavedFile) },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Share QR Code Image")
                            }
                        }
                    }
                }
            }
        }
    }
}
