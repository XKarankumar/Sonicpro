package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.SpatialAudio
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.SurroundSound
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.domain.model.DataSourceMode
import com.example.domain.model.EqBandState
import com.example.domain.model.EqPresetId
import com.example.domain.model.SpatialAudioMode
import com.example.ui.components.DataSourceModePill
import com.example.ui.components.SoniqGlassCard
import com.example.ui.components.UnsupportedBadge
import com.example.ui.theme.LiveEmerald
import com.example.ui.viewmodel.SoniqUiState

@Composable
fun SoundScreen(
    state: SoniqUiState,
    onToggleEqEnabled: (Boolean) -> Unit,
    onSelectPreset: (EqPresetId) -> Unit,
    onUpdateBand: (Int, Int) -> Unit,
    onUpdateBassBoost: (Int) -> Unit,
    onSelectSpatialMode: (SpatialAudioMode) -> Unit,
    onToggleGamingMode: () -> Unit,
    onSaveProfile: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showSaveDialog by remember { mutableStateOf(false) }
    var profileNameInput by remember { mutableStateOf("") }
    val eq = state.equalizerState
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
                        Text("Sound Studio", style = MaterialTheme.typography.headlineMedium)
                        Spacer(modifier = Modifier.height(4.dp))
                        DataSourceModePill(
                            mode = if (state.isDemoPreviewActive) DataSourceMode.DEMO_PREVIEW
                            else DataSourceMode.LIVE_HARDWARE
                        )
                    }
                    OutlinedButton(
                        onClick = { showSaveDialog = true },
                        modifier = Modifier.testTag("save_sound_profile_button")
                    ) {
                        Icon(Icons.Rounded.BookmarkAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save Profile")
                    }
                }
            }

            item {
                SoniqGlassCard(highlightCrimson = eq.isEnabled) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.GraphicEq,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Parametric Equalizer", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    text = if (eq.isEngineAvailable) "Live Android AudioFX DSP" else eq.statusMessage,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (eq.isEngineAvailable) LiveEmerald else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = eq.isEnabled,
                            onCheckedChange = onToggleEqEnabled,
                            enabled = eq.isEngineAvailable || state.isDemoPreviewActive,
                            modifier = Modifier.testTag("eq_master_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    EqCurveCanvas(
                        bands = eq.bands,
                        enabled = eq.isEnabled || state.isDemoPreviewActive,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        EqPresetId.entries.filter { it != EqPresetId.CUSTOM }.forEach { preset ->
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

                    Spacer(modifier = Modifier.height(12.dp))

                    eq.bands.forEach { band ->
                        val dbValue = band.levelMb / 100f
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = band.label,
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.width(56.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Slider(
                                value = band.levelMb.toFloat(),
                                onValueChange = { onUpdateBand(band.index, it.toInt()) },
                                valueRange = band.minMb.toFloat()..band.maxMb.toFloat(),
                                enabled = eq.isEnabled && (eq.isEngineAvailable || state.isDemoPreviewActive),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("eq_slider_${band.index}"),
                                colors = SliderDefaults.colors(
                                    thumbColor = MaterialTheme.colorScheme.primary,
                                    activeTrackColor = MaterialTheme.colorScheme.primary
                                )
                            )
                            Text(
                                text = String.format("%+.1f dB", dbValue),
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.width(64.dp),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            item {
                SoniqGlassCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.SurroundSound, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Bass Boost DSP", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    text = if (eq.bassBoostAvailable) "Hardware/OS BassBoost (${eq.bassBoostStrength / 10}%)"
                                    else "Not exposed by audio output mix",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        if (!eq.bassBoostAvailable && !state.isDemoPreviewActive) {
                            UnsupportedBadge("Unavailable")
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Slider(
                        value = eq.bassBoostStrength.toFloat(),
                        onValueChange = { onUpdateBassBoost(it.toInt()) },
                        valueRange = 0f..1000f,
                        enabled = eq.bassBoostAvailable || state.isDemoPreviewActive,
                        modifier = Modifier.testTag("bass_boost_slider"),
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }

            item {
                val hardwareSpatial = active?.capabilities?.supportsSpatialAudioHardware == true
                val spatialAvailable = hardwareSpatial || eq.virtualizerAvailable
                val selectedMode = active?.spatialAudioMode ?: if (eq.virtualizerStrength > 0) {
                    SpatialAudioMode.FIXED_STAGE
                } else {
                    SpatialAudioMode.OFF
                }

                SoniqGlassCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.SpatialAudio, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Spatial Audio", style = MaterialTheme.typography.titleMedium)
                        }
                        if (!spatialAvailable) {
                            UnsupportedBadge("Unsupported")
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))

                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.alpha(if (spatialAvailable) 1f else 0.5f)
                    ) {
                        SpatialAudioMode.entries.forEach { mode ->
                            val enabledForMode = when (mode) {
                                SpatialAudioMode.HEAD_TRACKING -> hardwareSpatial
                                else -> spatialAvailable
                            }
                            val isSelected = selectedMode == mode && enabledForMode
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable(enabled = enabledForMode) { onSelectSpatialMode(mode) }
                                    .testTag("spatial_mode_${mode.name.lowercase()}"),
                                color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(mode.label, style = MaterialTheme.typography.titleSmall)
                                    if (mode == SpatialAudioMode.HEAD_TRACKING && !hardwareSpatial) {
                                        UnsupportedBadge("Requires IMU")
                                    } else if (isSelected) {
                                        Text("ACTIVE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                val gamingSupported = active?.capabilities?.supportsLowLatencyGamingMode == true
                SoniqGlassCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Rounded.SportsEsports, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Gaming & Low-Latency Mode", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    text = if (gamingSupported) {
                                        if (active?.gamingModeEnabled == true) "Low-latency packet pipeline active" else "Standard A2DP buffering"
                                    } else {
                                        "Vendor low-latency BLE command not exposed by current device"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        if (!gamingSupported) {
                            UnsupportedBadge("Unsupported")
                        } else {
                            Switch(
                                checked = active?.gamingModeEnabled == true,
                                onCheckedChange = { onToggleGamingMode() },
                                modifier = Modifier.testTag("gaming_mode_switch")
                            )
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }
        }
    }

    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Save Sound Profile") },
            text = {
                OutlinedTextField(
                    value = profileNameInput,
                    onValueChange = { profileNameInput = it },
                    label = { Text("Profile Name") },
                    placeholder = { Text("e.g., Late Night Techno") },
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
                TextButton(onClick = { showSaveDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun EqCurveCanvas(
    bands: List<EqBandState>,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)

    Canvas(modifier = modifier) {
        val h = size.height
        val w = size.width
        // Grid lines (-15dB, 0dB, +15dB)
        listOf(0.15f, 0.5f, 0.85f).forEach { frac ->
            drawLine(
                color = gridColor,
                start = Offset(0f, h * frac),
                end = Offset(w, h * frac),
                strokeWidth = 1.dp.toPx()
            )
        }
        if (bands.isEmpty()) return@Canvas

        val pts = bands.mapIndexed { idx, band ->
            val x = if (bands.size == 1) w / 2f else (idx.toFloat() / (bands.size - 1)) * w
            val range = (band.maxMb - band.minMb).coerceAtLeast(1)
            val normY = 1f - ((band.levelMb - band.minMb).toFloat() / range)
            val y = (0.12f + normY * 0.76f) * h
            Offset(x, y)
        }

        val strokePath = Path().apply {
            moveTo(pts.first().x, pts.first().y)
            for (i in 0 until pts.size - 1) {
                val p0 = pts[i]
                val p1 = pts[i + 1]
                val cx = (p0.x + p1.x) / 2f
                cubicTo(cx, p0.y, cx, p1.y, p1.x, p1.y)
            }
        }

        val fillPath = Path().apply {
            addPath(strokePath)
            lineTo(pts.last().x, h)
            lineTo(pts.first().x, h)
            close()
        }

        val alphaMult = if (enabled) 1f else 0.35f
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                listOf(primary.copy(alpha = 0.35f * alphaMult), Color.Transparent)
            )
        )
        drawPath(
            path = strokePath,
            color = primary.copy(alpha = alphaMult),
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
        )
        pts.forEach { p ->
            drawCircle(color = primary.copy(alpha = alphaMult), radius = 5.dp.toPx(), center = p)
            drawCircle(color = Color.White.copy(alpha = alphaMult), radius = 2.5.dp.toPx(), center = p)
        }
    }
}
