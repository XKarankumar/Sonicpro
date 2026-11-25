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
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SpatialAudio
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.domain.model.EqPresetId
import com.example.ui.components.AcousticVisualizerCanvas
import com.example.ui.components.SoniqGlassCard
import com.example.ui.components.UnavailableBadge
import com.example.ui.theme.LiveEmerald
import com.example.ui.viewmodel.SoniqUiState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StudioScreen(
    state: SoniqUiState,
    onToggleEqEnabled: (Boolean) -> Unit,
    onSelectPreset: (EqPresetId) -> Unit,
    onUpdateBand: (Int, Int) -> Unit,
    onUpdateBassBoost: (Int) -> Unit,
    onUpdateVocalGain: (Int) -> Unit,
    onUpdateVirtualizer: (Int) -> Unit,
    onRefreshSpatializer: () -> Unit,
    onOpenSoundSettings: () -> Unit,
    onSaveProfile: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val eq = state.equalizerState
    val spatial = state.spatialState
    var showSaveDialog by remember { mutableStateOf(false) }
    var profileNameInput by remember { mutableStateOf("") }

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
                        Text("Audio Studio & 3D", style = MaterialTheme.typography.headlineMedium)
                        Text(
                            text = eq.statusMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Button(
                        onClick = { showSaveDialog = true },
                        modifier = Modifier.testTag("save_profile_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Rounded.BookmarkAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save")
                    }
                }
            }

            // Visualizer & Master DSP Switch
            item {
                SoniqGlassCard(highlightCrimson = eq.isEnabled) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.GraphicEq, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("AudioFX ${eq.hardwareBandCount}-Band DSP", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    text = if (eq.isEngineAvailable) "Active on Android audio session" else "AudioFX HAL unavailable on emulator/device",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        if (eq.isEngineAvailable) {
                            Switch(
                                checked = eq.isEnabled,
                                onCheckedChange = onToggleEqEnabled,
                                modifier = Modifier.testTag("eq_master_switch")
                            )
                        } else {
                            UnavailableBadge()
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    AcousticVisualizerCanvas(
                        isActive = eq.isEngineAvailable && eq.isEnabled,
                        eqBandsMb = eq.bands.map { it.levelMb },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(74.dp)
                    )
                }
            }

            // Presets
            item {
                SoniqGlassCard {
                    Text("Studio Sound Modes & Presets", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = eq.activePreset.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        EqPresetId.entries.forEach { preset ->
                            val selected = eq.activePreset == preset
                            FilterChip(
                                selected = selected,
                                onClick = { onSelectPreset(preset) },
                                label = { Text(preset.displayName) },
                                modifier = Modifier.testTag("eq_preset_${preset.name.lowercase()}"),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }
            }

            // Dynamic Hardware Equalizer Bands (5 to 10 bands depending on Android HAL)
            item {
                SoniqGlassCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("${eq.bands.size}-Band Parametric Equalizer", style = MaterialTheme.typography.titleMedium)
                        if (!eq.isEngineAvailable) UnavailableBadge("HAL LOCKED")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    eq.bands.forEach { band ->
                        val db = band.levelMb / 100f
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = band.label,
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.width(56.dp)
                            )
                            Slider(
                                value = band.levelMb.toFloat(),
                                onValueChange = { onUpdateBand(band.index, it.toInt()) },
                                valueRange = band.minMb.toFloat()..band.maxMb.toFloat(),
                                enabled = eq.isEngineAvailable && eq.isEnabled,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("eq_band_slider_${band.index}"),
                                colors = SliderDefaults.colors(
                                    thumbColor = MaterialTheme.colorScheme.primary,
                                    activeTrackColor = MaterialTheme.colorScheme.primary
                                )
                            )
                            Text(
                                text = String.format("%+.1fdB", db),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.width(54.dp)
                            )
                        }
                    }
                }
            }

            // Bass Enhancement & Vocal Enhancement
            item {
                SoniqGlassCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.RecordVoiceOver, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Bass & Vocal Enhancements", style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(modifier = Modifier.height(10.dp))

                    // Bass Boost
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Bass Enhancement", style = MaterialTheme.typography.titleSmall)
                        if (eq.bassBoostAvailable) {
                            Text("${eq.bassBoostStrength / 10}%", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        } else {
                            UnavailableBadge()
                        }
                    }
                    Slider(
                        value = eq.bassBoostStrength.toFloat(),
                        onValueChange = { onUpdateBassBoost(it.toInt()) },
                        valueRange = 0f..1000f,
                        enabled = eq.bassBoostAvailable,
                        modifier = Modifier.testTag("bass_boost_slider"),
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary
                        )
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Vocal Clarity (LoudnessEnhancer)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Vocal Clarity (LoudnessEnhancer)", style = MaterialTheme.typography.titleSmall)
                        if (eq.vocalEnhancerAvailable) {
                            Text(
                                text = String.format("+%.1f dB", eq.vocalGainMb / 100f),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            UnavailableBadge()
                        }
                    }
                    Slider(
                        value = eq.vocalGainMb.toFloat(),
                        onValueChange = { onUpdateVocalGain(it.toInt()) },
                        valueRange = 0f..1500f,
                        enabled = eq.vocalEnhancerAvailable,
                        modifier = Modifier.testTag("vocal_gain_slider"),
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }

            // Module 5: Spatial Audio & 3D Sound
            item {
                SoniqGlassCard(highlightCrimson = spatial.isAvailable && spatial.isEnabled) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Rounded.SpatialAudio, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Spatial Audio & 3D Sound", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    text = spatial.immersiveLevel.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (spatial.isAvailable) LiveEmerald else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        IconButton(onClick = onRefreshSpatializer) {
                            Icon(Icons.Rounded.Refresh, contentDescription = "Refresh Spatializer state")
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = spatial.explanation,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SpatialCapabilityBadge("Stereo", spatial.supportsStereo)
                        SpatialCapabilityBadge("5.1 PCM", spatial.supports5Point1)
                        SpatialCapabilityBadge("7.1 Surround", spatial.supports7Point1)
                        SpatialCapabilityBadge("Dolby E-AC3", spatial.supportsDolbyMultichannel)
                        SpatialCapabilityBadge("IMU Head Tracking", spatial.isHeadTrackerAvailable)
                    }

                    if (!spatial.apiSupported && eq.virtualizerAvailable) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Legacy AudioFX Virtualizer", style = MaterialTheme.typography.titleSmall)
                            Text("${eq.virtualizerStrength / 10}%", style = MaterialTheme.typography.labelMedium)
                        }
                        Slider(
                            value = eq.virtualizerStrength.toFloat(),
                            onValueChange = { onUpdateVirtualizer(it.toInt()) },
                            valueRange = 0f..1000f
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    TextButton(onClick = onOpenSoundSettings) {
                        Text("Open Android Spatial Audio Settings")
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }
        }
    }

    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Save Audio Profile") },
            text = {
                OutlinedTextField(
                    value = profileNameInput,
                    onValueChange = { profileNameInput = it },
                    label = { Text("Profile Name") },
                    placeholder = { Text("e.g., Cinema Late Night") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSaveProfile(profileNameInput.ifBlank { "Studio Custom" })
                        profileNameInput = ""
                        showSaveDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun SpatialCapabilityBadge(label: String, supported: Boolean) {
    Surface(
        color = if (supported) LiveEmerald.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        shape = RoundedCornerShape(50),
        border = BorderStroke(
            1.dp,
            if (supported) LiveEmerald.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
        )
    ) {
        Text(
            text = if (supported) "$label • OK" else "$label • N/A",
            style = MaterialTheme.typography.labelSmall,
            color = if (supported) LiveEmerald else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        )
    }
}
