package com.omnishield.utility.presentation.metadata

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VisibilityOff
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omnishield.utility.core.storage.StorageUtils
import com.omnishield.utility.domain.model.ExifMetadata
import com.omnishield.utility.domain.model.FileHashReport
import com.omnishield.utility.domain.model.SanitizerResult
import com.omnishield.utility.presentation.components.ErrorBanner
import com.omnishield.utility.presentation.components.OmniShieldAppBar
import com.omnishield.utility.presentation.components.OperationSuccessCard
import com.omnishield.utility.presentation.components.ProgressBarWithPercent
import com.omnishield.utility.presentation.theme.SecurityAmber
import com.omnishield.utility.presentation.theme.SecurityCyan
import com.omnishield.utility.presentation.theme.SecurityGreen
import com.omnishield.utility.presentation.theme.SecurityRed

@Composable
fun MetadataStripperScreen(
    viewModel: MetadataViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { viewModel.onImageSelected(context, it) }
    }

    val hashPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { viewModel.onHashFileSelected(context, it) }
    }

    Scaffold(
        topBar = {
            OmniShieldAppBar(
                title = "Metadata & Hashes",
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
                    selected = uiState.currentTab == MetadataTab.EXIF_SANITIZER,
                    onClick = { viewModel.setTab(MetadataTab.EXIF_SANITIZER) },
                    text = { Text("EXIF & Sanitizer") },
                    icon = { Icon(Icons.Default.VisibilityOff, contentDescription = null) }
                )
                Tab(
                    selected = uiState.currentTab == MetadataTab.HASH_VERIFIER,
                    onClick = { viewModel.setTab(MetadataTab.HASH_VERIFIER) },
                    text = { Text("Checksums & Hashes") },
                    icon = { Icon(Icons.Default.QrCode, contentDescription = null) }
                )
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when (uiState.currentTab) {
                    MetadataTab.EXIF_SANITIZER -> {
                        item {
                            ExifSection(
                                uiState = uiState,
                                onPickImage = {
                                    imagePickerLauncher.launch(arrayOf("image/*", "application/pdf"))
                                },
                                onSanitize = { viewModel.sanitizeSelectedFile() },
                                onShare = { uri ->
                                    StorageUtils.shareFile(context, uri, StorageUtils.getMimeType(context, uri))
                                }
                            )
                        }
                    }
                    MetadataTab.HASH_VERIFIER -> {
                        item {
                            HashSection(
                                uiState = uiState,
                                onPickFile = {
                                    hashPickerLauncher.launch(arrayOf("*/*"))
                                },
                                onVerificationInputChanged = { viewModel.onVerificationHashChanged(it) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExifSection(
    uiState: MetadataUiState,
    onPickImage: () -> Unit,
    onSanitize: () -> Unit,
    onShare: (Uri) -> Unit
) {
    val context = LocalContext.current
    var showRawTags by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Picker Header Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "EXIF Inspector & Privacy Sanitizer",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Detect and eliminate hidden GPS location, camera serials, timestamps, and device tags.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = onPickImage,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.PhotoCamera, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (uiState.selectedImageName.isEmpty()) "Select Image or Document" else "Change File")
                }
            }
        }

        if (uiState.isLoadingExif) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        uiState.exifMetadata?.let { exif ->
            // Camera & Device Info
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(text = "Device & Capture Details", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    MetadataRow(label = "Camera Make", value = exif.cameraMake ?: "Not specified")
                    MetadataRow(label = "Camera Model", value = exif.cameraModel ?: "Not specified")
                    MetadataRow(label = "Software", value = exif.software ?: "Not specified")
                    MetadataRow(label = "Timestamp", value = exif.dateTimeOriginal ?: "Not recorded")
                    if (exif.imageWidth != null && exif.imageHeight != null) {
                        MetadataRow(label = "Dimensions", value = "${exif.imageWidth} × ${exif.imageHeight} px")
                    }
                    MetadataRow(label = "Orientation", value = exif.orientation ?: "Normal")
                }
            }

            // Exposure & Sensor
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(text = "Exposure & Optics", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    MetadataRow(label = "ISO Sensitivity", value = exif.iso?.let { "ISO $it" } ?: "—")
                    MetadataRow(label = "Shutter Speed", value = exif.exposureTime ?: "—")
                    MetadataRow(label = "Aperture", value = exif.fNumber ?: "—")
                    MetadataRow(label = "Focal Length", value = exif.focalLength ?: "—")
                    MetadataRow(label = "Flash", value = exif.flash ?: "—")
                    MetadataRow(label = "White Balance", value = exif.whiteBalance ?: "—")
                }
            }

            // GPS Geolocation Card
            if (exif.gpsData != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = SecurityRed.copy(alpha = 0.1f))
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocationOn, contentDescription = null, tint = SecurityRed)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Embedded GPS Location (Privacy Leak)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = SecurityRed
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = exif.gpsData.formattedCoordinates,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        exif.gpsData.altitudeMeters?.let {
                            Text(
                                text = "Altitude: ${it.toInt()} meters",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(exif.gpsData.mapsDeepLink))
                                context.startActivity(intent)
                            }
                        ) {
                            Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("View on Maps")
                        }
                    }
                }
            }

            // Raw tags expandable
            if (exif.rawTags.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showRawTags = !showRawTags },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Raw Tags (${exif.rawTags.size})",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (showRawTags) "Hide" else "Show All",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        AnimatedVisibility(visible = showRawTags) {
                            Column(modifier = Modifier.padding(top = 10.dp)) {
                                exif.rawTags.forEach { (tag, value) ->
                                    MetadataRow(label = tag, value = value)
                                }
                            }
                        }
                    }
                }
            }

            // Sanitize Action Button
            Button(
                onClick = onSanitize,
                enabled = !uiState.isSanitizing,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = SecurityRed)
            ) {
                Icon(Icons.Default.DeleteSweep, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (uiState.isSanitizing) "Sanitizing..." else "Sanitize & Strip All Metadata")
            }
        }

        // Sanitizer Results
        when (val res = uiState.sanitizeResult) {
            is SanitizerResult.Success -> {
                OperationSuccessCard(
                    title = "File Sanitized Successfully!",
                    description = "Stripped ${res.strippedTagsCount} metadata tags.\nClean file: ${res.sanitizedFileName}\nSize: ${StorageUtils.formatBytes(res.sanitizedSizeBytes)}",
                    onShare = { onShare(res.outputUri) }
                )
            }
            is SanitizerResult.Failure -> {
                ErrorBanner(errorMessage = res.errorMessage)
            }
            null -> {}
        }

        uiState.exifError?.let {
            ErrorBanner(errorMessage = it)
        }
    }
}

@Composable
private fun HashSection(
    uiState: MetadataUiState,
    onPickFile: () -> Unit,
    onVerificationInputChanged: (String) -> Unit
) {
    val context = LocalContext.current

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "Cryptographic Checksum & Hash Verifier",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Generate and compare MD5, SHA-1, and SHA-256 hashes to detect tampering or corruption.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = onPickFile,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.FolderOpen, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (uiState.selectedHashFileName.isEmpty()) "Select Any File for Hashing" else "Change File")
                }
            }
        }

        if (uiState.isComputingHash) {
            ProgressBarWithPercent(
                progress = uiState.hashProgress,
                statusText = "Computing MD5, SHA-1, SHA-256 streams..."
            )
        }

        uiState.hashReport?.let { report ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = report.fileName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${StorageUtils.formatBytes(report.fileSizeBytes)} • Computed in ${report.durationMs}ms",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    HashItem(context, label = "SHA-256", hash = report.sha256)
                    Spacer(modifier = Modifier.height(8.dp))
                    HashItem(context, label = "SHA-1", hash = report.sha1)
                    Spacer(modifier = Modifier.height(8.dp))
                    HashItem(context, label = "MD5", hash = report.md5)
                }
            }

            // Hash Verification Tool
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Verify Checksum Match",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Paste expected hash to verify file authenticity:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = uiState.verificationHashInput,
                        onValueChange = onVerificationInputChanged,
                        label = { Text("Paste MD5, SHA-1, or SHA-256") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    when (uiState.hashMatchStatus) {
                        true -> {
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(SecurityGreen.copy(alpha = 0.15f))
                                    .padding(10.dp)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = SecurityGreen)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Checksum Verified: Perfect Match!",
                                    color = SecurityGreen,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        false -> {
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(SecurityRed.copy(alpha = 0.15f))
                                    .padding(10.dp)
                            ) {
                                Text(
                                    text = "Hash Mismatch: Checksum does not match.",
                                    color = SecurityRed,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        null -> {}
                    }
                }
            }
        }
    }
}

@Composable
private fun HashItem(context: Context, label: String, hash: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            .padding(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            IconButton(
                onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText(label, hash))
                    Toast.makeText(context, "$label copied to clipboard", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.size(24.dp)
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = "Copy Hash", modifier = Modifier.size(16.dp))
            }
        }
        Text(
            text = hash,
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun MetadataRow(label: String, value: String) {
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
