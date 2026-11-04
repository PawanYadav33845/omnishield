package com.omnishield.utility.presentation.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.EnhancedEncryption
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lan
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.SettingsRemote
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Transform
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omnishield.utility.presentation.components.OmniShieldAppBar
import com.omnishield.utility.presentation.components.SecurityGuaranteeBanner
import com.omnishield.utility.presentation.navigation.Screen
import com.omnishield.utility.presentation.theme.SecurityAmber
import com.omnishield.utility.presentation.theme.SecurityCyan
import com.omnishield.utility.presentation.theme.SecurityGreen
import com.omnishield.utility.presentation.theme.SecurityPurple
import com.omnishield.utility.presentation.theme.SecurityRed

enum class DashboardCategory(val label: String, val count: Int) {
    ALL("All Tools", 13),
    SECURITY("Security", 5),
    DOCUMENTS("Documents", 3),
    HARDWARE("Hardware & Net", 5)
}

@Composable
fun MainDashboardScreen(
    viewModel: DashboardViewModel,
    onNavigateTo: (String) -> Unit
) {
    val state by viewModel.state.collectAsState()
    var selectedCategory by remember { mutableStateOf(DashboardCategory.ALL) }
    val haptic = LocalHapticFeedback.current

    Scaffold(
        topBar = {
            OmniShieldAppBar(
                title = "OmniShield Suite"
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Modern Hero Card with Security Posture Pulse
            item {
                ModernCyberHeroCard()
            }

            // Quick Action Launcher Carousel
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "QUICK LAUNCH",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.outline,
                        letterSpacing = 1.2.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        QuickActionTile(
                            icon = Icons.Default.Lock,
                            label = "Lock File",
                            accent = SecurityGreen,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onNavigateTo(Screen.Crypto.route)
                            }
                        )
                        QuickActionTile(
                            icon = Icons.Default.VpnKey,
                            label = "Secret Vault",
                            accent = SecurityPurple,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onNavigateTo(Screen.NotesVault.route)
                            }
                        )
                        QuickActionTile(
                            icon = Icons.Default.Lan,
                            label = "LAN Audit",
                            accent = SecurityCyan,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onNavigateTo(Screen.NetworkStudio.route)
                            }
                        )
                        QuickActionTile(
                            icon = Icons.Default.Send,
                            label = "P2P Beam",
                            accent = SecurityCyan,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onNavigateTo(Screen.P2pTransfer.route)
                            }
                        )
                        QuickActionTile(
                            icon = Icons.Default.PictureAsPdf,
                            label = "Merge PDF",
                            accent = SecurityRed,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onNavigateTo(Screen.PdfStudio.route)
                            }
                        )
                        QuickActionTile(
                            icon = Icons.Default.Code,
                            label = "Inspect JWT",
                            accent = SecurityAmber,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onNavigateTo(Screen.TokenInspector.route)
                            }
                        )
                        QuickActionTile(
                            icon = Icons.Default.Nfc,
                            label = "Read NFC",
                            accent = SecurityCyan,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onNavigateTo(Screen.Nfc.route)
                            }
                        )
                        QuickActionTile(
                            icon = Icons.Default.DeleteForever,
                            label = "Shred Data",
                            accent = SecurityRed,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onNavigateTo(Screen.SecurityStudio.route)
                            }
                        )
                    }
                }
            }

            // Category Filter Pills
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DashboardCategory.entries.forEach { category ->
                        val isSelected = selectedCategory == category
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                selectedCategory = category
                            },
                            label = {
                                Text(
                                    "${category.label} (${category.count})",
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 12.sp
                                )
                            },
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(
                                width = 1.dp,
                                color = if (isSelected) SecurityCyan.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                            ),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SecurityCyan.copy(alpha = 0.15f),
                                selectedLabelColor = SecurityCyan,
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        )
                    }
                }
            }

            // ==========================================
            // CATEGORY 1: SECURITY & PRIVACY
            // ==========================================
            if (selectedCategory == DashboardCategory.ALL || selectedCategory == DashboardCategory.SECURITY) {
                item {
                    ModernSectionHeader("Security & Cryptography", "Military wiping, secret notes, AES-GCM & token analysis")
                }

                item {
                    ModernUtilityCard(
                        title = "Secret Notes & Duress Decoy Vault",
                        subtitle = "AES-256-GCM encrypted scratchpad with dual-PIN protection and emergency plausible decoy vault.",
                        badge = "DURESS PIN SYSTEM",
                        icon = Icons.Default.VpnKey,
                        accentColor = SecurityPurple,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onNavigateTo(Screen.NotesVault.route)
                        }
                    )
                }

                item {
                    ModernUtilityCard(
                        title = "AES-256-GCM Crypto Vault",
                        subtitle = "Hardware Keystore file locking with PBKDF2 & Fortified Password Generator.",
                        badge = "HARDWARE KEYSTORE",
                        icon = Icons.Default.EnhancedEncryption,
                        accentColor = SecurityGreen,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onNavigateTo(Screen.Crypto.route)
                        }
                    )
                }

                item {
                    ModernUtilityCard(
                        title = "Developer Token & X.509 Inspector",
                        subtitle = "Offline JWT claims decoder, expiration countdown & X.509 SSL certificate parser.",
                        badge = "TOKEN & CERT DECODER",
                        icon = Icons.Default.Code,
                        accentColor = SecurityAmber,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onNavigateTo(Screen.TokenInspector.route)
                        }
                    )
                }

                item {
                    ModernUtilityCard(
                        title = "DoD 5220.22-M File Shredder",
                        subtitle = "3-pass cryptographic wipe (0x00, 0xFF, CSPRNG noise), LSB steganography & app auditor.",
                        badge = "DOD SANITIZER",
                        icon = Icons.Default.DeleteForever,
                        accentColor = SecurityRed,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onNavigateTo(Screen.SecurityStudio.route)
                        }
                    )
                }

                item {
                    ModernUtilityCard(
                        title = "Metadata Inspector & Sanitizer",
                        subtitle = "Extract & strip camera EXIF, GPS optics, timestamps, and generate MD5/SHA checksums.",
                        badge = "PRIVACY SHIELD",
                        icon = Icons.Default.VisibilityOff,
                        accentColor = SecurityCyan,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onNavigateTo(Screen.Metadata.route)
                        }
                    )
                }
            }

            // ==========================================
            // CATEGORY 2: DOCUMENTS & FILES
            // ==========================================
            if (selectedCategory == DashboardCategory.ALL || selectedCategory == DashboardCategory.DOCUMENTS) {
                item {
                    ModernSectionHeader("Document & File Studio", "Offline PDF toolkit, binary inspector & converter")
                }

                item {
                    ModernUtilityCard(
                        title = "Offline PDF Studio",
                        subtitle = "Merge multi-file PDFs, split page ranges, and render high-res page preview images.",
                        badge = "NATIVE PDF",
                        icon = Icons.Default.PictureAsPdf,
                        accentColor = SecurityRed,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onNavigateTo(Screen.PdfStudio.route)
                        }
                    )
                }

                item {
                    ModernUtilityCard(
                        title = "Hex & Binary Inspector",
                        subtitle = "16-byte offset hex viewer with printable ASCII translation and magic header detector.",
                        badge = "LOW-LEVEL BYTE DUMP",
                        icon = Icons.Default.Terminal,
                        accentColor = SecurityCyan,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onNavigateTo(Screen.HexViewer.route)
                        }
                    )
                }

                item {
                    ModernUtilityCard(
                        title = "Universal File & Media Converter",
                        subtitle = "Lossless Level-9 ZIP compression, Target-size budget image reduction, and JSON/XML/YAML conversion.",
                        badge = "ADAPTIVE ENGINE",
                        icon = Icons.Default.Transform,
                        accentColor = SecurityPurple,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onNavigateTo(Screen.Converter.route)
                        }
                    )
                }
            }

            // ==========================================
            // CATEGORY 3: HARDWARE & NETWORK RADIOS
            // ==========================================
            if (selectedCategory == DashboardCategory.ALL || selectedCategory == DashboardCategory.HARDWARE) {
                item {
                    ModernSectionHeader("Hardware Peripherals & Network", "LAN audit, port scanner, P2P transfer, NFC & IR")
                }

                item {
                    ModernUtilityCard(
                        title = "Local Network & Wi-Fi Scanner (LAN Audit)",
                        subtitle = "Discovers active devices on local Wi-Fi subnet, open port checker, and network security diagnostics.",
                        badge = "LAN & SOCKET AUDIT",
                        icon = Icons.Default.Lan,
                        accentColor = SecurityCyan,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onNavigateTo(Screen.NetworkStudio.route)
                        }
                    )
                }

                item {
                    ModernUtilityCard(
                        title = "Zero-Cloud P2P AirBeam Transfer",
                        subtitle = "Beam gigabyte files directly between devices over local Wi-Fi or Hotspot with on-the-fly AES-256-GCM. 0% cloud, 100% offline.",
                        badge = "P2P AIRBEAM",
                        icon = Icons.Default.Send,
                        accentColor = SecurityCyan,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onNavigateTo(Screen.P2pTransfer.route)
                        }
                    )
                }

                item {
                    ModernUtilityCard(
                        title = "NFC Tag Toolkit & Vault",
                        subtitle = "Read tag UID, parse NDEF records, write text/URLs, and store cards in an offline vault.",
                        badge = "NFC CONTROLLER",
                        icon = Icons.Default.Nfc,
                        accentColor = SecurityGreen,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onNavigateTo(Screen.Nfc.route)
                        }
                    )
                }

                item {
                    ModernUtilityCard(
                        title = "Universal IR Remote Blaster",
                        subtitle = "Transmit TV, AC, and audio infrared signals with verified NEC protocols or custom Pronto Hex.",
                        badge = "CONSUMER IR",
                        icon = Icons.Default.SettingsRemote,
                        accentColor = SecurityAmber,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onNavigateTo(Screen.IrBlaster.route)
                        }
                    )
                }

                item {
                    ModernUtilityCard(
                        title = "Battery Health & Air-Gapped QR Vault",
                        subtitle = "Real-time battery diagnostics, thermals, voltage, and offline air-gapped QR generator.",
                        badge = "DIAGNOSTICS & QR",
                        icon = Icons.Default.BatteryChargingFull,
                        accentColor = SecurityPurple,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onNavigateTo(Screen.HardwareStudio.route)
                        }
                    )
                }
            }

            // Security Guarantee Banner
            item {
                SecurityGuaranteeBanner()
            }
        }
    }
}

@Composable
private fun ModernCyberHeroCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, SecurityCyan.copy(alpha = 0.25f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(
                            SecurityCyan.copy(alpha = 0.12f),
                            SecurityPurple.copy(alpha = 0.08f),
                            Color.Transparent
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(SecurityGreen)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "100% OFFLINE & AIR-GAPPED",
                                color = SecurityGreen,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "OmniShield Suite",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(SecurityCyan.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = SecurityCyan,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Capabilities row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FeaturePill(icon = Icons.Default.Bolt, label = "Zero Cloud")
                    FeaturePill(icon = Icons.Default.Fingerprint, label = "Duress Decoy")
                    FeaturePill(icon = Icons.Default.Lan, label = "LAN & Sockets")
                }
            }
        }
    }
}

@Composable
private fun FeaturePill(icon: ImageVector, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(13.dp), tint = SecurityCyan)
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = label, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun QuickActionTile(
    icon: ImageVector,
    label: String,
    accent: Color,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.width(108.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.2f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = accent, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun ModernSectionHeader(title: String, subtitle: String) {
    Column(modifier = Modifier.padding(top = 10.dp, bottom = 2.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline
        )
    }
}

@Composable
private fun ModernUtilityCard(
    title: String,
    subtitle: String,
    badge: String,
    icon: ImageVector,
    accentColor: Color,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.18f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(accentColor.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(26.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                    maxLines = 2
                )
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(accentColor.copy(alpha = 0.12f))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = badge,
                        color = accentColor,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
