package com.example

import android.app.Application
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
}
