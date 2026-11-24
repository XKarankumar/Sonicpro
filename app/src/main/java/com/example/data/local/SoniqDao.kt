package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SoniqDao {
    @Query("SELECT * FROM battery_history ORDER BY timestampMs DESC LIMIT 50")
    fun observeBatteryHistory(): Flow<List<BatteryHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBatteryHistory(entry: BatteryHistoryEntity)

    @Query("DELETE FROM battery_history")
    suspend fun clearBatteryHistory()

    @Query("SELECT * FROM connection_events ORDER BY timestampMs DESC LIMIT 60")
    fun observeConnectionEvents(): Flow<List<ConnectionEventEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConnectionEvent(event: ConnectionEventEntity)

    @Query("DELETE FROM connection_events")
    suspend fun clearConnectionEvents()

    @Query("SELECT * FROM device_profiles ORDER BY updatedAtMs DESC")
    fun observeDeviceProfiles(): Flow<List<DeviceProfileEntity>>

    @Query("SELECT * FROM device_profiles ORDER BY updatedAtMs DESC")
    suspend fun getAllDeviceProfilesOnce(): List<DeviceProfileEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeviceProfile(profile: DeviceProfileEntity): Long

    @Query("DELETE FROM device_profiles WHERE id = :id")
    suspend fun deleteDeviceProfile(id: Long)

    @Query("SELECT * FROM diagnostic_logs ORDER BY timestampMs DESC LIMIT 100")
    fun observeDiagnosticLogs(): Flow<List<DiagnosticLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDiagnosticLog(log: DiagnosticLogEntity)

    @Query("DELETE FROM diagnostic_logs")
    suspend fun clearDiagnosticLogs()
}
