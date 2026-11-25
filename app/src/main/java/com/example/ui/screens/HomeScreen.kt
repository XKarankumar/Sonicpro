package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.BluetoothSearching
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Hearing
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Speaker
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.model.AncMode
import com.example.domain.model.BluetoothConnectionState
import com.example.domain.model.DashboardWidgetId
import com.example.ui.components.AcousticVisualizerCanvas
import com.example.ui.components.AnimatedBatteryRing
import com.example.ui.components.LiveTelemetryBadge
import com.example.ui.components.PermissionRationaleBanner
import com.example.ui.components.SoniqGlassCard
import com.example.ui.components.UnavailableBadge
import com.example.ui.theme.LiveEmerald
import com.example.ui.viewmodel.SoniqUiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    state: SoniqUiState,
    onRequestBluetoothPermissions: () -> Unit,
    onNavigateToDevices: () -> Unit,
    onNavigateToStudio: () -> Unit,
    onRefreshLiveTelemetry: () -> Unit,
    onConnectDevice: (String) -> Unit,
    onDisconnectDevice: (String) -> Unit,
    onSelectInspectedDevice: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val active = state.activeDevice
    val bt = state.bluetoothSnapshot
    val visibleWidgets = remember(state.preferences.dashboardWidgets, state.preferences.hiddenDashboardWidgets) {
        state.preferences.dashboardWidgets.filterNot { it in state.preferences.hiddenDashboardWidgets }
    }

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
            // Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "SONIQ",
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Universal Audio Intelligence",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        LiveTelemetryBadge(
                            label = if (active != null) "CONNECTED" else "STANDBY",
                            isActive = active != null
                        )
                        IconButton(
                            onClick = onRefreshLiveTelemetry,
                            modifier = Modifier.testTag("refresh_telemetry_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = "Refresh live audio telemetry",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // Runtime Bluetooth permission prompt
            if (!bt.permissionState.hasAllRequiredBluetoothPermissions) {
                item {
                    PermissionRationaleBanner(
                        title = stringResource(R.string.permission_bt_title),
                        rationale = stringResource(R.string.permission_bt_rationale),
                        buttonText = "Grant Bluetooth Access",
                        onGrantClick = onRequestBluetoothPermissions,
                        testTag = "home_grant_bt_button"
                    )
                }
            }

            // Live Connected Device Hero Card
            item {
                SoniqGlassCard(highlightCrimson = active != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_earbuds_hero_case),
                            contentDescription = "Audio hardware visual",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(92.dp)
                                .clip(RoundedCornerShape(18.dp))
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            if (active != null) {
                                Text(
                                    text = active.name,
                                    style = MaterialTheme.typography.titleLarge,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${active.brand.displayName} • ${active.category.label}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = buildString {
                                        append("Status: Connected")
                                        active.modelNumber?.let { append(" • $it") }
                                        active.firmwareVersion?.let { append(" • FW $it") }
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                Text(
                                    text = "No Audio Device Connected",
                                    style = MaterialTheme.typography.titleLarge,
                                    modifier = Modifier.testTag("empty_device_title")
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = when {
                                        !bt.isBluetoothSupported -> "Bluetooth radio unavailable on this device."
                                        !bt.isBluetoothEnabled -> "Turn on Bluetooth and connect your earbuds, headphones, or speakers."
                                        else -> "Connect paired Bluetooth hardware or scan nearby devices for live telemetry."
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onNavigateToDevices,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("home_scan_devices_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Rounded.BluetoothSearching, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (active != null) "Manage Devices" else "Discover Devices")
                        }
                        if (active != null) {
                            OutlinedButton(
                                onClick = { onDisconnectDevice(active.address) },
                                modifier = Modifier.testTag("home_disconnect_button")
                            ) {
                                Text("Disconnect")
                            }
                        }
                    }
                }
            }

            // Customizable Dashboard Widgets
            visibleWidgets.forEach { widgetId ->
                when (widgetId) {
                    DashboardWidgetId.BATTERY_TRIAD -> item(key = widgetId.name) {
                        SoniqGlassCard {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(widgetId.title, style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        text = active?.battery?.sourceLabel ?: "Connect a device to inspect battery telemetry",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (active != null && !active.battery.hasAnyReading) {
                                    UnavailableBadge("NO TELEMETRY")
                                }
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                val bat = active?.battery
                                val combined = bat?.singlePercent
                                AnimatedBatteryRing(
                                    label = "Left",
                                    percent = bat?.leftPercent,
                                    isCharging = bat?.isLeftCharging == true
                                )
                                AnimatedBatteryRing(
                                    label = if (combined != null && bat.leftPercent == null) "Device ($combined%)" else "Combined",
                                    percent = combined,
                                    isCharging = false
                                )
                                AnimatedBatteryRing(
                                    label = "Right",
                                    percent = bat?.rightPercent,
                                    isCharging = bat?.isRightCharging == true
                                )
                                AnimatedBatteryRing(
                                    label = "Case",
                                    percent = bat?.casePercent,
                                    isCharging = bat?.isCaseCharging == true
                                )
                            }
                            if (active != null && active.battery.leftPercent == null && !active.capabilities.supportsIndividualBattery) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = active.capabilities.protocolNotes,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    DashboardWidgetId.ANC_CONTROL -> item(key = widgetId.name) {
                        val ancSupported = active?.capabilities?.supportsAncControl == true
                        val wearSupported = active?.capabilities?.supportsWearDetection == true
                        SoniqGlassCard {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Rounded.Hearing, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(widgetId.title, style = MaterialTheme.typography.titleMedium)
                                }
                                if (!ancSupported) {
                                    UnavailableBadge()
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.alpha(if (ancSupported) 1f else 0.45f)
                            ) {
                                AncMode.entries.forEach { mode ->
                                    Surface(
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                                        shape = RoundedCornerShape(14.dp),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
                                    ) {
                                        Text(
                                            text = mode.shortLabel,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (active == null) {
                                        "Connect hardware to inspect ANC & Wear Detection capabilities."
                                    } else {
                                        "Hardware ANC, Transparency & Wear Detection require ${active.brand.displayName} OEM partner SDK."
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(1f)
                                )
                                if (!wearSupported) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    UnavailableBadge("SDK REQ")
                                }
                            }
                        }
                    }

                    DashboardWidgetId.AUDIO_VISUALIZER -> item(key = widgetId.name) {
                        val eq = state.equalizerState
                        SoniqGlassCard {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Rounded.GraphicEq, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(widgetId.title, style = MaterialTheme.typography.titleMedium)
                                        Text(
                                            text = "${eq.activePreset.displayName} • ${eq.hardwareBandCount}-Band DSP",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                TextButton(
                                    onClick = onNavigateToStudio,
                                    modifier = Modifier.testTag("home_open_studio_button")
                                ) {
                                    Text("Open Studio")
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            AcousticVisualizerCanvas(
                                isActive = eq.isEngineAvailable && eq.isEnabled,
                                eqBandsMb = eq.bands.map { it.levelMb },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(64.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = eq.statusMessage,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    DashboardWidgetId.CODEC_TELEMETRY -> item(key = widgetId.name) {
                        val codec = active?.codecInfo
                        val spatial = state.spatialState
                        SoniqGlassCard {
                            Text(widgetId.title, style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                TelemetryTile(
                                    label = "Codec",
                                    value = codec?.codecName ?: "Unavailable",
                                    modifier = Modifier.weight(1f)
                                )
                                TelemetryTile(
                                    label = "Sample / Bit",
                                    value = if (codec?.sampleRateHz != null) {
                                        "${codec.sampleRateHz / 1000}kHz/${codec.bitsPerSample ?: 16}b"
                                    } else "Unavailable",
                                    modifier = Modifier.weight(1f)
                                )
                                TelemetryTile(
                                    label = "Spatial HAL",
                                    value = if (spatial.isAvailable) "Ready" else "Unavailable",
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    DashboardWidgetId.RECENT_DEVICES -> item(key = widgetId.name) {
                        SoniqGlassCard {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(widgetId.title, style = MaterialTheme.typography.titleMedium)
                                TextButton(onClick = onNavigateToDevices) {
                                    Text("View All")
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            val bonded = bt.bondedAudioDevices.take(4)
                            if (bonded.isNotEmpty()) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    bonded.forEach { dev ->
                                        val isConn = dev.connectionState == BluetoothConnectionState.CONNECTED
                                        Surface(
                                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                                            shape = RoundedCornerShape(14.dp),
                                            border = BorderStroke(
                                                1.dp,
                                                if (isConn) LiveEmerald.copy(alpha = 0.5f)
                                                else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                                            )
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = when (dev.category.name) {
                                                        "SPEAKER", "SOUNDBAR", "HOME_THEATRE" -> Icons.Rounded.Speaker
                                                        "HEADPHONES" -> Icons.Rounded.Headphones
                                                        else -> Icons.Rounded.Bluetooth
                                                    },
                                                    contentDescription = null,
                                                    tint = if (isConn) LiveEmerald else MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = dev.name,
                                                        style = MaterialTheme.typography.titleSmall,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Text(
                                                        text = buildString {
                                                            append(dev.brand.displayName)
                                                            dev.battery.singlePercent?.let { append(" • $it%") }
                                                        },
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                                TextButton(onClick = {
                                                    onSelectInspectedDevice(dev.address)
                                                    if (isConn) onDisconnectDevice(dev.address) else onConnectDevice(dev.address)
                                                }) {
                                                    Text(if (isConn) "Disconnect" else "Connect")
                                                }
                                            }
                                        }
                                    }
                                }
                            } else if (state.connectionEvents.isNotEmpty()) {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    state.connectionEvents.take(4).forEach { ev ->
                                        val time = remember(ev.timestampMs) {
                                            SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(Date(ev.timestampMs))
                                        }
                                        Text(
                                            text = "${ev.deviceName} • ${ev.eventType} ($time)",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            } else {
                                Text(
                                    text = "No paired or recently connected Bluetooth devices recorded yet.",
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
}

@Composable
private fun TelemetryTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(2.dp))
            Text(value, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
