package com.example.data.local

import com.example.BuildConfig
import com.example.domain.model.DashboardWidgetId
import com.example.domain.model.EqPresetId
import com.example.domain.model.NotificationSettings
import com.example.domain.model.ThemePreference
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

data class RestoredBackupResult(
    val themePreference: ThemePreference?,
    val activeEqPreset: EqPresetId?,
    val eqBandsMb: List<Int>?,
    val bassBoostStrength: Int?,
    val vocalGainMb: Int?,
    val virtualizerStrength: Int?,
    val eqEnabled: Boolean?,
    val notificationSettings: NotificationSettings?,
    val hiddenWidgets: Set<DashboardWidgetId>?,
    val profiles: List<DeviceProfileEntity>
)

@JsonClass(generateAdapter = true)
data class SoniqBackupPayload(
    val schemaVersion: Int = SettingsBackupManager.SCHEMA_VERSION,
    val appVersion: String = BuildConfig.VERSION_NAME,
    val exportedAtMs: Long = System.currentTimeMillis(),
    val themePreference: String? = null,
    val activeEqPreset: String? = null,
    val eqBandsCsv: String? = null,
    val bassBoostStrength: Int? = null,
    val vocalGainMb: Int? = null,
    val virtualizerStrength: Int? = null,
    val eqEnabled: Boolean? = null,
    val hiddenWidgets: List<String> = emptyList(),
    val notifications: BackupNotificationPayload? = null,
    val deviceProfiles: List<BackupProfilePayload> = emptyList()
)

@JsonClass(generateAdapter = true)
data class BackupNotificationPayload(
    val batteryLowAlertEnabled: Boolean = true,
    val batteryLowThreshold: Int = 20,
    val connectionAlertEnabled: Boolean = true,
    val firmwareAlertEnabled: Boolean = true,
    val lastKnownLocationOnDisconnect: Boolean = false
)

@JsonClass(generateAdapter = true)
data class BackupProfilePayload(
    val profileName: String = "Restored Profile",
    val deviceName: String = "Bluetooth Audio Device",
    val deviceAddress: String = "",
    val eqPresetName: String = EqPresetId.STUDIO_REFERENCE.name,
    val eqBandsCsv: String = "0,0,0,0,0",
    val bassBoostStrength: Int = 0,
    val vocalGainMb: Int? = null,
    val virtualizerStrength: Int = 0,
    val gamingModePreferred: Boolean = false,
    val gesturesJson: String = "[]"
)

/**
 * Versioned JSON backup & restore serializer with backward compatibility (schema v1).
 */
class SettingsBackupManager {
    companion object {
        const val SCHEMA_VERSION = 1
    }

    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()
    private val adapter = moshi.adapter(SoniqBackupPayload::class.java).indent("  ")

    fun exportToJson(
        prefs: StoredAppPreferences,
        profiles: List<DeviceProfileEntity>
    ): String {
        val payload = SoniqBackupPayload(
            schemaVersion = SCHEMA_VERSION,
            appVersion = BuildConfig.VERSION_NAME,
            exportedAtMs = System.currentTimeMillis(),
            themePreference = prefs.themePreference.name,
            activeEqPreset = prefs.activeEqPreset.name,
            eqBandsCsv = prefs.eqBandsMb.joinToString(","),
            bassBoostStrength = prefs.bassBoostStrength,
            vocalGainMb = prefs.vocalGainMb,
            virtualizerStrength = prefs.virtualizerStrength,
            eqEnabled = prefs.eqEnabled,
            hiddenWidgets = prefs.hiddenDashboardWidgets.map { it.name },
            notifications = BackupNotificationPayload(
                batteryLowAlertEnabled = prefs.notificationSettings.batteryLowAlertEnabled,
                batteryLowThreshold = prefs.notificationSettings.batteryLowThreshold,
                connectionAlertEnabled = prefs.notificationSettings.connectionAlertEnabled,
                firmwareAlertEnabled = prefs.notificationSettings.firmwareAlertEnabled,
                lastKnownLocationOnDisconnect = prefs.notificationSettings.lastKnownLocationOnDisconnect
            ),
            deviceProfiles = profiles.map { p ->
                BackupProfilePayload(
                    profileName = p.profileName,
                    deviceName = p.deviceName,
                    deviceAddress = p.deviceAddress,
                    eqPresetName = p.eqPresetName,
                    eqBandsCsv = p.eqBandsCsv,
                    bassBoostStrength = p.bassBoostStrength,
                    vocalGainMb = p.vocalGainMb,
                    virtualizerStrength = p.virtualizerStrength,
                    gamingModePreferred = p.gamingModePreferred,
                    gesturesJson = p.gesturesJson
                )
            }
        )
        return adapter.toJson(payload)
    }

    fun importFromJson(rawJson: String): Result<RestoredBackupResult> = runCatching {
        val payload = adapter.fromJson(rawJson) ?: error("Empty or malformed JSON backup")
        require(payload.schemaVersion in 1..SCHEMA_VERSION) {
            "Unsupported backup schema version: ${payload.schemaVersion}"
        }

        val theme = payload.themePreference?.let { name ->
            ThemePreference.entries.firstOrNull { it.name == name }
        }
        val preset = payload.activeEqPreset?.let { name ->
            EqPresetId.entries.firstOrNull { it.name == name }
        }
        val bands = payload.eqBandsCsv
            ?.split(",")
            ?.mapNotNull { it.trim().toIntOrNull() }
            ?.takeIf { it.size in 1..10 }

        val hidden = payload.hiddenWidgets.mapNotNull { name ->
            DashboardWidgetId.entries.firstOrNull { it.name == name }
        }.toSet()

        val notif = payload.notifications?.let { obj ->
            NotificationSettings(
                batteryLowAlertEnabled = obj.batteryLowAlertEnabled,
                batteryLowThreshold = obj.batteryLowThreshold,
                connectionAlertEnabled = obj.connectionAlertEnabled,
                firmwareAlertEnabled = obj.firmwareAlertEnabled,
                lastKnownLocationOnDisconnect = obj.lastKnownLocationOnDisconnect
            )
        }

        val profiles = payload.deviceProfiles.map { item ->
            DeviceProfileEntity(
                profileName = item.profileName,
                deviceName = item.deviceName,
                deviceAddress = item.deviceAddress,
                eqPresetName = item.eqPresetName,
                eqBandsCsv = item.eqBandsCsv,
                bassBoostStrength = item.bassBoostStrength,
                vocalGainMb = item.vocalGainMb ?: 0,
                virtualizerStrength = item.virtualizerStrength,
                gamingModePreferred = item.gamingModePreferred,
                gesturesJson = item.gesturesJson
            )
        }

        RestoredBackupResult(
            themePreference = theme,
            activeEqPreset = preset,
            eqBandsMb = bands,
            bassBoostStrength = payload.bassBoostStrength,
            vocalGainMb = payload.vocalGainMb,
            virtualizerStrength = payload.virtualizerStrength,
            eqEnabled = payload.eqEnabled,
            notificationSettings = notif,
            hiddenWidgets = hidden,
            profiles = profiles
        )
    }
}
