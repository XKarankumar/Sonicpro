package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.BluetoothConnected
import androidx.compose.material.icons.rounded.BluetoothSearching
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.model.BluetoothConnectionState
import com.example.domain.model.EarbudSide
import com.example.domain.model.EarbudsDevice
import com.example.domain.model.TouchAction
import com.example.domain.model.TouchTrigger
import com.example.ui.components.BluetoothRadarPulse
import com.example.ui.components.PermissionRationaleBanner
import com.example.ui.components.SoniqGlassCard
import com.example.ui.components.UnsupportedBadge
import com.example.ui.theme.DemoAmber
import com.example.ui.theme.LiveEmerald
import com.example.ui.viewmodel.SoniqUiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DevicesScreen(
    state: SoniqUiState,
    onRequestBluetoothPermissions: () -> Unit,
    onRequestLocationPermission: () -> Unit,
    onStartScan: () -> Unit,
    onStopScan: () -> Unit,
    onPairDevice: (String) -> Unit,
    onConnectDevice: (String) -> Unit,
    onDisconnectDevice: (String) -> Unit,
    onEnableDemoPreview: () -> Unit,
    onDisableDemoPreview: () -> Unit,
    onUpdateGesture: (EarbudSide, TouchTrigger, TouchAction) -> Unit,
    onToggleDualDevice: () -> Unit,
    onToggleCallNoiseReduction: () -> Unit,
    onToggleMicSidetone: () -> Unit,
    onToggleFindChime: (EarbudSide?) -> Unit,
    onCaptureLastKnownLocation: () -> Unit,
    onOpenBluetoothSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedGestureSide by remember { mutableStateOf(EarbudSide.LEFT) }
    var pendingFindSide by remember { mutableStateOf<EarbudSide?>(null) }
    var showFindSafetyDialog by remember { mutableStateOf(false) }

    val bt = state.bluetoothSnapshot
    val active = state.activeDevice

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 680.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Devices & Controls", style = MaterialTheme.typography.headlineMedium)
                        Text(
                            text = bt.lastStatusMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    TextButton(onClick = onOpenBluetoothSettings) {
                        Text("OS Settings")
                    }
                }
            }

            if (!bt.permissionState.hasAllRequiredBluetoothPermissions) {
                item {
                    PermissionRationaleBanner(
                        title = stringResource(R.string.permission_bt_title),
                        rationale = stringResource(R.string.permission_bt_rationale),
                        buttonText = "Allow Bluetooth Discovery",
                        onGrantClick = onRequestBluetoothPermissions,
                        testTag = "devices_grant_bt_button"
                    )
                }
            }

            // Scanner & Discovery Card
            item {
                SoniqGlassCard(highlightCrimson = bt.isScanning) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BluetoothRadarPulse(isScanning = bt.isScanning)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Bluetooth Discovery", style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = when {
                                    !bt.isBluetoothSupported -> "No Bluetooth radio on this device"
                                    !bt.isBluetoothEnabled -> "Turn on Bluetooth to scan nearby earbuds"
                                    bt.isScanning -> "Scanning BLE & Classic audio (auto-stops in 12s)"
                                    else -> "Power-efficient BLE & Classic scanner ready"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { if (bt.isScanning) onStopScan() else onStartScan() },
                            enabled = bt.permissionState.hasAllRequiredBluetoothPermissions && bt.isBluetoothEnabled,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("scan_toggle_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(
                                imageVector = if (bt.isScanning) Icons.Rounded.Stop else Icons.Rounded.BluetoothSearching,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (bt.isScanning) "Stop Scan" else "Scan Nearby")
                        }
                        OutlinedButton(
                            onClick = { if (state.isDemoPreviewActive) onDisableDemoPreview() else onEnableDemoPreview() },
                            modifier = Modifier.testTag("devices_demo_toggle_button"),
                            border = BorderStroke(1.dp, DemoAmber.copy(alpha = 0.65f))
                        ) {
                            Icon(Icons.Rounded.Science, contentDescription = null, tint = DemoAmber, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (state.isDemoPreviewActive) "Exit Demo" else "Demo Buds", color = DemoAmber)
                        }
                    }

                    if (bt.discoveredDevices.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "DISCOVERED NEARBY (${bt.discoveredDevices.size})",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            bt.discoveredDevices.take(10).forEach { dev ->
                                DeviceRowItem(
                                    device = dev,
                                    actionLabel = if (dev.isBonded) "Connect" else "Pair",
                                    onAction = {
                                        if (dev.isBonded) onConnectDevice(dev.address)
                                        else onPairDevice(dev.address)
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Paired / Bonded Devices Card
            item {
                SoniqGlassCard {
                    Text(
                        text = "Paired Bluetooth Devices (${bt.bondedAudioDevices.size})",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    if (bt.bondedAudioDevices.isEmpty()) {
                        Text(
                            text = "No paired Bluetooth devices reported by Android yet. Pair your earbuds via Scan Nearby or Android Bluetooth settings.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            bt.bondedAudioDevices.forEach { dev ->
                                val isConnected = dev.connectionState == BluetoothConnectionState.CONNECTED
                                DeviceRowItem(
                                    device = dev,
                                    actionLabel = if (isConnected) "Disconnect" else "Connect",
                                    onAction = {
                                        if (isConnected) onDisconnectDevice(dev.address)
                                        else onConnectDevice(dev.address)
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Touch Gesture Configuration
            item {
                val gestureSupported = active?.capabilities?.supportsCustomTouchGestures == true
                SoniqGlassCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.TouchApp, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Touch Gesture Mapping", style = MaterialTheme.typography.titleMedium)
                        }
                        if (!gestureSupported) {
                            UnsupportedBadge("Unsupported")
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        EarbudSide.entries.forEach { side ->
                            FilterChip(
                                selected = selectedGestureSide == side,
                                onClick = { selectedGestureSide = side },
                                label = { Text(side.label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.alpha(if (gestureSupported) 1f else 0.5f)
                    ) {
                        state.gestureMappings
                            .filter { it.side == selectedGestureSide }
                            .forEach { mapping ->
                                var expanded by remember { mutableStateOf(false) }
                                Box {
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(14.dp))
                                            .clickable(enabled = gestureSupported) { expanded = true },
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                        shape = RoundedCornerShape(14.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(mapping.trigger.label, style = MaterialTheme.typography.titleSmall)
                                            Text(
                                                text = if (gestureSupported) mapping.action.label else "Hardware Locked",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                    DropdownMenu(
                                        expanded = expanded,
                                        onDismissRequest = { expanded = false }
                                    ) {
                                        TouchAction.entries.forEach { action ->
                                            DropdownMenuItem(
                                                text = { Text(action.label) },
                                                onClick = {
                                                    onUpdateGesture(selectedGestureSide, mapping.trigger, action)
                                                    expanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                    }
                }
            }

            // Call & Microphone + Dual Device Settings
            item {
                val micSupported = active?.capabilities?.supportsCallMicControls == true
                val dualSupported = active?.capabilities?.supportsDualDeviceMultipoint == true
                SoniqGlassCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Mic, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Call, Mic & Multipoint", style = MaterialTheme.typography.titleMedium)
                        }
                        if (!micSupported && !dualSupported) UnsupportedBadge("Unsupported")
                    }
                    Spacer(modifier = Modifier.height(10.dp))

                    FeatureToggleRow(
                        title = "Call Environmental Noise Reduction",
                        subtitle = "Suppresses wind & background noise on voice calls",
                        checked = active?.callNoiseReductionEnabled == true,
                        enabled = micSupported,
                        onCheckedChange = { onToggleCallNoiseReduction() }
                    )
                    FeatureToggleRow(
                        title = "Voice Sidetone (Self-Voice)",
                        subtitle = "Hear your natural voice during calls",
                        checked = active?.micSidetoneEnabled == true,
                        enabled = micSupported,
                        onCheckedChange = { onToggleMicSidetone() }
                    )
                    FeatureToggleRow(
                        title = "Dual-Device Multipoint Standby",
                        subtitle = "Keep two host devices paired and ready to switch",
                        checked = active?.dualDeviceEnabled == true,
                        enabled = dualSupported,
                        onCheckedChange = { onToggleDualDevice() }
                    )
                }
            }

            // Find My Earbuds & Last Known Location
            item {
                SoniqGlassCard(highlightCrimson = state.isFindChimePlaying) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.AutoMirrored.Rounded.VolumeUp, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Find My Earbuds", style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = "Plays a stereo / isolated Left or Right locator chime over active audio output.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                            .clip(RoundedCornerShape(16.dp))
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_earbuds_duo),
                            contentDescription = "Left and Right Earbuds",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            null to "Both",
                            EarbudSide.LEFT to "Ring Left",
                            EarbudSide.RIGHT to "Ring Right"
                        ).forEach { (side, label) ->
                            val isPlayingThis = state.isFindChimePlaying && state.findChimeTarget == side
                            Button(
                                onClick = {
                                    if (isPlayingThis) {
                                        onToggleFindChime(side)
                                    } else {
                                        pendingFindSide = side
                                        showFindSafetyDialog = true
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("find_chime_${side?.name?.lowercase() ?: "both"}"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isPlayingThis) LiveEmerald else MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = if (isPlayingThis) Color.Black else MaterialTheme.colorScheme.onSurface
                                )
                            ) {
                                Text(
                                    text = if (isPlayingThis) "Stop" else label,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Rounded.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Last-Known Location", style = MaterialTheme.typography.titleSmall)
                                }
                                TextButton(
                                    onClick = {
                                        if (bt.permissionState.hasLocationPermission) onCaptureLastKnownLocation()
                                        else onRequestLocationPermission()
                                    },
                                    modifier = Modifier.testTag("record_location_button")
                                ) {
                                    Icon(Icons.Rounded.MyLocation, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(if (bt.permissionState.hasLocationPermission) "Pin Now" else "Allow Coarse")
                                }
                            }
                            val loc = state.preferences.lastKnownLocation
                            if (loc != null) {
                                val dateStr = remember(loc.recordedAtMs) {
                                    SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(Date(loc.recordedAtMs))
                                }
                                Text(
                                    text = "${loc.deviceName} • ${String.format("%.4f, %.4f", loc.latitude, loc.longitude)} (±${loc.accuracyMeters.toInt()}m) • $dateStr",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                Text(
                                    text = stringResource(R.string.permission_location_rationale),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }
        }
    }

    if (showFindSafetyDialog) {
        AlertDialog(
            onDismissRequest = { showFindSafetyDialog = false },
            icon = { Icon(Icons.Rounded.WarningAmber, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Ear Safety Check") },
            text = {
                Text("Make sure your earbuds are NOT in your ears before playing the high-frequency locator chime.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showFindSafetyDialog = false
                        onToggleFindChime(pendingFindSide)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Play Chime")
                }
            },
            dismissButton = {
                TextButton(onClick = { showFindSafetyDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun DeviceRowItem(
    device: EarbudsDevice,
    actionLabel: String,
    onAction: () -> Unit
) {
    val isConnected = device.connectionState == BluetoothConnectionState.CONNECTED
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            1.dp,
            if (isConnected) LiveEmerald.copy(alpha = 0.55f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isConnected) Icons.Rounded.BluetoothConnected else Icons.Rounded.Bluetooth,
                contentDescription = null,
                tint = if (isConnected) LiveEmerald else MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = device.name,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = buildString {
                        append(device.brand.displayName)
                        device.rssiDbm?.let { append(" • $it dBm") }
                        device.battery.singlePercent?.let { append(" • $it%") }
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            OutlinedButton(onClick = onAction) {
                Text(actionLabel)
            }
        }
    }
}

@Composable
private fun FeatureToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .alpha(if (enabled) 1f else 0.5f),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(
            checked = checked,
            onCheckedChange = { onCheckedChange() },
            enabled = enabled
        )
    }
}
