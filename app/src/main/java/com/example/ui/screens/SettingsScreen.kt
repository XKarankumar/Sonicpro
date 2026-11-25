package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Hub
import androidx.compose.material.icons.rounded.NewReleases
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PrivacyTip
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.BuildConfig
import com.example.R
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
fun SettingsScreen(
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
    onExportCompatibilityReport: () -> Unit,
    onClearHistory: () -> Unit,
    onSetDiagnosticsConsent: (Boolean) -> Unit,
    onRunDiagnostics: () -> Unit,
    onClearDiagnosticLogs: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showAllIntegrations by remember { mutableStateOf(false) }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 680.dp)
                .testTag("settings_list"),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Text("Settings & Ecosystem", style = MaterialTheme.typography.headlineMedium)
                Text(
                    text = "OEM integrations, profiles, privacy, diagnostics & backup",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // OEM Integration Architecture Matrix (8 modules covering 15 brands)
            item {
                SoniqGlassCard(highlightCrimson = true) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Hub, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("OEM Integrations (${state.oemIntegrations.size} Modules)", style = MaterialTheme.typography.titleMedium)
                        }
                        TextButton(onClick = { showAllIntegrations = !showAllIntegrations }) {
                            Text(if (showAllIntegrations) "Collapse" else "Show All")
                        }
                    }
                    Text(
                        text = "Covering 15 audio brands with transparent protocol boundaries — no simulated OEM controls.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    val displayed = if (showAllIntegrations) state.oemIntegrations else state.oemIntegrations.take(4)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        displayed.forEach { integration ->
                            val info = integration.info
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                                    Text(info.displayName, style = MaterialTheme.typography.titleSmall)
                                    Text(
                                        text = "Brands: ${info.coveredBrands.joinToString { it.displayName }}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = info.sdkStatus,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Saved Device Profiles
            item {
                SoniqGlassCard {
                    Text("Saved Audio Profiles (${state.savedProfiles.size})", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    if (state.savedProfiles.isEmpty()) {
                        Text(
                            text = "Save an EQ & DSP preset in the Studio tab to persist device profiles here.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            state.savedProfiles.forEach { profile ->
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(profile.profileName, style = MaterialTheme.typography.titleSmall)
                                            Text(
                                                text = "${profile.deviceName} • ${profile.eqPresetName}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        TextButton(onClick = { onApplyProfile(profile) }) { Text("Apply") }
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

            // Theme & Home Dashboard Customization
            item {
                SoniqGlassCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Theme & Home Layout", style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ThemePreference.entries.forEach { pref ->
                            val selected = state.preferences.themePreference == pref
                            Surface(
                                onClick = { onSelectTheme(pref) },
                                color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(
                                    1.dp,
                                    if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(pref.label, style = MaterialTheme.typography.titleSmall)
                                    if (selected) Text("Active", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("HOME DASHBOARD WIDGETS", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(6.dp))
                    state.preferences.dashboardWidgets.forEachIndexed { idx, widget ->
                        val visible = widget !in state.preferences.hiddenDashboardWidgets
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(widget.title, style = MaterialTheme.typography.titleSmall)
                                Text(widget.subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (idx > 0) {
                                IconButton(onClick = { onMoveWidgetUp(widget) }) {
                                    Icon(Icons.Rounded.ArrowUpward, contentDescription = "Move ${widget.title} up")
                                }
                            }
                            Switch(checked = visible, onCheckedChange = { onToggleWidgetVisibility(widget) })
                        }
                    }
                }
            }

            // Notifications
            item {
                val notif = state.preferences.notificationSettings
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
                            TextButton(onClick = onRequestNotificationPermission) { Text("Enable OS Alerts") }
                        }
                    }
                    Text(
                        text = stringResource(R.string.permission_notifications_rationale),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    SettingsToggleRow(
                        label = "Low Battery Alert (${notif.batteryLowThreshold}%)",
                        checked = notif.batteryLowAlertEnabled,
                        onCheckedChange = { onUpdateNotificationSettings(notif.copy(batteryLowAlertEnabled = it)) }
                    )
                    SettingsToggleRow(
                        label = "Bluetooth Connection Events",
                        checked = notif.connectionAlertEnabled,
                        onCheckedChange = { onUpdateNotificationSettings(notif.copy(connectionAlertEnabled = it)) }
                    )
                    SettingsToggleRow(
                        label = "Record Coarse Location on Disconnect",
                        checked = notif.lastKnownLocationOnDisconnect,
                        onCheckedChange = { onUpdateNotificationSettings(notif.copy(lastKnownLocationOnDisconnect = it)) }
                    )
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
                            Text("Battery & Event History", style = MaterialTheme.typography.titleMedium)
                        }
                        TextButton(onClick = onClearHistory) { Text("Clear") }
                    }
                    if (state.connectionEvents.isEmpty() && state.batteryHistory.isEmpty()) {
                        Text(
                            text = "No hardware events recorded yet. Telemetry is captured locally when Bluetooth devices connect.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        state.connectionEvents.take(5).forEach { ev ->
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
                }
            }

            // Diagnostics, Privacy & Backup
            item {
                SoniqGlassCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.BugReport, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Diagnostics, Privacy & Backup", style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    SettingsToggleRow(
                        label = "Store Local Diagnostics & Crash Logs",
                        checked = state.preferences.diagnosticsConsentGranted,
                        onCheckedChange = onSetDiagnosticsConsent
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onRunDiagnostics,
                            enabled = !state.isRunningDiagnostics,
                            modifier = Modifier.weight(1f).testTag("run_diagnostics_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text(if (state.isRunningDiagnostics) "Running..." else "Run Diagnostics")
                        }
                        OutlinedButton(
                            onClick = onExportCompatibilityReport,
                            modifier = Modifier.weight(1f).testTag("export_compatibility_button")
                        ) {
                            Text("Export Report")
                        }
                    }

                    if (state.diagnosticResults.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            state.diagnosticResults.forEach { res ->
                                Text(
                                    text = "${if (res.passed) "PASS" else "INFO"} • ${res.title}: ${res.detail}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (res.passed) LiveEmerald else WarningOrange
                                )
                            }
                        }
                    }

                    if (state.diagnosticLogs.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("RECENT LOGS (${state.diagnosticLogs.size})", style = MaterialTheme.typography.labelSmall)
                            TextButton(onClick = onClearDiagnosticLogs) { Text("Clear Logs") }
                        }
                        state.diagnosticLogs.take(4).forEach { log ->
                            Text(
                                text = "[${log.severity}] ${log.category}: ${log.message}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onExportBackup,
                            modifier = Modifier.weight(1f).testTag("export_backup_button")
                        ) {
                            Icon(Icons.Rounded.Upload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Export JSON")
                        }
                        OutlinedButton(
                            onClick = onImportBackup,
                            modifier = Modifier.weight(1f).testTag("import_backup_button")
                        ) {
                            Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Restore JSON")
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.PrivacyTip, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Zero personal data collection. All audio telemetry, profiles & logs stay strictly on-device.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Version & Release Notes
            item {
                SoniqGlassCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.NewReleases, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("SONIQ v${BuildConfig.VERSION_NAME} Release Notes", style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    state.releaseNotes.forEach { note ->
                        Text(
                            text = "v${note.versionName} (${note.releaseDate})" + if (note.isCurrent) " • Current" else "",
                            style = MaterialTheme.typography.titleSmall,
                            color = if (note.isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                        note.highlights.forEach { hl ->
                            Text("• $hl", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }
        }
    }
}

@Composable
private fun SettingsToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
