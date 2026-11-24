package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "battery_history")
data class BatteryHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val deviceAddress: String,
    val deviceName: String,
    val leftPercent: Int?,
    val rightPercent: Int?,
    val casePercent: Int?,
    val singlePercent: Int?,
    val dataSourceMode: String,
    val timestampMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "connection_events")
data class ConnectionEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val deviceAddress: String,
    val deviceName: String,
    val eventType: String, // CONNECTED, DISCONNECTED, BONDED, CODEC_SWITCH
    val detail: String,
    val dataSourceMode: String,
    val timestampMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "device_profiles")
data class DeviceProfileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val profileName: String,
    val deviceName: String,
    val deviceAddress: String,
    val eqPresetName: String,
    val eqBandsCsv: String,
    val bassBoostStrength: Int,
    val virtualizerStrength: Int,
    val gamingModePreferred: Boolean,
    val gesturesJson: String,
    val updatedAtMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "diagnostic_logs")
data class DiagnosticLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val category: String,
    val message: String,
    val severity: String, // INFO, WARN, ERROR
    val timestampMs: Long = System.currentTimeMillis()
)
