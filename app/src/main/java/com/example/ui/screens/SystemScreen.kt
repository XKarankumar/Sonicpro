package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.HelpOutline
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.BuildConfig
import com.example.data.local.BatteryHistoryEntity
import com.example.data.local.DeviceProfileEntity
import com.example.domain.model.DashboardWidgetId
import com.example.domain.model.NotificationSettings
import com.example.domain.model.ThemePreference
import com.example.ui.components.SoniqGlassCard
import com.example.ui.theme.LiveEmerald
import com.example.ui.theme.WarningOrange
import com.example.ui.viewmodel.SoniqUiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SystemScreen(
    state: SoniqUiState,
    onSelectTheme: (ThemePreference) -> Unit,
    onToggleWidgetVisibility: (DashboardWidgetId) -> Unit,
    onMoveWidgetUp: (DashboardWidgetId) -> Unit,
    onUpdateNotificationSettings: (NotificationSettings) -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onApplyProfile: (DeviceProfileEntity) -> Unit,
    onDeleteProfile: (Long) -> Unit,
    onExportBackup: () -> Unit,
    onImportBackup: () -> Unit,
    onClearHistory: () -> Unit,
    onSetDiagnosticsConsent: (Boolean) -> Unit,
    onRunDiagnostics: () -> Unit,
    onClearDiagnosticLogs: () -> Unit,
    modifier: Modifier = Modifier
) {
    val active = state.activeDevice
    val notif = state.preferences.notificationSettings

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
                Text("System & Telemetry", style = MaterialTheme.typography.headlineMedium)
                Text(
                    text = "SONIQ PRO v${BuildConfig.VERSION_NAME} • Local-First Privacy",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Theme & Dashboard Customization
            item {
                SoniqGlassCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Appearance & Theme", style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ThemePreference.entries.forEach { theme ->
                            val selected = state.preferences.themePreference == theme
                            FilterChip(
                                selected = selected,
                                onClick = { onSelectTheme(theme) },
                                label = {
                                    Text(
                                        text = when (theme) {
                                            ThemePreference.DARK_OLED -> "OLED Dark"
                                            ThemePreference.LIGHT_CRIMSON -> "Light"
                                            ThemePreference.SYSTEM -> "System"
                                        }
                                    )
                                },
                                modifier = Modifier.testTag("theme_${theme.name.lowercase()}"),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Widgets, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Customize Studio Dashboard", style = MaterialTheme.typography.titleSmall)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    state.preferences.dashboardWidgets.forEachIndexed { index, widget ->
                        val visible = widget !in state.preferences.hiddenDashboardWidgets
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { onMoveWidgetUp(widget) },
                                enabled = index > 0
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.ArrowUpward,
                                    contentDescription = "Move ${widget.title} up",
                                    tint = if (index > 0) MaterialTheme.colorScheme.onSurface
                                    else MaterialTheme.colorScheme.outline
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(widget.title, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    text = widget.subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = visible,
                                onCheckedChange = { onToggleWidgetVisibility(widget) }
                            )
                        }
                    }
                }
            }

            // Battery & Connection History
            item {
                SoniqGlassCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.History, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Battery & Connection History", style = MaterialTheme.typography.titleMedium)
                        }
                        if (state.batteryHistory.isNotEmpty() || state.connectionEvents.isNotEmpty()) {
                            TextButton(onClick = onClearHistory) {
                                Text("Clear")
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))

                    if (state.batteryHistory.isEmpty() && state.connectionEvents.isEmpty()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Rounded.BatteryChargingFull,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Real battery readings and Bluetooth connect/disconnect events are logged here automatically when live earbuds connect.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        if (state.batteryHistory.size >= 2) {
                            BatteryHistoryChart(
                                entries = state.batteryHistory.take(20).reversed(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(90.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                        }
                        state.connectionEvents.take(5).forEach { ev ->
                            val timeStr = remember(ev.timestampMs) {
                                SimpleDateFormat("MMM d HH:mm", Locale.getDefault()).format(Date(ev.timestampMs))
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("${ev.deviceName} • ${ev.eventType}", style = MaterialTheme.typography.titleSmall)
                                    Text(ev.detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text(timeStr, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }

            // Device Profiles & Backup
            item {
                SoniqGlassCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Backup, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Device Profiles & JSON Backup", style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onExportBackup,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("export_backup_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Export JSON")
                        }
                        OutlinedButton(
                            onClick = onImportBackup,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("import_backup_button")
                        ) {
                            Icon(Icons.Rounded.Restore, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Restore")
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    if (state.savedProfiles.isEmpty()) {
                        Text(
                            text = "No saved profiles yet. Save an EQ & gesture preset from the Sound tab.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            state.savedProfiles.forEach { profile ->
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onApplyProfile(profile) }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(profile.profileName, style = MaterialTheme.typography.titleSmall)
                                            Text(
                                                text = "${profile.deviceName} • ${profile.eqPresetName}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        IconButton(onClick = { onDeleteProfile(profile.id) }) {
                                            Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete profile")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Notifications
            item {
                SoniqGlassCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Alerts & Notifications", style = MaterialTheme.typography.titleMedium)
                        }
                        if (!state.bluetoothSnapshot.permissionState.hasNotificationPermission) {
                            TextButton(onClick = onRequestNotificationPermission) {
                                Text("Enable OS")
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    NotifRow(
                        title = "Low Battery Warning (${notif.batteryLowThreshold}%)",
                        checked = notif.batteryLowAlertEnabled,
                        onToggle = { onUpdateNotificationSettings(notif.copy(batteryLowAlertEnabled = it)) }
                    )
                    if (notif.batteryLowAlertEnabled) {
                        Slider(
                            value = notif.batteryLowThreshold.toFloat(),
                            onValueChange = {
                                onUpdateNotificationSettings(notif.copy(batteryLowThreshold = it.toInt()))
                            },
                            valueRange = 10f..40f,
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                    NotifRow(
                        title = "Bluetooth Connection Alerts",
                        checked = notif.connectionAlertEnabled,
                        onToggle = { onUpdateNotificationSettings(notif.copy(connectionAlertEnabled = it)) }
                    )
                    NotifRow(
                        title = "Release & Firmware Notes Alerts",
                        checked = notif.firmwareAlertEnabled,
                        onToggle = { onUpdateNotificationSettings(notif.copy(firmwareAlertEnabled = it)) }
                    )
                    NotifRow(
                        title = "Log Coarse Location on Disconnect",
                        checked = notif.lastKnownLocationOnDisconnect,
                        onToggle = { onUpdateNotificationSettings(notif.copy(lastKnownLocationOnDisconnect = it)) }
                    )
                }
            }

            // Firmware & Release Notes
            item {
                SoniqGlassCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.SystemUpdate, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Firmware & App Updates", style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Device Firmware: ${active?.firmwareVersion ?: "Not exposed by connected device"}",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = if (active?.capabilities?.supportsFirmwareOta == true) {
                                    "OTA service available"
                                } else {
                                    "Vendor OTA firmware flashing requires signed manufacturer packets and is never simulated."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    state.releaseNotes.forEach { release ->
                        Column(modifier = Modifier.padding(vertical = 4.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "v${release.versionName}",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = if (release.isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "${release.releaseDate}${if (release.isCurrent) " • CURRENT" else ""}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            release.highlights.forEach { line ->
                                Text(
                                    text = "• $line",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Diagnostics & Help
            item {
                SoniqGlassCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.BugReport, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Diagnostics & Troubleshooting", style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    NotifRow(
                        title = "Store Local Diagnostics Logs (Consent)",
                        checked = state.preferences.diagnosticsConsentGranted,
                        onToggle = onSetDiagnosticsConsent
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = onRunDiagnostics,
                            enabled = !state.isRunningDiagnostics,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("run_diagnostics_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text(if (state.isRunningDiagnostics) "Checking..." else "Run Health Check")
                        }
                        if (state.diagnosticLogs.isNotEmpty()) {
                            OutlinedButton(onClick = onClearDiagnosticLogs) {
                                Text("Clear Logs")
                            }
                        }
                    }

                    if (state.diagnosticResults.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            state.diagnosticResults.forEach { check ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (check.passed) Icons.Rounded.CheckCircle else Icons.Rounded.ErrorOutline,
                                        contentDescription = null,
                                        tint = if (check.passed) LiveEmerald else WarningOrange,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(check.title, style = MaterialTheme.typography.titleSmall)
                                        Text(check.detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.HelpOutline, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Troubleshooting Tips", style = MaterialTheme.typography.titleSmall)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "• Why are ANC or L/R battery rings locked on generic earbuds? Generic Bluetooth A2DP/HFP only exposes single battery and audio codecs; proprietary ANC/gesture packets require vendor keys.\n• Why does EQ feel subtle? Disable other system equalizer apps so Android AudioFX session 0 has exclusive control.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }
        }
    }
}

@Composable
private fun NotifRow(
    title: String,
    checked: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f).padding(end = 8.dp)
        )
        Switch(checked = checked, onCheckedChange = onToggle)
    }
}

@Composable
private fun BatteryHistoryChart(
    entries: List<BatteryHistoryEntity>,
    modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary
    val grid = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
    Canvas(modifier = modifier) {
        drawLine(grid, Offset(0f, size.height * 0.5f), Offset(size.width, size.height * 0.5f), 1.dp.toPx())
        val pts = entries.mapIndexedNotNull { idx, e ->
            val pct = e.singlePercent ?: e.leftPercent ?: e.rightPercent ?: return@mapIndexedNotNull null
            val x = if (entries.size <= 1) size.width / 2f else (idx.toFloat() / (entries.size - 1)) * size.width
            val y = (1f - (pct.coerceIn(0, 100) / 100f)) * size.height * 0.85f + size.height * 0.08f
            Offset(x, y)
        }
        if (pts.size < 2) return@Canvas
        val path = Path().apply {
            moveTo(pts.first().x, pts.first().y)
            pts.drop(1).forEach { lineTo(it.x, it.y) }
        }
        drawPath(path, primary, style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round))
        pts.forEach { drawCircle(primary, 3.5.dp.toPx(), it) }
    }
}
