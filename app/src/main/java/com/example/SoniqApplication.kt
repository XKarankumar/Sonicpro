package com.example

import android.app.Application
import android.content.Context
import com.example.data.local.SettingsBackupManager
import com.example.data.local.SoniqDatabase
import com.example.data.local.UserPreferencesDataStore
import com.example.data.repository.SoniqRepository

class SoniqApplication : Application() {
    val database: SoniqDatabase by lazy { SoniqDatabase.getInstance(this) }
    val preferencesDataStore: UserPreferencesDataStore by lazy { UserPreferencesDataStore(this) }
    val backupManager: SettingsBackupManager by lazy { SettingsBackupManager() }

    val repository: SoniqRepository by lazy {
        SoniqRepository(
            context = this,
            dao = database.soniqDao(),
            prefsDataStore = preferencesDataStore,
            backupManager = backupManager
        )
    }

    override fun onCreate() {
        super.onCreate()
        installLocalCrashLogger()
    }

    private fun installLocalCrashLogger() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching {
                val prefs = getSharedPreferences("soniq_crash_prefs", Context.MODE_PRIVATE)
                val summary = "${throwable.javaClass.simpleName}: ${throwable.message ?: "Unknown"} on ${thread.name}"
                prefs.edit()
                    .putString("last_crash_summary", summary)
                    .putLong("last_crash_time", System.currentTimeMillis())
                    .commit()
            }
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }
}
