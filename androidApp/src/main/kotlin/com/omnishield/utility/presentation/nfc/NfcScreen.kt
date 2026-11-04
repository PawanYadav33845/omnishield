package com.omnishield.utility.presentation.nfc

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omnishield.utility.domain.model.NfcScanState
import com.omnishield.utility.domain.model.NfcTagModel
import com.omnishield.utility.domain.model.NfcWriteState
import com.omnishield.utility.domain.model.NfcWriteType
import com.omnishield.utility.presentation.components.ErrorBanner
import com.omnishield.utility.presentation.components.OmniShieldAppBar
import com.omnishield.utility.presentation.components.OperationSuccessCard
import com.omnishield.utility.presentation.theme.SecurityAmber
import com.omnishield.utility.presentation.theme.SecurityCyan
import com.omnishield.utility.presentation.theme.SecurityGreen
import com.omnishield.utility.presentation.theme.SecurityRed

@Composable
fun NfcScreen(
    viewModel: NfcViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.checkHardware(context)
    }

    Scaffold(
        topBar = {
            OmniShieldAppBar(
                title = "NFC Tag Toolkit",
                canNavigateBack = true,
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
                selectedTabIndex = uiState.currentTab.ordinal,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Tab(
                    selected = uiState.currentTab == NfcTab.READ_TAG,
                    onClick = { viewModel.setTab(NfcTab.READ_TAG) },
                    text = { Text("Read Tag") },
                    icon = { Icon(Icons.Default.Nfc, contentDescription = null) }
                )
                Tab(
                    selected = uiState.currentTab == NfcTab.WRITE_TAG,
                    onClick = { viewModel.setTab(NfcTab.WRITE_TAG) },
                    text = { Text("Write Tag") },
                    icon = { Icon(Icons.Default.Edit, contentDescription = null) }
                )
                Tab(
                    selected = uiState.currentTab == NfcTab.SAVED_VAULT,
                    onClick = { viewModel.setTab(NfcTab.SAVED_VAULT) },
                    text = { Text("Vault (${uiState.savedTags.size})") },
                    icon = { Icon(Icons.Default.Save, contentDescription = null) }
                )
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Hardware Status Banner
                item {
                    NfcHardwareBanner(
                        isSupported = uiState.isSupported,
                        isEnabled = uiState.isEnabled,
                        onOpenSettings = {
                            context.startActivity(Intent(Settings.ACTION_NFC_SETTINGS))
                        }
                    )
                }

                when (uiState.currentTab) {
                    NfcTab.READ_TAG -> {
                        item {
                            ReadTagSection(
                                scanState = uiState.scanState,
                                onStartListening = { viewModel.startListening() },
                                onSaveToVault = { tag ->
                                    viewModel.saveTag(context, tag)
                                    Toast.makeText(context, "Saved to NFC Vault!", Toast.LENGTH_SHORT).show()
                                },
                                onCopyId = { id ->
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("NFC Tag UID", id))
                                    Toast.makeText(context, "Copied UID: $id", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                    NfcTab.WRITE_TAG -> {
                        item {
                            WriteTagSection(
                                writeState = uiState.writeState,
                                currentType = uiState.writeType,
                                onPrepareWrite = { type, content ->
                                    viewModel.prepareWrite(type, content)
                                },
                                onCancelWrite = { viewModel.cancelWrite() }
                            )
                        }
                    }
                    NfcTab.SAVED_VAULT -> {
                        item {
                            VaultSection(
                                savedTags = uiState.savedTags,
                                onDelete = { id -> viewModel.deleteTag(context, id) },
                                onCopy = { text ->
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("NFC Record", text))
                                    Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NfcHardwareBanner(
    isSupported: Boolean,
    isEnabled: Boolean,
    onOpenSettings: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                !isSupported -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                !isEnabled -> SecurityAmber.copy(alpha = 0.15f)
                else -> SecurityGreen.copy(alpha = 0.12f)
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isEnabled) Icons.Default.Nfc else Icons.Default.Warning,
                contentDescription = null,
                tint = if (isEnabled) SecurityGreen else SecurityAmber,
                modifier = Modifier.size(26.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = when {
                        !isSupported -> "NFC Sensor Not Detected"
                        !isEnabled -> "NFC Is Turned Off"
                        else -> "NFC Controller Active"
                    },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = when {
                        !isSupported -> "This device lacks an NFC transceiver."
                        !isEnabled -> "Enable NFC in device settings to read and write tags."
                        else -> "Ready to detect contactless NDEF and Mifare tags."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
            if (isSupported && !isEnabled) {
                OutlinedButton(onClick = onOpenSettings) {
                    Text("Enable")
                }
            }
        }
    }
}

@Composable
private fun ReadTagSection(
    scanState: NfcScanState,
    onStartListening: () -> Unit,
    onSaveToVault: (NfcTagModel) -> Unit,
    onCopyId: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Radar Pulse Scanner Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                NfcPulseWave()
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Hold Tag Against Back of Device",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Reads Tag UID, memory capacity, NDEF records & technology profiles",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }

        when (scanState) {
            is NfcScanState.TagDetected -> {
                val tag = scanState.tag
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "Tag Detected", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = SecurityGreen)
                            Button(onClick = { onSaveToVault(tag) }) {
                                Icon(Icons.Default.BookmarkAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Save to Vault")
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .clickable { onCopyId(tag.tagIdHex) }
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Serial Number (UID)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                                Text(tag.tagIdHex, style = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Monospace), fontWeight = FontWeight.Bold)
                            }
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy UID", modifier = Modifier.size(18.dp))
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        NfcTagRow(label = "Memory Capacity", value = if (tag.maxSizeByte > 0) "${tag.maxSizeByte} Bytes" else "Standard")
                        NfcTagRow(label = "Writable Status", value = if (tag.isWritable) "Read / Write" else "Locked (Read-Only)")
                        NfcTagRow(label = "Technologies", value = tag.technologies.joinToString(", "))

                        if (tag.records.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Text("NDEF Records (${tag.records.size})", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            tag.records.forEach { rec ->
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                        .padding(10.dp)
                                ) {
                                    Text(rec.typeName, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                    Text(rec.payloadDisplay, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }
                }
            }
            is NfcScanState.Error -> {
                ErrorBanner(errorMessage = scanState.message)
            }
            else -> {}
        }
    }
}

@Composable
private fun NfcPulseWave() {
    val infiniteTransition = rememberInfiniteTransition(label = "NfcPulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseScale"
    )

    Box(contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(90.dp)
                .scale(scale)
                .clip(CircleShape)
                .background(SecurityCyan.copy(alpha = 0.15f))
        )
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(SecurityCyan),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Nfc, contentDescription = null, tint = Color.White, modifier = Modifier.size(36.dp))
        }
    }
}

@Composable
private fun WriteTagSection(
    writeState: NfcWriteState,
    currentType: NfcWriteType,
    onPrepareWrite: (NfcWriteType, String) -> Unit,
    onCancelWrite: () -> Unit
) {
    var contentInput by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("") }
    var selectedType by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(NfcWriteType.TEXT) }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text("Write to NFC Tag", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(10.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(NfcWriteType.TEXT to "Plain Text", NfcWriteType.URI to "Web URL", NfcWriteType.MIME_JSON to "JSON Payload").forEach { (type, label) ->
                        FilterChip(
                            selected = selectedType == type,
                            onClick = { selectedType = type },
                            label = { Text(label) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = contentInput,
                    onValueChange = { contentInput = it },
                    label = {
                        Text(when (selectedType) {
                            NfcWriteType.URI -> "e.g. https://github.com"
                            NfcWriteType.TEXT -> "Text to write into tag"
                            NfcWriteType.MIME_JSON -> "{\"device\":\"OmniShield\"}"
                        })
                    },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )

                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { onPrepareWrite(selectedType, contentInput) },
                    enabled = contentInput.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Prepare Write (Tap Tag Next)")
                }
            }
        }

        when (writeState) {
            is NfcWriteState.ReadyToTap -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SecurityAmber.copy(alpha = 0.15f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Nfc, contentDescription = null, tint = SecurityAmber)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Ready: Tap an NFC Tag Now", fontWeight = FontWeight.Bold, color = SecurityAmber)
                        }
                        Text("Hold your blank or rewritable tag to the antenna to flash payload.", style = MaterialTheme.typography.bodySmall)
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(onClick = onCancelWrite) { Text("Cancel") }
                    }
                }
            }
            is NfcWriteState.Success -> {
                OperationSuccessCard(
                    title = "Tag Written Successfully!",
                    description = "Written ${writeState.bytesWritten} bytes to Tag UID ${writeState.tagIdHex}.",
                    onShare = {}
                )
            }
            is NfcWriteState.Error -> {
                ErrorBanner(errorMessage = writeState.message)
            }
            else -> {}
        }
    }
}

@Composable
private fun VaultSection(
    savedTags: List<NfcTagModel>,
    onDelete: (String) -> Unit,
    onCopy: (String) -> Unit
) {
    if (savedTags.isEmpty()) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Default.Save, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(44.dp))
                Spacer(modifier = Modifier.height(8.dp))
                Text("No Saved Tags in Vault", fontWeight = FontWeight.SemiBold)
                Text("Read an NFC tag and tap 'Save to Vault' to archive its UID and payloads.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
            }
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            savedTags.forEach { tag ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(tag.userLabel, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                            IconButton(onClick = { onDelete(tag.tagIdHex) }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = SecurityRed, modifier = Modifier.size(18.dp))
                            }
                        }
                        Text("UID: ${tag.tagIdHex}", style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace), color = MaterialTheme.colorScheme.primary)
                        tag.records.firstOrNull()?.let {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(it.payloadDisplay, style = MaterialTheme.typography.bodyMedium, maxLines = 2)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NfcTagRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
        Text(text = value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
    }
}
