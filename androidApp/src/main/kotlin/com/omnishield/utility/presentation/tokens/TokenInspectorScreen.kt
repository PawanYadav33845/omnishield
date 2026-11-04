package com.omnishield.utility.presentation.tokens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omnishield.utility.core.tokens.JwtParsedReport
import com.omnishield.utility.core.tokens.X509ParsedReport
import com.omnishield.utility.presentation.components.OmniShieldAppBar
import com.omnishield.utility.presentation.theme.SecurityAmber
import com.omnishield.utility.presentation.theme.SecurityCyan
import com.omnishield.utility.presentation.theme.SecurityGreen
import com.omnishield.utility.presentation.theme.SecurityPurple
import com.omnishield.utility.presentation.theme.SecurityRed

@Composable
fun TokenInspectorScreen(
    viewModel: TokenInspectorViewModel,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            OmniShieldAppBar(
                title = "Token & Certificate Inspector",
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
                    text = { Text("JWT Inspector", fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = state.activeTab == 1,
                    onClick = { viewModel.selectTab(1) },
                    text = { Text("X.509 Certificate", fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Default.Badge, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
            }

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

            when (state.activeTab) {
                0 -> JwtInspectorTabContent(
                    state = state,
                    onInputChange = { viewModel.setJwtInput(it) }
                )
                1 -> CertInspectorTabContent(
                    state = state,
                    onInputChange = { viewModel.setCertInput(it) }
                )
            }
        }
    }
}

@Composable
private fun JwtInspectorTabContent(
    state: TokenInspectorState,
    onInputChange: (String) -> Unit
) {
    val context = LocalContext.current

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
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Paste JSON Web Token", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        OutlinedButton(
                            onClick = {
                                val sample = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkFsaWNlIFNlY3VyaXR5IiwiaWF0IjoxNTE2MjM5MDIyLCJleHAiOjE5MTYyMzkwMjJ9.4zY5..."
                                onInputChange(sample)
                            },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Sample", fontSize = 11.sp)
                        }
                    }

                    OutlinedTextField(
                        value = state.jwtInputText,
                        onValueChange = onInputChange,
                        placeholder = { Text("Paste raw encoded JWT (header.payload.signature)...") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        minLines = 3,
                        maxLines = 6
                    )
                }
            }
        }

        if (state.jwtReport != null) {
            val report = state.jwtReport

            // Warnings
            if (report.securityWarnings.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = SecurityRed.copy(alpha = 0.12f))
                    ) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = SecurityRed, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                report.securityWarnings.forEach { warning ->
                                    Text(warning, color = SecurityRed, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }

            // Status Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Algorithm: ${report.algorithm}", fontWeight = FontWeight.Bold, color = SecurityCyan)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (report.isExpired) SecurityRed.copy(alpha = 0.15f) else SecurityGreen.copy(alpha = 0.15f))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = report.expirationRelativeStatus,
                                    color = if (report.isExpired) SecurityRed else SecurityGreen,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        if (report.expirationDateFormatted != null) {
                            Text("Expires: ${report.expirationDateFormatted}", style = MaterialTheme.typography.bodySmall)
                        }
                        if (report.subject != null) {
                            Text("Subject (sub): ${report.subject}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            // Header JSON
            item {
                JsonDisplayCard(title = "Decoded Header", jsonText = report.headerJson, context = context)
            }

            // Payload JSON
            item {
                JsonDisplayCard(title = "Decoded Payload Claims", jsonText = report.payloadJson, context = context)
            }
        }
    }
}

@Composable
private fun CertInspectorTabContent(
    state: TokenInspectorState,
    onInputChange: (String) -> Unit
) {
    val context = LocalContext.current

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
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Paste X.509 Certificate (PEM)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)

                    OutlinedTextField(
                        value = state.certInputText,
                        onValueChange = onInputChange,
                        placeholder = { Text("Paste -----BEGIN CERTIFICATE----- ... -----END CERTIFICATE-----") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        minLines = 3,
                        maxLines = 6
                    )
                }
            }
        }

        if (state.certReport != null) {
            val cert = state.certReport

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("X.509 Certificate", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (cert.isExpired) SecurityRed.copy(alpha = 0.15f) else SecurityGreen.copy(alpha = 0.15f))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (cert.isExpired) "EXPIRED" else "VALID",
                                    color = if (cert.isExpired) SecurityRed else SecurityGreen,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        CertField(label = "Subject", value = cert.subject)
                        CertField(label = "Issuer", value = cert.issuer)
                        CertField(label = "Serial Number", value = cert.serialNumberHex)
                        CertField(label = "Validity", value = "${cert.validFromFormatted}  to  ${cert.validToFormatted}")
                        CertField(label = "Public Key", value = cert.publicKeyAlgorithm)
                        CertField(label = "Signature Alg", value = cert.signatureAlgorithm)

                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Fingerprints (Hashes)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                        CertFingerprint(label = "SHA-256", hash = cert.sha256Fingerprint, context = context)
                        CertFingerprint(label = "SHA-1", hash = cert.sha1Fingerprint, context = context)
                    }
                }
            }
        }
    }
}

@Composable
private fun CertField(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 2.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun CertFingerprint(label: String, hash: String, context: Context) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
            Text(hash, fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = SecurityCyan)
        }
        IconButton(onClick = {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("$label Fingerprint", hash))
            Toast.makeText(context, "$label copied", Toast.LENGTH_SHORT).show()
        }) {
            Icon(Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun JsonDisplayCard(title: String, jsonText: String, context: Context) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF10161A))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, fontWeight = FontWeight.Bold, color = SecurityCyan, style = MaterialTheme.typography.titleSmall)
                IconButton(onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText(title, jsonText))
                    Toast.makeText(context, "$title copied to clipboard", Toast.LENGTH_SHORT).show()
                }) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = Color.White, modifier = Modifier.size(16.dp))
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = jsonText,
                color = Color(0xFF80CBC4),
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                lineHeight = 16.sp
            )
        }
    }
}
