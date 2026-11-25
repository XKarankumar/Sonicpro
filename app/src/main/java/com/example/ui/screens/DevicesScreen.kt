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
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.example.ui.components.BluetoothRadarPulse
import com.example.ui.components.FeatureMatrixList
import com.example.ui.components.PermissionRationaleBanner
import com.example.ui.components.SoniqGlassCard
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
    onSelectInspectedDevice: (String) -> Unit,
    onToggleFindChime: (EarbudSide?) -> Unit,
    onCaptureLastKnownLocation: () -> Unit,
    onOpenBluetoothSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    var pendingFindSide by remember { mutableStateOf<EarbudSide?>(null) }
    var showFindSafetyDialog by remember { mutableStateOf(false) }

    val bt = state.bluetoothSnapshot
    val inspected = state.inspectedDevice

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
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Devices & Telemetry", style = MaterialTheme.typography.headlineMedium)
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
                                    !bt.isBluetoothEnabled -> "Turn on Bluetooth to scan nearby earbuds & speakers"
                                    bt.isScanning -> "Scanning BLE & Classic audio (auto-stops in 10s)"
                                    else -> "BLE & Classic scanner ready for all audio brands"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { if (bt.isScanning) onStopScan() else onStartScan() },
                        enabled = bt.permissionState.hasAllRequiredBluetoothPermissions && bt.isBluetoothEnabled,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("scan_toggle_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(
                            imageVector = if (bt.isScanning) Icons.Rounded.Stop else Icons.Rounded.BluetoothSearching,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (bt.isScanning) "Stop Scan" else "Scan Nearby Audio Hardware")
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
                            bt.discoveredDevices.take(12).forEach { dev ->
                                DeviceRowItem(
                                    device = dev,
                                    isInspected = inspected?.address == dev.address,
                                    actionLabel = if (dev.isBonded) "Connect" else "Pair",
                                    onInspect = { onSelectInspectedDevice(dev.address) },
                                    onAction = {
                                        onSelectInspectedDevice(dev.address)
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
                            text = "No paired Bluetooth devices reported by Android yet. Tap Scan Nearby or pair in Android Settings.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            bt.bondedAudioDevices.forEach { dev ->
                                val isConnected = dev.connectionState == BluetoothConnectionState.CONNECTED
                                DeviceRowItem(
                                    device = dev,
                                    isInspected = inspected?.address == dev.address,
                                    actionLabel = if (isConnected) "Disconnect" else "Connect",
                                    onInspect = { onSelectInspectedDevice(dev.address) },
                                    onAction = {
                                        onSelectInspectedDevice(dev.address)
                                        if (isConnected) onDisconnectDevice(dev.address)
                                        else onConnectDevice(dev.address)
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Hardware Capability & Firmware Matrix Card
            item {
                SoniqGlassCard(highlightCrimson = inspected != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.VerifiedUser, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = inspected?.let { "${it.name} Capabilities" } ?: "OEM Hardware Feature Matrix",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = inspected?.let {
                                    "Integration: ${it.capabilities.integrationId} • ${it.brand.displayName}"
                                } ?: "Select or connect a Bluetooth device to inspect live hardware & protocol support.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (inspected != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "Model: ${inspected.modelNumber ?: "Not exposed via GATT"} • FW: ${inspected.firmwareVersion ?: "Unavailable"}",
                                    style = MaterialTheme.typography.labelMedium
                                )
                                Text(
                                    text = "Codec: ${inspected.codecInfo?.codecName ?: "Unavailable"} • Battery: ${inspected.battery.singlePercent?.let { "$it%" } ?: "Unavailable"}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        FeatureMatrixList(items = inspected.capabilities.featureMatrix)
                    }
                }
            }

            // Find My Device & Last Known Location
            item {
                SoniqGlassCard(highlightCrimson = state.isFindChimePlaying) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.AutoMirrored.Rounded.VolumeUp, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Find Device Locator Chime", style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = "Plays a stereo or isolated Left / Right locator chime over active audio output.",
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
                            contentDescription = "Audio earbuds stereo pair",
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
                Text("Make sure your earbuds or headphones are NOT in your ears before playing the high-frequency locator chime.")
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
    isInspected: Boolean,
    actionLabel: String,
    onInspect: () -> Unit,
    onAction: () -> Unit
) {
    val isConnected = device.connectionState == BluetoothConnectionState.CONNECTED
    Surface(
        modifier = Modifier.clickable { onInspect() },
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (isInspected) 0.85f else 0.55f),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            1.dp,
            when {
                isConnected -> LiveEmerald.copy(alpha = 0.65f)
                isInspected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            }
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
                        append("${device.brand.displayName} • ${device.category.label}")
                        device.rssiDbm?.let { append(" • $it dBm") }
                        device.battery.singlePercent?.let { append(" • $it%") }
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            OutlinedButton(onClick = onAction) {
                Text(actionLabel)
            }
        }
    }
}
