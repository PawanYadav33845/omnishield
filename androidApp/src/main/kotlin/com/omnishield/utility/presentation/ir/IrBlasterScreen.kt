package com.omnishield.utility.presentation.ir

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeviceHub
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.SettingsRemote
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omnishield.utility.domain.model.IrCommand
import com.omnishield.utility.domain.model.IrDeviceCategory
import com.omnishield.utility.domain.model.IrDeviceProfile
import com.omnishield.utility.presentation.components.OmniShieldAppBar
import com.omnishield.utility.presentation.theme.SecurityAmber
import com.omnishield.utility.presentation.theme.SecurityCyan
import com.omnishield.utility.presentation.theme.SecurityGreen
import com.omnishield.utility.presentation.theme.SecurityRed

@Composable
fun IrBlasterScreen(
    viewModel: IrViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.init(context)
    }

    Scaffold(
        topBar = {
            OmniShieldAppBar(
                title = "IR Remote Blaster",
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
            // Hardware Status Card
            item {
                IrStatusBanner(
                    hasEmitter = uiState.hardwareStatus.hasEmitter,
                    lastFired = uiState.lastCommandTransmitted
                )
            }

            // Device Profile Selector
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    uiState.availableProfiles.forEachIndexed { index, profile ->
                        FilterChip(
                            selected = uiState.selectedProfileIndex == index,
                            onClick = { viewModel.selectProfile(index) },
                            label = { Text(profile.brandName) },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (profile.category == IrDeviceCategory.TV) Icons.Default.Tv else Icons.Default.SettingsRemote,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        )
                    }
                }
            }

            // Interactive Remote Console
            item {
                val currentProfile = uiState.availableProfiles.getOrNull(uiState.selectedProfileIndex)
                if (currentProfile != null) {
                    RemoteConsole(
                        profile = currentProfile,
                        onCommand = { cmd -> viewModel.transmitCommand(context, cmd) }
                    )
                }
            }

            // Custom Pronto Hex Raw Transmitter
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "Raw Pronto Hex Transmitter",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Paste standard Pronto Hex (e.g. from Flipper Zero or LIRC) to fire custom IR codes.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = uiState.customProntoInput,
                            onValueChange = { viewModel.onCustomProntoChanged(it) },
                            label = { Text("0000 006D 0022 0002 ...") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2,
                            textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = { viewModel.transmitCustomPronto(context) },
                            enabled = uiState.customProntoInput.isNotBlank(),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Transmit Raw Signal")
                        }

                        uiState.customTransmitStatus?.let {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun IrStatusBanner(
    hasEmitter: Boolean,
    lastFired: String?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (hasEmitter) SecurityGreen.copy(alpha = 0.12f) else SecurityAmber.copy(alpha = 0.15f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (hasEmitter) Icons.Default.SettingsRemote else Icons.Default.Warning,
                contentDescription = null,
                tint = if (hasEmitter) SecurityGreen else SecurityAmber,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (hasEmitter) "Consumer IR Blaster Ready" else "IR Emitter Not Detected (Simulation Mode)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (hasEmitter) "Carrier frequencies verified • Ready to pulse LEDs." else "Visualizing & generating timing patterns (device lacks hardware IR LED).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
                if (lastFired != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Last Signal: $lastFired",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun RemoteConsole(
    profile: IrDeviceProfile,
    onCommand: (IrCommand) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Power and Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                profile.commands.firstOrNull { it.id == "source" || it.id == "input" }?.let { cmd ->
                    RemoteSmallButton(text = "INPUT", onClick = { onCommand(cmd) })
                } ?: Spacer(modifier = Modifier.width(48.dp))

                // Red Glowing Power Button
                profile.commands.firstOrNull { it.isPrimary }?.let { pwrCmd ->
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(SecurityRed)
                            .clickable { onCommand(pwrCmd) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PowerSettingsNew,
                            contentDescription = "Power",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                profile.commands.firstOrNull { it.id == "mute" }?.let { muteCmd ->
                    RemoteSmallButton(text = "MUTE", onClick = { onCommand(muteCmd) })
                } ?: Spacer(modifier = Modifier.width(48.dp))
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Directional Pad (D-Pad)
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center
            ) {
                // Up
                profile.commands.firstOrNull { it.id == "dpad_up" || it.id == "temp_up" }?.let { cmd ->
                    IconButton(
                        onClick = { onCommand(cmd) },
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 8.dp)
                    ) {
                        Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Up", modifier = Modifier.size(32.dp))
                    }
                }
                // Down
                profile.commands.firstOrNull { it.id == "dpad_down" || it.id == "temp_down" }?.let { cmd ->
                    IconButton(
                        onClick = { onCommand(cmd) },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 8.dp)
                    ) {
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Down", modifier = Modifier.size(32.dp))
                    }
                }
                // Left
                profile.commands.firstOrNull { it.id == "dpad_left" }?.let { cmd ->
                    IconButton(
                        onClick = { onCommand(cmd) },
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(start = 8.dp)
                    ) {
                        Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Left", modifier = Modifier.size(32.dp))
                    }
                }
                // Right
                profile.commands.firstOrNull { it.id == "dpad_right" }?.let { cmd ->
                    IconButton(
                        onClick = { onCommand(cmd) },
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 8.dp)
                    ) {
                        Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Right", modifier = Modifier.size(32.dp))
                    }
                }
                // Center OK Button
                profile.commands.firstOrNull { it.id == "ok" || it.id == "mode_cool" }?.let { cmd ->
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                            .clickable { onCommand(cmd) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (cmd.id == "mode_cool") "COOL" else "OK",
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Volume & Channel Rockers Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                // Volume Column
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("VOL", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    RockingPill(
                        onPlus = {
                            profile.commands.firstOrNull { it.id == "vol_up" }?.let(onCommand)
                        },
                        onMinus = {
                            profile.commands.firstOrNull { it.id == "vol_down" }?.let(onCommand)
                        }
                    )
                }

                // Channel Column
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("CH", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    RockingPill(
                        onPlus = {
                            profile.commands.firstOrNull { it.id == "ch_up" }?.let(onCommand)
                        },
                        onMinus = {
                            profile.commands.firstOrNull { it.id == "ch_down" }?.let(onCommand)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun RockingPill(
    onPlus: () -> Unit,
    onMinus: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(58.dp)
            .height(110.dp)
            .clip(RoundedCornerShape(29.dp))
            .background(MaterialTheme.colorScheme.surface),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(onClick = onPlus, modifier = Modifier.size(50.dp)) {
            Icon(Icons.Default.Add, contentDescription = "Plus")
        }
        IconButton(onClick = onMinus, modifier = Modifier.size(50.dp)) {
            Icon(Icons.Default.Remove, contentDescription = "Minus")
        }
    }
}

@Composable
private fun RemoteSmallButton(
    text: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, fontSize = 9.sp, fontWeight = FontWeight.Bold)
    }
}
