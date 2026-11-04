package com.omnishield.utility.presentation.pdf

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CallSplit
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MergeType
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omnishield.utility.core.storage.StorageUtils
import com.omnishield.utility.presentation.components.OmniShieldAppBar
import com.omnishield.utility.presentation.theme.SecurityCyan
import com.omnishield.utility.presentation.theme.SecurityGreen
import com.omnishield.utility.presentation.theme.SecurityRed

@Composable
fun PdfStudioScreen(
    viewModel: PdfStudioViewModel,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    val mergePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) viewModel.addMergeFiles(uris)
    }

    val splitPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.setSplitFile(it) }
    }

    val renderPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.setRenderFile(it) }
    }

    Scaffold(
        topBar = {
            OmniShieldAppBar(
                title = "Offline PDF Studio",
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
                    text = { Text("Merge PDFs", fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Default.MergeType, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = state.activeTab == 1,
                    onClick = { viewModel.selectTab(1) },
                    text = { Text("Split / Extract", fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Default.CallSplit, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = state.activeTab == 2,
                    onClick = { viewModel.selectTab(2) },
                    text = { Text("Render Preview", fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
            }

            if (state.errorMessage != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
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
                        .padding(horizontal = 16.dp, vertical = 8.dp),
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
                0 -> MergeTabContent(
                    state = state,
                    onPickFiles = { mergePicker.launch(arrayOf("application/pdf")) },
                    onRemoveFile = { viewModel.removeMergeFile(it) },
                    onClearAll = { viewModel.clearMergeFiles() },
                    onMerge = { viewModel.executeMerge() },
                    onShareOutput = { file ->
                        val uri = StorageUtils.getShareableUri(context, file)
                        StorageUtils.shareFile(context, uri, "application/pdf", "Share Merged PDF")
                    }
                )
                1 -> SplitTabContent(
                    state = state,
                    onPickFile = { splitPicker.launch(arrayOf("application/pdf")) },
                    onRangeChange = { start, end -> viewModel.setSplitPages(start, end) },
                    onSplit = { viewModel.executeSplit() },
                    onShareOutput = { file ->
                        val uri = StorageUtils.getShareableUri(context, file)
                        StorageUtils.shareFile(context, uri, "application/pdf", "Share Split PDF")
                    }
                )
                2 -> RenderTabContent(
                    state = state,
                    onPickFile = { renderPicker.launch(arrayOf("application/pdf")) },
                    onRenderPage = { page -> viewModel.executeRender(page) }
                )
            }
        }
    }
}

@Composable
private fun MergeTabContent(
    state: PdfStudioState,
    onPickFiles: () -> Unit,
    onRemoveFile: (Int) -> Unit,
    onClearAll: () -> Unit,
    onMerge: () -> Unit,
    onShareOutput: (java.io.File) -> Unit
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
                        Text("PDF Merge Queue", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            "${state.mergeInputFiles.size} document(s) queued for combination",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                    Button(onClick = onPickFiles, shape = RoundedCornerShape(12.dp)) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add PDFs")
                    }
                }
            }
        }

        itemsIndexed(state.mergeInputFiles) { index, item ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "#${index + 1}",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(item.name, fontWeight = FontWeight.SemiBold, maxLines = 1)
                        if (item.sizeFormatted.isNotBlank()) {
                            Text(item.sizeFormatted, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                        }
                    }
                    IconButton(onClick = { onRemoveFile(index) }) {
                        Icon(Icons.Default.Close, contentDescription = "Remove", tint = SecurityRed, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }

        if (state.mergeInputFiles.isNotEmpty()) {
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = onClearAll,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Clear Queue")
                    }
                    Button(
                        onClick = onMerge,
                        modifier = Modifier.weight(2f),
                        enabled = !state.isLoading && state.mergeInputFiles.size >= 2,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (state.isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White)
                        } else {
                            Text("Merge PDFs (${state.mergeInputFiles.size})")
                        }
                    }
                }
            }
        }

        if (state.mergedOutputFile != null) {
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
                            Text("Merged PDF Ready", fontWeight = FontWeight.Bold, color = SecurityGreen)
                            Text(state.mergedOutputFile.name, style = MaterialTheme.typography.bodySmall)
                        }
                        Button(
                            onClick = { onShareOutput(state.mergedOutputFile) },
                            shape = RoundedCornerShape(12.dp)
                        ) {
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

@Composable
private fun SplitTabContent(
    state: PdfStudioState,
    onPickFile: () -> Unit,
    onRangeChange: (Int, Int) -> Unit,
    onSplit: () -> Unit,
    onShareOutput: (java.io.File) -> Unit
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
                    Text(state.splitInputFile?.name ?: "No PDF Selected", fontWeight = FontWeight.Bold, maxLines = 1)
                    Text(state.splitInputFile?.sizeFormatted ?: "Pick a PDF document to extract specific pages", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                }
                Button(onClick = onPickFile, shape = RoundedCornerShape(12.dp)) {
                    Icon(Icons.Default.FileOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Select PDF")
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = state.splitStartPage.toString(),
                onValueChange = { str ->
                    val p = str.toIntOrNull() ?: 1
                    onRangeChange(p, state.splitEndPage)
                },
                label = { Text("Start Page (1-based)") },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )
            OutlinedTextField(
                value = state.splitEndPage.toString(),
                onValueChange = { str ->
                    val p = str.toIntOrNull() ?: 1
                    onRangeChange(state.splitStartPage, p)
                },
                label = { Text("End Page (1-based)") },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )
        }

        Button(
            onClick = onSplit,
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.isLoading && state.splitInputFile != null,
            shape = RoundedCornerShape(14.dp)
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White)
            } else {
                Text("Extract & Save Pages")
            }
        }

        if (state.splitOutputFile != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SecurityCyan.copy(alpha = 0.12f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Extracted PDF Ready", fontWeight = FontWeight.Bold, color = SecurityCyan)
                        Text(state.splitOutputFile.name, style = MaterialTheme.typography.bodySmall)
                    }
                    Button(
                        onClick = { onShareOutput(state.splitOutputFile) },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share")
                    }
                }
            }
        }
    }
}

@Composable
private fun RenderTabContent(
    state: PdfStudioState,
    onPickFile: () -> Unit,
    onRenderPage: (Int) -> Unit
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
                    Text(state.renderInputFile?.name ?: "No PDF Selected", fontWeight = FontWeight.Bold, maxLines = 1)
                    Text("Render high-res image bitmap preview", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                }
                Button(onClick = onPickFile, shape = RoundedCornerShape(12.dp)) {
                    Icon(Icons.Default.FileOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Select PDF")
                }
            }
        }

        if (state.renderInputFile != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { if (state.renderPageIndex > 1) onRenderPage(state.renderPageIndex - 1) },
                    enabled = state.renderPageIndex > 1,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Prev Page")
                }

                Text(
                    text = "Page ${state.renderPageIndex} of ${state.totalPagesInRenderDoc.coerceAtLeast(1)}",
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                Button(
                    onClick = { onRenderPage(state.renderPageIndex + 1) },
                    enabled = state.renderPageIndex < state.totalPagesInRenderDoc,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Next Page")
                }
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                when {
                    state.isLoading -> CircularProgressIndicator()
                    state.renderedBitmap != null -> {
                        Image(
                            bitmap = state.renderedBitmap.asImageBitmap(),
                            contentDescription = "Rendered PDF Page",
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp)
                        )
                    }
                    else -> {
                        Text(
                            text = "Select a PDF to view rendered pages",
                            color = MaterialTheme.colorScheme.outline,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}
