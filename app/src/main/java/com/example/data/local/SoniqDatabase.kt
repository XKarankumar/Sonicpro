package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        BatteryHistoryEntity::class,
        ConnectionEventEntity::class,
        DeviceProfileEntity::class,
        DiagnosticLogEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class SoniqDatabase : RoomDatabase() {
    abstract fun soniqDao(): SoniqDao

    companion object {
        @Volatile
        private var INSTANCE: SoniqDatabase? = null

        fun getInstance(context: Context): SoniqDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    SoniqDatabase::class.java,
                    "soniq_pro_db"
                ).fallbackToDestructiveMigration(dropAllTables = false)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
