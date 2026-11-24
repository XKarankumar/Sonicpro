package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.domain.model.DashboardWidgetId
import com.example.domain.model.EqPresetId
import com.example.domain.model.LastKnownLocationRecord
import com.example.domain.model.NotificationSettings
import com.example.domain.model.ThemePreference
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.soniqDataStore: DataStore<Preferences> by preferencesDataStore(name = "soniq_prefs")

data class StoredAppPreferences(
    val themePreference: ThemePreference = ThemePreference.DARK_OLED,
    val dashboardWidgets: List<DashboardWidgetId> = DashboardWidgetId.entries.toList(),
    val hiddenDashboardWidgets: Set<DashboardWidgetId> = emptySet(),
    val notificationSettings: NotificationSettings = NotificationSettings(),
    val diagnosticsConsentGranted: Boolean = false,
    val activeEqPreset: EqPresetId = EqPresetId.STUDIO_REFERENCE,
    val eqBandsMb: List<Int> = EqPresetId.STUDIO_REFERENCE.curveMb,
    val bassBoostStrength: Int = 180,
    val virtualizerStrength: Int = 0,
    val eqEnabled: Boolean = true,
    val lastKnownLocation: LastKnownLocationRecord? = null
)

class UserPreferencesDataStore(private val context: Context) {

    private object Keys {
        val THEME = stringPreferencesKey("theme_preference")
        val WIDGET_ORDER = stringPreferencesKey("dashboard_widget_order")
        val HIDDEN_WIDGETS = stringPreferencesKey("hidden_dashboard_widgets")
        val BATTERY_ALERT = booleanPreferencesKey("notif_battery_alert")
        val BATTERY_THRESHOLD = intPreferencesKey("notif_battery_threshold")
        val CONNECTION_ALERT = booleanPreferencesKey("notif_connection_alert")
        val FIRMWARE_ALERT = booleanPreferencesKey("notif_firmware_alert")
        val LOCATION_ON_DISCONNECT = booleanPreferencesKey("location_on_disconnect")
        val DIAGNOSTICS_CONSENT = booleanPreferencesKey("diagnostics_consent")
        val EQ_PRESET = stringPreferencesKey("eq_preset")
        val EQ_BANDS_CSV = stringPreferencesKey("eq_bands_csv")
        val BASS_BOOST = intPreferencesKey("bass_boost_strength")
        val VIRTUALIZER = intPreferencesKey("virtualizer_strength")
        val EQ_ENABLED = booleanPreferencesKey("eq_enabled")
        val LOC_DEVICE_NAME = stringPreferencesKey("loc_device_name")
        val LOC_DEVICE_ADDR = stringPreferencesKey("loc_device_addr")
        val LOC_LAT = doublePreferencesKey("loc_lat")
        val LOC_LON = doublePreferencesKey("loc_lon")
        val LOC_ACC = floatPreferencesKey("loc_acc")
        val LOC_TIME = longPreferencesKey("loc_time")
    }

    val preferencesFlow: Flow<StoredAppPreferences> = context.soniqDataStore.data.map { prefs ->
        val theme = prefs[Keys.THEME]?.let { name ->
            ThemePreference.entries.firstOrNull { it.name == name }
        } ?: ThemePreference.DARK_OLED

        val order = prefs[Keys.WIDGET_ORDER]
            ?.split(",")
            ?.mapNotNull { name -> DashboardWidgetId.entries.firstOrNull { it.name == name } }
            ?.distinct()
            ?.takeIf { it.isNotEmpty() }
            ?: DashboardWidgetId.entries.toList()
        val missing = DashboardWidgetId.entries.filterNot { it in order }

        val hidden = prefs[Keys.HIDDEN_WIDGETS]
            ?.split(",")
            ?.mapNotNull { name -> DashboardWidgetId.entries.firstOrNull { it.name == name } }
            ?.toSet()
            ?: emptySet()

        val eqPreset = prefs[Keys.EQ_PRESET]?.let { name ->
            EqPresetId.entries.firstOrNull { it.name == name }
        } ?: EqPresetId.STUDIO_REFERENCE

        val eqBands = prefs[Keys.EQ_BANDS_CSV]
            ?.split(",")
            ?.mapNotNull { it.toIntOrNull() }
            ?.takeIf { it.size == 5 }
            ?: eqPreset.curveMb

        val location = if (prefs.contains(Keys.LOC_LAT) && prefs.contains(Keys.LOC_LON)) {
            LastKnownLocationRecord(
                deviceName = prefs[Keys.LOC_DEVICE_NAME] ?: "Bluetooth Earbuds",
                deviceAddress = prefs[Keys.LOC_DEVICE_ADDR] ?: "",
                latitude = prefs[Keys.LOC_LAT] ?: 0.0,
                longitude = prefs[Keys.LOC_LON] ?: 0.0,
                accuracyMeters = prefs[Keys.LOC_ACC] ?: 0f,
                recordedAtMs = prefs[Keys.LOC_TIME] ?: 0L
            )
        } else null

        StoredAppPreferences(
            themePreference = theme,
            dashboardWidgets = order + missing,
            hiddenDashboardWidgets = hidden,
            notificationSettings = NotificationSettings(
                batteryLowAlertEnabled = prefs[Keys.BATTERY_ALERT] ?: true,
                batteryLowThreshold = prefs[Keys.BATTERY_THRESHOLD] ?: 20,
                connectionAlertEnabled = prefs[Keys.CONNECTION_ALERT] ?: true,
                firmwareAlertEnabled = prefs[Keys.FIRMWARE_ALERT] ?: true,
                lastKnownLocationOnDisconnect = prefs[Keys.LOCATION_ON_DISCONNECT] ?: false
            ),
            diagnosticsConsentGranted = prefs[Keys.DIAGNOSTICS_CONSENT] ?: false,
            activeEqPreset = eqPreset,
            eqBandsMb = eqBands,
            bassBoostStrength = prefs[Keys.BASS_BOOST] ?: 180,
            virtualizerStrength = prefs[Keys.VIRTUALIZER] ?: 0,
            eqEnabled = prefs[Keys.EQ_ENABLED] ?: true,
            lastKnownLocation = location
        )
    }

    suspend fun setThemePreference(theme: ThemePreference) {
        context.soniqDataStore.edit { it[Keys.THEME] = theme.name }
    }

    suspend fun updateDashboardLayout(
        ordered: List<DashboardWidgetId>,
        hidden: Set<DashboardWidgetId>
    ) {
        context.soniqDataStore.edit {
            it[Keys.WIDGET_ORDER] = ordered.joinToString(",") { w -> w.name }
            it[Keys.HIDDEN_WIDGETS] = hidden.joinToString(",") { w -> w.name }
        }
    }

    suspend fun updateNotificationSettings(settings: NotificationSettings) {
        context.soniqDataStore.edit {
            it[Keys.BATTERY_ALERT] = settings.batteryLowAlertEnabled
            it[Keys.BATTERY_THRESHOLD] = settings.batteryLowThreshold
            it[Keys.CONNECTION_ALERT] = settings.connectionAlertEnabled
            it[Keys.FIRMWARE_ALERT] = settings.firmwareAlertEnabled
            it[Keys.LOCATION_ON_DISCONNECT] = settings.lastKnownLocationOnDisconnect
        }
    }

    suspend fun setDiagnosticsConsent(granted: Boolean) {
        context.soniqDataStore.edit { it[Keys.DIAGNOSTICS_CONSENT] = granted }
    }

    suspend fun saveAudioDspSettings(
        preset: EqPresetId,
        bandsMb: List<Int>,
        bassBoostStrength: Int,
        virtualizerStrength: Int,
        eqEnabled: Boolean
    ) {
        context.soniqDataStore.edit {
            it[Keys.EQ_PRESET] = preset.name
            it[Keys.EQ_BANDS_CSV] = bandsMb.joinToString(",")
            it[Keys.BASS_BOOST] = bassBoostStrength.coerceIn(0, 1000)
            it[Keys.VIRTUALIZER] = virtualizerStrength.coerceIn(0, 1000)
            it[Keys.EQ_ENABLED] = eqEnabled
        }
    }

    suspend fun saveLastKnownLocation(record: LastKnownLocationRecord) {
        context.soniqDataStore.edit {
            it[Keys.LOC_DEVICE_NAME] = record.deviceName
            it[Keys.LOC_DEVICE_ADDR] = record.deviceAddress
            it[Keys.LOC_LAT] = record.latitude
            it[Keys.LOC_LON] = record.longitude
            it[Keys.LOC_ACC] = record.accuracyMeters
            it[Keys.LOC_TIME] = record.recordedAtMs
        }
    }
}
