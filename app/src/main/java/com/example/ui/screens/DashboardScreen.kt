package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BluetoothSearching
import androidx.compose.material.icons.rounded.Earbuds
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Hearing
import androidx.compose.material.icons.rounded.LaptopMac
import androidx.compose.material.icons.rounded.NoiseAware
import androidx.compose.material.icons.rounded.NoiseControlOff
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.SpatialAudio
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.SurroundSound
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.model.AncMode
import com.example.domain.model.DashboardWidgetId
import com.example.domain.model.DataSourceMode
import com.example.domain.model.EarbudsDevice
import com.example.domain.model.SpatialAudioMode
import com.example.ui.components.AcousticVisualizerCanvas
import com.example.ui.components.AnimatedBatteryRing
import com.example.ui.components.DataSourceModePill
import com.example.ui.components.PermissionRationaleBanner
import com.example.ui.components.SoniqGlassCard
import com.example.ui.components.UnsupportedBadge
import com.example.ui.theme.DemoAmber
import com.example.ui.theme.LiveEmerald
import com.example.ui.viewmodel.SoniqUiState

@Composable
fun DashboardScreen(
    state: SoniqUiState,
    onRequestBluetoothPermissions: () -> Unit,
    onNavigateToDevices: () -> Unit,
    onNavigateToSound: () -> Unit,
    onEnableDemoPreview: () -> Unit,
    onDisableDemoPreview: () -> Unit,
    onToggleDemoCaseLid: () -> Unit,
    onRefreshLiveTelemetry: () -> Unit,
    onSelectAncMode: (AncMode) -> Unit,
    onChangeAncIntensity: (Float) -> Unit,
    onToggleGamingMode: () -> Unit,
    onCycleSpatialAudio: (SpatialAudioMode) -> Unit,
    onToggleInEarDetection: () -> Unit,
    onSwitchMultipointHost: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val activeDevice = state.activeDevice
    val visibleWidgets = state.preferences.dashboardWidgets.filterNot {
        it in state.preferences.hiddenDashboardWidgets
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
            item {
                DashboardTopHeader(
                    activeDevice = activeDevice,
                    isDemo = state.isDemoPreviewActive,
                    onRefreshLive = onRefreshLiveTelemetry,
                    onExitDemo = onDisableDemoPreview
                )
            }

            if (!state.bluetoothSnapshot.permissionState.hasAllRequiredBluetoothPermissions) {
                item {
                    PermissionRationaleBanner(
                        title = stringResource(R.string.permission_bt_title),
                        rationale = stringResource(R.string.permission_bt_rationale),
                        buttonText = "Grant Bluetooth Access",
                        onGrantClick = onRequestBluetoothPermissions,
                        testTag = "grant_bt_permission_button"
                    )
                }
            }

            if (activeDevice == null) {
                item {
                    EmptyNoDeviceStage(
                        bluetoothEnabled = state.bluetoothSnapshot.isBluetoothEnabled,
                        bluetoothSupported = state.bluetoothSnapshot.isBluetoothSupported,
                        bondedCount = state.bluetoothSnapshot.bondedAudioDevices.size,
                        onOpenDevices = onNavigateToDevices,
                        onLaunchDemo = onEnableDemoPreview
                    )
                }
            } else {
                item {
                    HeroEarbudsStageCard(
                        device = activeDevice,
                        onToggleCaseLid = onToggleDemoCaseLid,
                        onOpenDevices = onNavigateToDevices
                    )
                }

                items(visibleWidgets, key = { it.name }) { widgetId ->
                    when (widgetId) {
                        DashboardWidgetId.BATTERY_TRIAD -> BatteryTriadCard(device = activeDevice)
                        DashboardWidgetId.ANC_CONTROL -> AncControlCard(
                            device = activeDevice,
                            onSelectAncMode = onSelectAncMode,
                            onChangeIntensity = onChangeAncIntensity
                        )
                        DashboardWidgetId.AUDIO_VISUALIZER -> AudioVisualizerCard(
                            device = activeDevice,
                            state = state,
                            onOpenSound = onNavigateToSound
                        )
                        DashboardWidgetId.QUICK_CONTROLS -> QuickControlsGridCard(
                            device = activeDevice,
                            state = state,
                            onToggleGamingMode = onToggleGamingMode,
                            onCycleSpatial = onCycleSpatialAudio,
                            onToggleInEar = onToggleInEarDetection,
                            onOpenSound = onNavigateToSound
                        )
                        DashboardWidgetId.CODEC_TELEMETRY -> CodecAndProtocolCard(device = activeDevice)
                        DashboardWidgetId.MULTIPOINT_STATUS -> MultipointStatusCard(
                            device = activeDevice,
                            onSwitchHost = onSwitchMultipointHost
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }
        }
    }
}

@Composable
private fun DashboardTopHeader(
    activeDevice: EarbudsDevice?,
    isDemo: Boolean,
    onRefreshLive: () -> Unit,
    onExitDemo: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "SONIQ",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "PRO",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            DataSourceModePill(
                mode = if (isDemo) DataSourceMode.DEMO_PREVIEW else DataSourceMode.LIVE_HARDWARE
            )
        }

        if (isDemo) {
            OutlinedButton(
                onClick = onExitDemo,
                modifier = Modifier.testTag("exit_demo_button"),
                border = BorderStroke(1.dp, DemoAmber.copy(alpha = 0.6f))
            ) {
                Text(
                    text = "Exit Demo",
                    color = DemoAmber,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        } else if (activeDevice != null) {
            IconButton(
                onClick = onRefreshLive,
                modifier = Modifier.testTag("refresh_telemetry_button")
            ) {
                Icon(
                    imageVector = Icons.Rounded.Refresh,
                    contentDescription = "Refresh Bluetooth Telemetry",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun EmptyNoDeviceStage(
    bluetoothEnabled: Boolean,
    bluetoothSupported: Boolean,
    bondedCount: Int,
    onOpenDevices: () -> Unit,
    onLaunchDemo: () -> Unit
) {
    SoniqGlassCard(highlightCrimson = true) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(190.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Color(0xFF09090B))
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_earbuds_hero_case),
                contentDescription = "SONIQ Wireless Earbuds 3D Render",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color(0xCC09090B))
                        )
                    )
            )
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(14.dp)
            ) {
                Text(
                    text = when {
                        !bluetoothSupported -> "No Bluetooth Radio Detected"
                        !bluetoothEnabled -> "Bluetooth Radio is Off"
                        else -> "No Earbuds Connected"
                    },
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White
                )
                Text(
                    text = if (bondedCount > 0) {
                        "$bondedCount paired device(s) available in Devices tab."
                    } else {
                        "Connect real Bluetooth earbuds or preview the interactive Demo Sandbox."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFD4D4DE)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onOpenDevices,
                modifier = Modifier
                    .weight(1f)
                    .testTag("empty_scan_pair_button"),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(
                    imageVector = Icons.Rounded.BluetoothSearching,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Discover", maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            OutlinedButton(
                onClick = onLaunchDemo,
                modifier = Modifier
                    .weight(1f)
                    .testTag("launch_demo_preview_button"),
                border = BorderStroke(1.dp, DemoAmber.copy(alpha = 0.65f))
            ) {
                Icon(
                    imageVector = Icons.Rounded.Science,
                    contentDescription = null,
                    tint = DemoAmber,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Demo Mode", color = DemoAmber, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun HeroEarbudsStageCard(
    device: EarbudsDevice,
    onToggleCaseLid: () -> Unit,
    onOpenDevices: () -> Unit
) {
    val isDemo = device.dataSourceMode == DataSourceMode.DEMO_PREVIEW
    val scale by animateFloatAsState(
        targetValue = if (device.isCaseOpen || !isDemo) 1f else 0.94f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 260f),
        label = "case_lid_scale"
    )

    SoniqGlassCard(highlightCrimson = true) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = device.brand.displayName.uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = device.name,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = buildString {
                        append(device.address)
                        device.firmwareVersion?.let { append(" • FW $it") }
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            TextButton(
                onClick = onOpenDevices,
                modifier = Modifier.testTag("switch_device_button")
            ) {
                Icon(Icons.Rounded.SwapHoriz, contentDescription = "Switch device", modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Switch")
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(176.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Color(0xFF09090B))
                .clickable(enabled = isDemo) { onToggleCaseLid() },
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(
                    id = if (device.isCaseOpen || !isDemo) R.drawable.img_earbuds_hero_case
                    else R.drawable.img_charging_case
                ),
                contentDescription = "${device.name} 3D illustration",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(scaleX = scale, scaleY = scale)
            )
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(10.dp),
                color = Color(0xB809090B),
                shape = RoundedCornerShape(50),
                border = BorderStroke(1.dp, LiveEmerald.copy(alpha = 0.55f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(if (isDemo) DemoAmber else LiveEmerald)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isDemo) {
                            if (device.isCaseOpen) "Case Open (Tap to close)" else "Case Closed (Tap to open)"
                        } else {
                            "Connected"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun BatteryTriadCard(device: EarbudsDevice) {
    val bat = device.battery
    SoniqGlassCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Battery Telemetry", style = MaterialTheme.typography.titleMedium)
            Text(
                text = bat.sourceLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.height(14.dp))

        if (device.capabilities.supportsIndividualBattery) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                AnimatedBatteryRing("LEFT", bat.leftPercent, bat.isLeftCharging)
                AnimatedBatteryRing("RIGHT", bat.rightPercent, bat.isRightCharging)
                AnimatedBatteryRing("CASE", bat.casePercent, bat.isCaseCharging)
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AnimatedBatteryRing("EARBUDS", bat.singlePercent, false, ringSize = 86.dp)
                Column(modifier = Modifier.padding(start = 14.dp)) {
                    Text(
                        text = if (bat.singlePercent != null) {
                            "Combined battery reported by Android Bluetooth."
                        } else {
                            "Device does not broadcast battery level over standard GATT/HFP."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Individual L / R / Case channels unavailable on generic profile.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun AncControlCard(
    device: EarbudsDevice,
    onSelectAncMode: (AncMode) -> Unit,
    onChangeIntensity: (Float) -> Unit
) {
    val supported = device.capabilities.supportsAncControl
    SoniqGlassCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Noise Control", style = MaterialTheme.typography.titleMedium)
            if (!supported) {
                UnsupportedBadge("Hardware Unsupported")
            } else {
                Text(
                    text = device.ancMode.shortLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .alpha(if (supported) 1f else 0.45f),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AncMode.entries.forEach { mode ->
                val selected = device.ancMode == mode && supported
                val icon = when (mode) {
                    AncMode.OFF -> Icons.Rounded.NoiseControlOff
                    AncMode.NOISE_CANCELLATION -> Icons.Rounded.NoiseAware
                    AncMode.TRANSPARENCY -> Icons.Rounded.Hearing
                    AncMode.ADAPTIVE -> Icons.Rounded.Earbuds
                }
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable(enabled = supported) { onSelectAncMode(mode) }
                        .testTag("anc_mode_${mode.name.lowercase()}"),
                    color = if (selected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(16.dp),
                    border = if (selected) null else BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = mode.label,
                            tint = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = mode.shortLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        AnimatedVisibility(visible = supported && device.ancMode != AncMode.OFF) {
            Column(modifier = Modifier.padding(top = 10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Ambient Intensity",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${(device.ancIntensity * 100).toInt()}%",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Slider(
                    value = device.ancIntensity,
                    onValueChange = onChangeIntensity,
                    valueRange = 0.2f..1f,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
        }
    }
}

@Composable
private fun AudioVisualizerCard(
    device: EarbudsDevice,
    state: SoniqUiState,
    onOpenSound: () -> Unit
) {
    SoniqGlassCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Acoustic Signature", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "${state.equalizerState.activePreset.displayName} • Bass ${(state.equalizerState.bassBoostStrength / 10)}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            TextButton(onClick = onOpenSound) {
                Icon(Icons.Rounded.GraphicEq, contentDescription = "Open EQ", modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Tune EQ")
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        AcousticVisualizerCanvas(
            isActive = state.equalizerState.isEnabled || state.isDemoPreviewActive,
            ancMode = device.ancMode,
            eqBandsMb = state.equalizerState.bands.map { it.levelMb },
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
        )
    }
}

@Composable
private fun QuickControlsGridCard(
    device: EarbudsDevice,
    state: SoniqUiState,
    onToggleGamingMode: () -> Unit,
    onCycleSpatial: (SpatialAudioMode) -> Unit,
    onToggleInEar: () -> Unit,
    onOpenSound: () -> Unit
) {
    val spatialSupported = device.capabilities.supportsSpatialAudioHardware || state.equalizerState.virtualizerAvailable
    val nextSpatial = when (device.spatialAudioMode) {
        SpatialAudioMode.OFF -> SpatialAudioMode.FIXED_STAGE
        SpatialAudioMode.FIXED_STAGE -> if (device.capabilities.supportsSpatialAudioHardware) SpatialAudioMode.HEAD_TRACKING else SpatialAudioMode.OFF
        SpatialAudioMode.HEAD_TRACKING -> SpatialAudioMode.OFF
    }

    SoniqGlassCard {
        Text("Quick Controls", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            QuickTile(
                title = "Gaming Mode",
                subtitle = if (device.gamingModeEnabled) "Low Latency ON" else "Standard Latency",
                icon = Icons.Rounded.SportsEsports,
                active = device.gamingModeEnabled,
                supported = device.capabilities.supportsLowLatencyGamingMode,
                onClick = onToggleGamingMode,
                modifier = Modifier
                    .weight(1f)
                    .testTag("tile_gaming_mode")
            )
            QuickTile(
                title = "Spatial Audio",
                subtitle = if (state.equalizerState.virtualizerStrength > 0 || device.spatialAudioMode != SpatialAudioMode.OFF) {
                    device.spatialAudioMode.label
                } else {
                    "Off"
                },
                icon = Icons.Rounded.SpatialAudio,
                active = state.equalizerState.virtualizerStrength > 0 || device.spatialAudioMode != SpatialAudioMode.OFF,
                supported = spatialSupported,
                onClick = { onCycleSpatial(nextSpatial) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("tile_spatial_audio")
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            QuickTile(
                title = "In-Ear Sensor",
                subtitle = if (device.inEarDetectionEnabled) "Auto Play/Pause" else "Disabled",
                icon = Icons.Rounded.Earbuds,
                active = device.inEarDetectionEnabled,
                supported = device.capabilities.supportsInEarDetection,
                onClick = onToggleInEar,
                modifier = Modifier
                    .weight(1f)
                    .testTag("tile_in_ear")
            )
            QuickTile(
                title = "Bass Boost",
                subtitle = if (state.equalizerState.bassBoostAvailable) "${state.equalizerState.bassBoostStrength / 10}% DSP" else "DSP N/A",
                icon = Icons.Rounded.SurroundSound,
                active = state.equalizerState.bassBoostStrength > 0,
                supported = state.equalizerState.bassBoostAvailable || state.isDemoPreviewActive,
                onClick = onOpenSound,
                modifier = Modifier
                    .weight(1f)
                    .testTag("tile_bass_boost")
            )
        }
    }
}

@Composable
private fun QuickTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    active: Boolean,
    supported: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .clickable { onClick() }
            .alpha(if (supported) 1f else 0.55f),
        color = if (active && supported) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(
            1.dp,
            if (active && supported) MaterialTheme.colorScheme.primary.copy(alpha = 0.65f)
            else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = if (active && supported) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                )
                if (!supported) {
                    UnsupportedBadge("N/A")
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = if (supported) subtitle else "Unsupported",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun CodecAndProtocolCard(device: EarbudsDevice) {
    SoniqGlassCard {
        Text("Codec & Protocol Telemetry", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TelemetryMiniBox(
                label = "AUDIO CODEC",
                value = device.codecInfo?.codecName ?: "Not exposed",
                modifier = Modifier.weight(1f)
            )
            TelemetryMiniBox(
                label = "SAMPLE RATE",
                value = device.codecInfo?.sampleRateHz?.let { "${it / 1000} kHz" } ?: "N/A",
                modifier = Modifier.weight(1f)
            )
            TelemetryMiniBox(
                label = "SIGNAL RSSI",
                value = device.rssiDbm?.let { "$it dBm" } ?: "Bonded",
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = device.capabilities.protocolNotes,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun TelemetryMiniBox(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun MultipointStatusCard(
    device: EarbudsDevice,
    onSwitchHost: (String) -> Unit
) {
    SoniqGlassCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Dual-Device Multipoint", style = MaterialTheme.typography.titleMedium)
            if (!device.capabilities.supportsDualDeviceMultipoint) {
                UnsupportedBadge("Unsupported")
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        if (!device.capabilities.supportsDualDeviceMultipoint) {
            Text(
                text = "Multipoint host switching is controlled directly by this device's hardware.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            device.multipointHosts.forEach { host ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onSwitchHost(host.hostName) }
                        .padding(vertical = 8.dp, horizontal = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (host.hostType.contains("Laptop")) Icons.Rounded.LaptopMac else Icons.Rounded.PhoneAndroid,
                        contentDescription = host.hostName,
                        tint = if (host.isActiveAudioSource) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(host.hostName, style = MaterialTheme.typography.titleSmall)
                        Text(
                            text = host.hostType,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = if (host.isActiveAudioSource) "ACTIVE" else "STANDBY",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (host.isActiveAudioSource) LiveEmerald else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
