package com.omnishield.utility.presentation.security

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omnishield.utility.core.diagnostics.AppPrivacyRisk
import com.omnishield.utility.core.storage.StorageUtils
import com.omnishield.utility.presentation.components.OmniShieldAppBar
import com.omnishield.utility.presentation.theme.SecurityAmber
import com.omnishield.utility.presentation.theme.SecurityCyan
import com.omnishield.utility.presentation.theme.SecurityGreen
import com.omnishield.utility.presentation.theme.SecurityRed

@Composable
fun SecurityStudioScreen(
    viewModel: SecurityStudioViewModel,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    val shredderPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.setShredderFile(it) }
    }

    val stegoCarrierPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.setStegoCarrier(it) }
    }

    var showShredderConfirmDialog by remember { mutableStateOf(false) }

    if (showShredderConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showShredderConfirmDialog = false },
            title = { Text("Confirm Cryptographic Erasure", fontWeight = FontWeight.Bold) },
            text = {
                Text("This will perform a DoD 5220.22-M 3-pass wipe (zeros, ones, CSPRNG noise) and sanitize '${state.shredderFileName}'. This operation CANNOT be undone by any recovery software.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showShredderConfirmDialog = false
                        viewModel.executeShred()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SecurityRed)
                ) {
                    Text("Permanently Shred")
                }
            },
            dismissButton = {
                TextButton(onClick = { showShredderConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            OmniShieldAppBar(
                title = "Security & Privacy Studio",
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
                    text = { Text("File Shredder", fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = state.activeTab == 1,
                    onClick = { viewModel.selectTab(1) },
                    text = { Text("Steganography", fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Default.VisibilityOff, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = state.activeTab == 2,
                    onClick = { viewModel.selectTab(2) },
                    text = { Text("Privacy Auditor", fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
            }

            // Messages
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
                0 -> ShredderTabContent(
                    state = state,
                    onPickFile = { shredderPicker.launch(arrayOf("*/*")) },
                    onConfirmShred = { showShredderConfirmDialog = true }
                )
                1 -> SteganographyTabContent(
                    state = state,
                    onModeChange = { viewModel.setStegoMode(it) },
                    onPickCarrier = { stegoCarrierPicker.launch(arrayOf("image/*")) },
                    onSecretChange = { viewModel.setStegoSecret(it) },
                    onEmbed = { viewModel.executeStegoEmbed() },
                    onExtract = { viewModel.executeStegoExtract() },
                    onShareOutput = { file ->
                        val uri = StorageUtils.getShareableUri(context, file)
                        StorageUtils.shareFile(context, uri, "image/png", "Share Stego Image")
                    }
                )
                2 -> PrivacyAuditorTabContent(
                    state = state,
                    onSearchChange = { viewModel.setSearchQuery(it) },
                    onRefresh = { viewModel.refreshAppAudit() }
                )
            }
        }
    }
}

@Composable
private fun ShredderTabContent(
    state: SecurityStudioState,
    onPickFile: () -> Unit,
    onConfirmShred: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = SecurityRed.copy(alpha = 0.1f))
        ) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = SecurityRed, modifier = Modifier.size(28.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("DoD 5220.22-M Compliance", fontWeight = FontWeight.Bold, color = SecurityRed)
                    Text(
                        "Writes 3 cryptographic passes (Pass 1: 0x00, Pass 2: 0xFF, Pass 3: CSPRNG Random) to permanently defeat magnetic & flash forensic analysis.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

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
                    Text(state.shredderFileName ?: "No File Selected", fontWeight = FontWeight.Bold, maxLines = 1)
                    Text(state.shredderFileSizeFormatted ?: "Pick any document, image, or database file", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                }
                Button(onClick = onPickFile, shape = RoundedCornerShape(12.dp)) {
                    Icon(Icons.Default.FileOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Select File")
                }
            }
        }

        if (state.isShredding) {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Overwriting in Progress...", fontWeight = FontWeight.SemiBold)
                    Text("${(state.shredderProgress * 100).toInt()}%", fontWeight = FontWeight.Bold, color = SecurityRed)
                }
                LinearProgressIndicator(
                    progress = { state.shredderProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = SecurityRed
                )
            }
        }

        Button(
            onClick = onConfirmShred,
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.isShredding && state.shredderFileUri != null,
            colors = ButtonDefaults.buttonColors(containerColor = SecurityRed),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Execute DoD 3-Pass Shred")
        }
    }
}

@Composable
private fun SteganographyTabContent(
    state: SecurityStudioState,
    onModeChange: (Int) -> Unit,
    onPickCarrier: () -> Unit,
    onSecretChange: (String) -> Unit,
    onEmbed: () -> Unit,
    onExtract: () -> Unit,
    onShareOutput: (java.io.File) -> Unit
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = { onModeChange(0) },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (state.stegoMode == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    "Conceal Secret",
                    color = if (state.stegoMode == 0) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Button(
                onClick = { onModeChange(1) },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (state.stegoMode == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    "Extract Secret",
                    color = if (state.stegoMode == 1) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

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
                    Text(
                        if (state.stegoCarrierUri != null) "Image Selected" else "No Carrier Image",
                        fontWeight = FontWeight.Bold
                    )
                    Text("Select a PNG/JPEG image", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                }
                Button(onClick = onPickCarrier, shape = RoundedCornerShape(12.dp)) {
                    Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Pick Image")
                }
            }
        }

        if (state.stegoMode == 0) {
            OutlinedTextField(
                value = state.stegoSecretText,
                onValueChange = onSecretChange,
                label = { Text("Confidential Secret Message") },
                placeholder = { Text("Enter plain text or passphrase to hide in pixel LSBs") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                minLines = 3
            )

            Button(
                onClick = onEmbed,
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isStegoProcessing && state.stegoCarrierUri != null && state.stegoSecretText.isNotBlank(),
                shape = RoundedCornerShape(14.dp)
            ) {
                if (state.isStegoProcessing) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White)
                } else {
                    Text("Conceal Inside PNG Carrier")
                }
            }

            if (state.stegoOutputImage != null) {
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
                            Text("Stego Image Generated", fontWeight = FontWeight.Bold, color = SecurityGreen)
                            Text(state.stegoOutputImage.name, style = MaterialTheme.typography.bodySmall)
                        }
                        Button(onClick = { onShareOutput(state.stegoOutputImage) }, shape = RoundedCornerShape(12.dp)) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Share")
                        }
                    }
                }
            }
        } else {
            Button(
                onClick = onExtract,
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isStegoProcessing && state.stegoCarrierUri != null,
                shape = RoundedCornerShape(14.dp)
            ) {
                if (state.isStegoProcessing) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White)
                } else {
                    Text("Extract Hidden Secret")
                }
            }

            if (state.stegoExtractedText != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Decoded Secret Message:", fontWeight = FontWeight.Bold, color = SecurityCyan)
                            IconButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Decoded Secret", state.stegoExtractedText))
                                    Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(18.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = state.stegoExtractedText ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PrivacyAuditorTabContent(
    state: SecurityStudioState,
    onSearchChange: (String) -> Unit,
    onRefresh: () -> Unit
) {
    val filteredReports = remember(state.appReports, state.searchQuery) {
        if (state.searchQuery.isBlank()) state.appReports
        else state.appReports.filter {
            it.appName.contains(state.searchQuery, ignoreCase = true) ||
            it.packageName.contains(state.searchQuery, ignoreCase = true)
        }
    }

    val criticalCount = remember(state.appReports) {
        state.appReports.count { it.riskLevel == "Critical" }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
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
                    Column {
                        Text(
                            "${state.appReports.size} Installed Apps Analyzed",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            "$criticalCount app(s) have CRITICAL permission exposure",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (criticalCount > 0) SecurityRed else SecurityGreen,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    IconButton(onClick = onRefresh) {
                        if (state.isScanningApps) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp))
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }

        item {
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = onSearchChange,
                placeholder = { Text("Search installed applications...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                singleLine = true
            )
        }

        items(filteredReports) { report ->
            AppPrivacyCard(report = report)
        }
    }
}

@Composable
private fun AppPrivacyCard(report: AppPrivacyRisk) {
    val riskColor = when (report.riskLevel) {
        "Critical" -> SecurityRed
        "Moderate" -> SecurityAmber
        else -> SecurityGreen
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
                    Text(report.appName, fontWeight = FontWeight.Bold, maxLines = 1)
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(riskColor.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${report.riskLevel} (${report.riskScore})",
                            color = riskColor,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(report.packageName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline, maxLines = 1)
                Spacer(modifier = Modifier.height(6.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (report.sensitivePermissions.contains("Camera")) PermissionMiniIcon(icon = Icons.Default.CameraAlt, desc = "Camera")
                    if (report.sensitivePermissions.contains("Microphone")) PermissionMiniIcon(icon = Icons.Default.Mic, desc = "Microphone")
                    if (report.sensitivePermissions.contains("Fine Location")) PermissionMiniIcon(icon = Icons.Default.LocationOn, desc = "Location")
                    if (report.sensitivePermissions.contains("Contacts")) PermissionMiniIcon(icon = Icons.Default.Contacts, desc = "Contacts")
                    if (report.sensitivePermissions.contains("SMS")) PermissionMiniIcon(icon = Icons.Default.Sms, desc = "SMS")
                }
            }
        }
    }
}

@Composable
private fun PermissionMiniIcon(icon: ImageVector, desc: String) {
    Box(
        modifier = Modifier
            .size(24.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        Icon(imageVector = icon, contentDescription = desc, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
