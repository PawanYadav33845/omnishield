package com.omnishield.utility.presentation.crypto

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.omnishield.utility.core.storage.StorageUtils
import com.omnishield.utility.domain.model.CryptoMode
import com.omnishield.utility.domain.model.CryptoResult
import com.omnishield.utility.presentation.components.ErrorBanner
import com.omnishield.utility.presentation.components.FileSelectedCard
import com.omnishield.utility.presentation.components.OmniShieldAppBar
import com.omnishield.utility.presentation.components.OperationSuccessCard
import com.omnishield.utility.presentation.components.ProgressBarWithPercent
import com.omnishield.utility.presentation.theme.SecurityAmber
import com.omnishield.utility.presentation.theme.SecurityGreen
import com.omnishield.utility.presentation.theme.SecurityRed

@Composable
fun EncryptionScreen(
    viewModel: CryptoViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    // File picker launcher
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { viewModel.onFileSelected(context, it) }
    }

    // Save To Storage Launcher
    val saveFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { destUri: Uri? ->
        if (destUri != null) {
            viewModel.startCryptoOperation(destUri)
        }
    }

    Scaffold(
        topBar = {
            OmniShieldAppBar(
                title = "AES-256-GCM Vault",
                canNavigateBack = true,
                onNavigateBack = onNavigateBack
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Mode Selector Tabs (Lock / Unlock)
            item {
                TabRow(
                    selectedTabIndex = uiState.mode.ordinal,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.clip(RoundedCornerShape(14.dp))
                ) {
                    Tab(
                        selected = uiState.mode == CryptoMode.ENCRYPT,
                        onClick = { viewModel.setMode(CryptoMode.ENCRYPT) },
                        text = { Text("Lock File (Encrypt)") },
                        icon = { Icon(Icons.Default.Lock, contentDescription = null) }
                    )
                    Tab(
                        selected = uiState.mode == CryptoMode.DECRYPT,
                        onClick = { viewModel.setMode(CryptoMode.DECRYPT) },
                        text = { Text("Unlock File (Decrypt)") },
                        icon = { Icon(Icons.Default.LockOpen, contentDescription = null) }
                    )
                }
            }

            // File Selection Area
            item {
                if (uiState.selectedFileUri == null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = if (uiState.mode == CryptoMode.ENCRYPT) Icons.Default.Lock else Icons.Default.LockOpen,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = if (uiState.mode == CryptoMode.ENCRYPT) "Select Any File to Protect" else "Select Encrypted .shield File",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Supported: Any document, image, video, audio, archive or raw binary",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { filePickerLauncher.launch(arrayOf("*/*")) },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.FolderOpen, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Browse Storage")
                            }
                        }
                    }
                } else {
                    FileSelectedCard(
                        fileName = uiState.selectedFileName,
                        fileSizeFormatted = uiState.selectedFileSizeFormatted,
                        mimeType = StorageUtils.getMimeType(context, uiState.selectedFileUri!!),
                        onClear = { viewModel.clear() }
                    )
                }
            }

            // Cryptographic Key / Passphrase Input Form
            if (uiState.selectedFileUri != null) {
                item {
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
                                Text(
                                    text = if (uiState.mode == CryptoMode.ENCRYPT) "Set Encryption Passphrase" else "Enter Decryption Passphrase",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                if (uiState.mode == CryptoMode.ENCRYPT) {
                                    OutlinedButton(
                                        onClick = { viewModel.generateAndSetStrongPassword() },
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                                    ) {
                                        Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Generate Strong", fontSize = 11.sp)
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedTextField(
                                value = uiState.passphrase,
                                onValueChange = { viewModel.onPassphraseChanged(it) },
                                label = { Text("Passphrase") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                visualTransformation = if (uiState.isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                trailingIcon = {
                                    IconButton(onClick = { viewModel.togglePasswordVisibility() }) {
                                        Icon(
                                            imageVector = if (uiState.isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = "Toggle Passphrase Visibility"
                                        )
                                    }
                                }
                            )

                            // Password Strength Bar (Encrypt mode only)
                            if (uiState.mode == CryptoMode.ENCRYPT && uiState.passphrase.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Strength: ${uiState.passwordStrengthLabel}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = when {
                                            uiState.passwordStrength < 0.4f -> SecurityRed
                                            uiState.passwordStrength < 0.7f -> SecurityAmber
                                            else -> SecurityGreen
                                        }
                                    )
                                    Text(
                                        text = "${(uiState.passwordStrength * 100).toInt()}%",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                LinearProgressIndicator(
                                    progress = { uiState.passwordStrength },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = when {
                                        uiState.passwordStrength < 0.4f -> SecurityRed
                                        uiState.passwordStrength < 0.7f -> SecurityAmber
                                        else -> SecurityGreen
                                    }
                                )
                            }

                            // Confirm Passphrase for Encrypt mode
                            if (uiState.mode == CryptoMode.ENCRYPT) {
                                Spacer(modifier = Modifier.height(12.dp))
                                OutlinedTextField(
                                    value = uiState.confirmPassphrase,
                                    onValueChange = { viewModel.onConfirmPassphraseChanged(it) },
                                    label = { Text("Confirm Passphrase") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    visualTransformation = if (uiState.isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                    isError = uiState.confirmPassphrase.isNotEmpty() && uiState.passphrase != uiState.confirmPassphrase
                                )
                            }

                            // Biometrics Option
                            if (BiometricAuthManager.canAuthenticate(context)) {
                                Spacer(modifier = Modifier.height(14.dp))
                                OutlinedButton(
                                    onClick = {
                                        (context as? FragmentActivity)?.let { activity ->
                                            BiometricAuthManager.promptBiometric(
                                                activity = activity,
                                                title = "OmniShield Vault Access",
                                                subtitle = "Authenticate to unlock cryptographic master key"
                                            ) { status ->
                                                viewModel.onBiometricSuccess()
                                            }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.Fingerprint, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Biometric Authentication")
                                }
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            // Start Processing Button
                            Button(
                                onClick = { viewModel.startCryptoOperation() },
                                enabled = !uiState.isProcessing && uiState.passphrase.isNotEmpty(),
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (uiState.mode == CryptoMode.ENCRYPT) MaterialTheme.colorScheme.primary else SecurityGreen
                                )
                            ) {
                                Icon(
                                    imageVector = if (uiState.mode == CryptoMode.ENCRYPT) Icons.Default.Lock else Icons.Default.LockOpen,
                                    contentDescription = null
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (uiState.isProcessing) "Streaming Cryptography..." else if (uiState.mode == CryptoMode.ENCRYPT) "Encrypt & Lock File" else "Decrypt & Unlock File"
                                )
                            }
                        }
                    }
                }
            }

            // Realtime Progress Indicator
            if (uiState.isProcessing) {
                item {
                    ProgressBarWithPercent(
                        progress = uiState.progress.percentage,
                        bytesProcessed = uiState.progress.bytesProcessed,
                        totalBytes = uiState.progress.totalBytes,
                        statusText = "Streaming AES-256-GCM cipher chunks..."
                    )
                }
            }

            // Results Display
            item {
                when (val res = uiState.result) {
                    is CryptoResult.Success -> {
                        OperationSuccessCard(
                            title = if (res.mode == CryptoMode.ENCRYPT) "File Locked Successfully!" else "File Unlocked Successfully!",
                            description = "Output: ${res.outputFileName}\nSize: ${StorageUtils.formatBytes(res.outputSizeBytes)} in ${res.durationMs}ms\nIntegrity: AEAD GCM 128-bit verified.",
                            onShare = {
                                StorageUtils.shareFile(
                                    context = context,
                                    fileUri = res.outputUri,
                                    mimeType = StorageUtils.getMimeType(context, res.outputUri),
                                    chooserTitle = "Share Protected File"
                                )
                            },
                            onSaveToLocation = {
                                saveFileLauncher.launch(res.outputFileName)
                            }
                        )
                    }
                    is CryptoResult.Failure -> {
                        ErrorBanner(errorMessage = res.errorMessage)
                    }
                    null -> {}
                }
            }
        }
    }
}
