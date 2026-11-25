package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Cast
import androidx.compose.material.icons.rounded.Cable
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.SpeakerGroup
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.WifiFind
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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.model.AudioDeviceCategory
import com.example.domain.model.BluetoothConnectionState
import com.example.ui.components.LiveTelemetryBadge
import com.example.ui.components.SoniqGlassCard
import com.example.ui.components.UnavailableBadge
import com.example.ui.theme.LiveEmerald
import com.example.ui.viewmodel.SoniqUiState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TheatreScreen(
    state: SoniqUiState,
    onStartNetworkScan: () -> Unit,
    onStopNetworkScan: () -> Unit,
    onRefreshRoutes: () -> Unit,
    onSetVolume: (Int) -> Unit,
    onToggleMute: () -> Unit,
    onMediaPlayPause: () -> Unit,
    onMediaNext: () -> Unit,
    onMediaPrevious: () -> Unit,
    onConnectBluetoothSpeaker: (String) -> Unit,
    onDisconnectBluetoothSpeaker: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val theatre = state.theatreState
    val speakerDevices = state.bluetoothSnapshot.bondedAudioDevices.filter {
        it.category == AudioDeviceCategory.SPEAKER ||
            it.category == AudioDeviceCategory.SOUNDBAR ||
            it.category == AudioDeviceCategory.HOME_THEATRE
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Home Theatre", style = MaterialTheme.typography.headlineMedium)
                        Text(
                            text = theatre.statusMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        LiveTelemetryBadge(
                            label = if (theatre.isWifiConnected) "LAN READY" else "NO WI-FI",
                            isActive = theatre.isWifiConnected
                        )
                        IconButton(onClick = onRefreshRoutes) {
                            Icon(Icons.Rounded.Refresh, contentDescription = "Refresh audio output routes")
                        }
                    }
                }
            }

            // Master Stream Volume & Playback Transport
            item {
                SoniqGlassCard(highlightCrimson = true) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (theatre.isMuted) Icons.AutoMirrored.Rounded.VolumeOff else Icons.AutoMirrored.Rounded.VolumeUp,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("System Volume & Transport", style = MaterialTheme.typography.titleMedium)
                        }
                        Text(
                            text = if (theatre.isMuted) "Muted" else "${theatre.currentStreamVolume} / ${theatre.maxStreamVolume}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Slider(
                        value = theatre.currentStreamVolume.toFloat(),
                        onValueChange = { onSetVolume(it.toInt()) },
                        valueRange = 0f..theatre.maxStreamVolume.coerceAtLeast(1).toFloat(),
                        modifier = Modifier.testTag("theatre_volume_slider"),
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary
                        )
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onToggleMute,
                            modifier = Modifier.weight(1f).testTag("theatre_mute_button")
                        ) {
                            Text(if (theatre.isMuted) "Unmute" else "Mute")
                        }
                        OutlinedButton(
                            onClick = onMediaPrevious,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Rounded.SkipPrevious, contentDescription = "Previous track")
                        }
                        Button(
                            onClick = onMediaPlayPause,
                            modifier = Modifier.weight(1f).testTag("theatre_play_pause_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Rounded.PlayArrow, contentDescription = "Play or pause")
                        }
                        OutlinedButton(
                            onClick = onMediaNext,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Rounded.SkipNext, contentDescription = "Next track")
                        }
                    }
                }
            }

            // LAN mDNS Discovery (Cast, AirPlay, Sonos, Spotify Connect, DLNA)
            item {
                SoniqGlassCard(highlightCrimson = theatre.isScanningNetwork) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Rounded.Cast, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Wi-Fi / LAN Audio Discovery", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    text = "Discovers Cast, AirPlay, Sonos, Spotify Connect & DLNA AVRs via Android NsdManager",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = { if (theatre.isScanningNetwork) onStopNetworkScan() else onStartNetworkScan() },
                        enabled = theatre.isWifiConnected,
                        modifier = Modifier.fillMaxWidth().testTag("theatre_mdns_scan_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(
                            imageVector = if (theatre.isScanningNetwork) Icons.Rounded.Stop else Icons.Rounded.WifiFind,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            when {
                                !theatre.isWifiConnected -> "Connect to Wi-Fi to Scan LAN"
                                theatre.isScanningNetwork -> "Stop LAN Scan"
                                else -> "Discover Network Speakers & AVRs"
                            }
                        )
                    }

                    if (theatre.discoveredNetworkDevices.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            theatre.discoveredNetworkDevices.forEach { endpoint ->
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                                    shape = RoundedCornerShape(14.dp),
                                    border = BorderStroke(1.dp, LiveEmerald.copy(alpha = 0.45f))
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Rounded.SpeakerGroup, contentDescription = null, tint = LiveEmerald)
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = endpoint.serviceName,
                                                style = MaterialTheme.typography.titleSmall,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = buildString {
                                                    append(endpoint.protocolLabel)
                                                    endpoint.modelHint?.let { append(" • $it") }
                                                    if (endpoint.hostAddress != null) {
                                                        append(" • ${endpoint.hostAddress}")
                                                        endpoint.port?.let { append(":$it") }
                                                    }
                                                },
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Active Android Audio Output Routes (HDMI ARC / eARC / USB / A2DP)
            item {
                SoniqGlassCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Cable, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Hardware Output Routes (${theatre.activeOutputRoutes.size})", style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    if (theatre.activeOutputRoutes.isEmpty()) {
                        Text(
                            text = "No active Android output routes reported.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            theatre.activeOutputRoutes.forEach { route ->
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = route.productName,
                                                style = MaterialTheme.typography.titleSmall,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Text(
                                                text = route.typeLabel,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (route.isHomeTheatreCapable) LiveEmerald else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        if (route.supportedEncodings.isNotEmpty()) {
                                            Spacer(modifier = Modifier.height(6.dp))
                                            FlowRow(
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                verticalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                route.supportedEncodings.take(5).forEach { enc ->
                                                    Surface(
                                                        color = MaterialTheme.colorScheme.surface,
                                                        shape = RoundedCornerShape(50)
                                                    ) {
                                                        Text(
                                                            text = enc,
                                                            style = MaterialTheme.typography.labelSmall,
                                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Paired Bluetooth Speakers & Soundbars + Compatibility Boundaries
            item {
                SoniqGlassCard {
                    Text("Bluetooth Speakers & Soundbars", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(6.dp))
                    if (speakerDevices.isEmpty()) {
                        Text(
                            text = "No paired Bluetooth speakers or soundbars found. Pair one on the Devices tab.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            speakerDevices.forEach { spk ->
                                val isConn = spk.connectionState == BluetoothConnectionState.CONNECTED
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(spk.name, style = MaterialTheme.typography.titleSmall)
                                        Text(
                                            text = "${spk.brand.displayName} • ${spk.category.label}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    OutlinedButton(onClick = {
                                        if (isConn) onDisconnectBluetoothSpeaker(spk.address)
                                        else onConnectBluetoothSpeaker(spk.address)
                                    }) {
                                        Text(if (isConn) "Disconnect" else "Connect")
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Proprietary AVR HDMI Input Switching & Vendor Multi-Room Grouping",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        UnavailableBadge("VENDOR SDK")
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }
        }
    }
}
