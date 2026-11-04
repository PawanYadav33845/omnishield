package com.omnishield.utility.presentation.converter

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PhotoSizeSelectLarge
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Transform
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omnishield.utility.core.storage.StorageUtils
import com.omnishield.utility.domain.model.ConversionResult
import com.omnishield.utility.domain.model.DataFormat
import com.omnishield.utility.domain.model.ImageFormat
import com.omnishield.utility.presentation.components.ErrorBanner
import com.omnishield.utility.presentation.components.OmniShieldAppBar
import com.omnishield.utility.presentation.components.OperationSuccessCard
import com.omnishield.utility.presentation.components.ProgressBarWithPercent
import com.omnishield.utility.presentation.theme.SecurityGreen

@Composable
fun FileConverterScreen(
    viewModel: FileConverterViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    // ActivityResult Launchers
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.onImagesSelected(uris)
        }
    }

    val losslessPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.onLosslessFileSelected(context, it) }
    }

    val dataPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.onDataFileSelected(context, it) }
    }

    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.onAudioSelected(it) }
    }

    Scaffold(
        topBar = {
            OmniShieldAppBar(
                title = "File & Image Engine",
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
                    selected = uiState.currentTab == ConverterTab.IMAGES,
                    onClick = { viewModel.setTab(ConverterTab.IMAGES) },
                    text = { Text("Images") },
                    icon = { Icon(Icons.Default.Image, contentDescription = null) }
                )
                Tab(
                    selected = uiState.currentTab == ConverterTab.LOSSLESS_ZIP,
                    onClick = { viewModel.setTab(ConverterTab.LOSSLESS_ZIP) },
                    text = { Text("Lossless ZIP") },
                    icon = { Icon(Icons.Default.Archive, contentDescription = null) }
                )
                Tab(
                    selected = uiState.currentTab == ConverterTab.DATA_DOCS,
                    onClick = { viewModel.setTab(ConverterTab.DATA_DOCS) },
                    text = { Text("Data / Docs") },
                    icon = { Icon(Icons.Default.DataObject, contentDescription = null) }
                )
                Tab(
                    selected = uiState.currentTab == ConverterTab.AUDIO_MEDIA,
                    onClick = { viewModel.setTab(ConverterTab.AUDIO_MEDIA) },
                    text = { Text("Audio Info") },
                    icon = { Icon(Icons.Default.Audiotrack, contentDescription = null) }
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
                    ConverterTab.IMAGES -> {
                        item {
                            ImageConverterSection(
                                uiState = uiState,
                                onPickImages = {
                                    imagePickerLauncher.launch(arrayOf("image/*"))
                                },
                                onFormatSelected = { viewModel.setTargetImageFormat(it) },
                                onQualityChanged = { viewModel.setImageQuality(it) },
                                onTargetSizePreset = { viewModel.setTargetSizePreset(it) },
                                onDimensionScale = { viewModel.setDimensionScale(it) },
                                onConvert = { viewModel.startImageConversion() },
                                onClear = { viewModel.clearImages() },
                                onShareItem = { uri, mime ->
                                    StorageUtils.shareFile(context, uri, mime)
                                }
                            )
                        }
                    }
                    ConverterTab.LOSSLESS_ZIP -> {
                        item {
                            LosslessZipSection(
                                uiState = uiState,
                                onPickFile = {
                                    losslessPickerLauncher.launch(arrayOf("*/*"))
                                },
                                onCompress = { viewModel.startLosslessCompression() },
                                onShare = {
                                    uiState.compressedLosslessUri?.let { uri ->
                                        StorageUtils.shareFile(context, uri, "application/zip", "Share Compressed ZIP")
                                    }
                                }
                            )
                        }
                    }
                    ConverterTab.DATA_DOCS -> {
                        item {
                            DataConverterSection(
                                uiState = uiState,
                                onPickData = {
                                    dataPickerLauncher.launch(
                                        arrayOf("application/json", "application/xml", "text/xml", "text/csv", "application/x-yaml", "text/plain")
                                    )
                                },
                                onSourceFormatChange = { viewModel.setSourceDataFormat(it) },
                                onTargetFormatChange = { viewModel.setTargetDataFormat(it) },
                                onConvert = { viewModel.startDataConversion() },
                                onShare = {
                                    uiState.convertedDataUri?.let { uri ->
                                        StorageUtils.shareFile(context, uri, uiState.targetDataFormat.mimeType)
                                    }
                                }
                            )
                        }
                    }
                    ConverterTab.AUDIO_MEDIA -> {
                        item {
                            AudioInspectorSection(
                                uiState = uiState,
                                onPickAudio = {
                                    audioPickerLauncher.launch(arrayOf("audio/*", "video/mp4"))
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
private fun ImageConverterSection(
    uiState: ConverterUiState,
    onPickImages: () -> Unit,
    onFormatSelected: (ImageFormat) -> Unit,
    onQualityChanged: (Int) -> Unit,
    onTargetSizePreset: (Int?) -> Unit,
    onDimensionScale: (Int) -> Unit,
    onConvert: () -> Unit,
    onClear: () -> Unit,
    onShareItem: (Uri, String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                Text(
                    text = "Image Compression & Converter",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Compress by size budget, scale dimensions, or convert formats (PNG, JPEG, WebP, HEIC).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )

                Spacer(modifier = Modifier.height(14.dp))

                if (uiState.selectedImageUris.isEmpty()) {
                    Button(
                        onClick = onPickImages,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.FolderOpen, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Select Images (Single or Batch)")
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${uiState.selectedImageUris.size} images selected",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        OutlinedButton(onClick = onClear) {
                            Text("Clear / Reselect")
                        }
                    }
                }
            }
        }

        // Settings (visible if images selected)
        if (uiState.selectedImageUris.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "1. Target Format",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ImageFormat.entries.forEach { format ->
                            FilterChip(
                                selected = uiState.targetImageFormat == format,
                                onClick = { onFormatSelected(format) },
                                label = { Text(format.displayName) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "2. Dimension Downscaling",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(100 to "100% (Original)", 75 to "75%", 50 to "50% (Half)", 25 to "25% (Quarter)").forEach { (scale, label) ->
                            FilterChip(
                                selected = uiState.dimensionScalePercent == scale,
                                onClick = { onDimensionScale(scale) },
                                label = { Text(label) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "3. Target Size Budget",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(null to "Manual Quality", 100 to "< 100 KB", 250 to "< 250 KB", 500 to "< 500 KB", 1024 to "< 1 MB", 2048 to "< 2 MB").forEach { (kb, label) ->
                            FilterChip(
                                selected = uiState.targetSizePresetKb == kb,
                                onClick = { onTargetSizePreset(kb) },
                                label = { Text(label) }
                            )
                        }
                    }

                    if (uiState.targetSizePresetKb == null && uiState.targetImageFormat != ImageFormat.PNG && uiState.targetImageFormat != ImageFormat.WEBP_LOSSLESS) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Compression Quality: ${uiState.imageQuality}%",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Slider(
                            value = uiState.imageQuality.toFloat(),
                            onValueChange = { onQualityChanged(it.toInt()) },
                            valueRange = 10f..100f,
                            steps = 9
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onConvert,
                        enabled = !uiState.isConvertingImages,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Transform, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (uiState.isConvertingImages) "Processing..." else "Compress & Convert ${uiState.selectedImageUris.size} Images")
                    }
                }
            }
        }

        // Progress
        if (uiState.isConvertingImages) {
            ProgressBarWithPercent(
                progress = uiState.imageProgress.overallPercentage,
                statusText = "Processing ${uiState.imageProgress.currentItemName} (${uiState.imageProgress.currentItemIndex}/${uiState.imageProgress.totalItems})"
            )
        }

        // Results
        when (val res = uiState.imageConversionResult) {
            is ConversionResult.Success -> {
                OperationSuccessCard(
                    title = "Processing Complete!",
                    description = "Optimized ${res.convertedItems.size} files locally in ${res.durationMs}ms.",
                    onShare = {
                        res.convertedItems.firstOrNull()?.let {
                            onShareItem(it.outputUri, it.mimeType)
                        }
                    }
                )
                res.convertedItems.forEach { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = item.outputFileName, fontWeight = FontWeight.SemiBold)
                                Text(
                                    text = "${StorageUtils.formatBytes(item.originalSizeBytes)} ➔ ${StorageUtils.formatBytes(item.convertedSizeBytes)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                            Button(onClick = { onShareItem(item.outputUri, item.mimeType) }) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Share")
                            }
                        }
                    }
                }
            }
            is ConversionResult.Failure -> {
                ErrorBanner(errorMessage = res.errorMessage)
            }
            null -> {}
        }
    }
}

@Composable
private fun LosslessZipSection(
    uiState: ConverterUiState,
    onPickFile: () -> Unit,
    onCompress: () -> Unit,
    onShare: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                Text(
                    text = "Lossless File Compressor (DEFLATE ZIP)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Compress documents, logs, JSON, or code losslessly using Level-9 DEFLATE without losing a single byte.",
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
                    Text(if (uiState.selectedLosslessFileName.isEmpty()) "Select Any File to Compress" else "Change File")
                }
            }
        }

        if (uiState.selectedLosslessUri != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "File: ${uiState.selectedLosslessFileName}",
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = onCompress,
                        enabled = !uiState.isCompressingLossless,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Archive, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (uiState.isCompressingLossless) "Compressing..." else "Compress into .zip (Lossless)")
                    }
                }
            }
        }

        if (uiState.compressedLosslessUri != null) {
            OperationSuccessCard(
                title = "Lossless Compression Finished!",
                description = "Successfully packaged file into compressed ZIP archive.",
                onShare = onShare
            )
        }

        uiState.losslessError?.let {
            ErrorBanner(errorMessage = it)
        }
    }
}

@Composable
private fun DataConverterSection(
    uiState: ConverterUiState,
    onPickData: () -> Unit,
    onSourceFormatChange: (DataFormat) -> Unit,
    onTargetFormatChange: (DataFormat) -> Unit,
    onConvert: () -> Unit,
    onShare: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                Text(
                    text = "Structured Data Converter",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Convert between JSON, XML, YAML, and CSV formats on-device.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = onPickData,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.FolderOpen, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (uiState.selectedDataFileName.isEmpty()) "Select Data File" else "Change File")
                }
            }
        }

        if (uiState.selectedDataUri != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "File: ${uiState.selectedDataFileName}",
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Source Format:", style = MaterialTheme.typography.bodySmall)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        DataFormat.entries.forEach { format ->
                            FilterChip(
                                selected = uiState.sourceDataFormat == format,
                                onClick = { onSourceFormatChange(format) },
                                label = { Text(format.name) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Target Format:", style = MaterialTheme.typography.bodySmall)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        DataFormat.entries.forEach { format ->
                            FilterChip(
                                selected = uiState.targetDataFormat == format,
                                onClick = { onTargetFormatChange(format) },
                                label = { Text(format.name) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onConvert,
                        enabled = !uiState.isConvertingData,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (uiState.isConvertingData) "Converting..." else "Convert Data Format")
                    }
                }
            }
        }

        if (uiState.convertedDataUri != null) {
            OperationSuccessCard(
                title = "Data Transformed!",
                description = "Successfully converted to ${uiState.targetDataFormat.displayName}.",
                onShare = onShare
            )
        }

        uiState.dataError?.let {
            ErrorBanner(errorMessage = it)
        }
    }
}

@Composable
private fun AudioInspectorSection(
    uiState: ConverterUiState,
    onPickAudio: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                Text(
                    text = "Audio Container Inspector",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Inspect container stream bitrate, duration, sample rate & ID3 tags.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = onPickAudio,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.FolderOpen, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Select Media / Audio File")
                }
            }
        }

        uiState.audioMetadata?.let { info ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = info.fileName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    AudioRow(label = "Format / MIME", value = info.mimeType)
                    AudioRow(label = "Duration", value = "${info.formattedDuration} (${info.durationMs} ms)")
                    AudioRow(label = "Bitrate", value = "${info.bitrateKbps} kbps")
                    AudioRow(label = "Sample Rate", value = "${info.sampleRateHz} Hz")
                    AudioRow(label = "Channels", value = if (info.channels == 2) "Stereo (2)" else "${info.channels} ch")
                    info.title?.let { AudioRow(label = "Title", value = it) }
                    info.artist?.let { AudioRow(label = "Artist", value = it) }
                    info.album?.let { AudioRow(label = "Album", value = it) }
                    AudioRow(label = "Container Size", value = StorageUtils.formatBytes(info.sizeBytes))
                }
            }
        }

        uiState.audioError?.let {
            ErrorBanner(errorMessage = it)
        }
    }
}

@Composable
private fun AudioRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
        Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}
