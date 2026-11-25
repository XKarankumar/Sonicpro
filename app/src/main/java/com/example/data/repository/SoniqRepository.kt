package com.example.data.repository

import android.content.Context
import android.os.Build
import com.example.BuildConfig
import com.example.data.bluetooth.AndroidBluetoothController
import com.example.data.bluetooth.AudioFxDspEngine
import com.example.data.bluetooth.BluetoothHardwareSnapshot
import com.example.data.bluetooth.FindEarbudsEngine
import com.example.data.bluetooth.HomeTheatreManager
import com.example.data.bluetooth.SoniqNotificationHelper
import com.example.data.local.BatteryHistoryEntity
import com.example.data.local.ConnectionEventEntity
import com.example.data.local.DeviceProfileEntity
import com.example.data.local.DiagnosticLogEntity
import com.example.data.local.SettingsBackupManager
import com.example.data.local.SoniqDao
import com.example.data.local.StoredAppPreferences
import com.example.data.local.UserPreferencesDataStore
import com.example.domain.integration.OemIntegrationRegistry
import com.example.domain.model.BatteryTelemetry
import com.example.domain.model.DashboardWidgetId
import com.example.domain.model.DiagnosticCheckResult
import com.example.domain.model.EarbudSide
import com.example.domain.model.EqPresetId
import com.example.domain.model.EqualizerState
import com.example.domain.model.LastKnownLocationRecord
import com.example.domain.model.NotificationSettings
import com.example.domain.model.SpatialAudioCapabilityState
import com.example.domain.model.TheatreEcosystemState
import com.example.domain.model.ThemePreference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SoniqRepository(
    private val context: Context,
    private val dao: SoniqDao,
    private val prefsDataStore: UserPreferencesDataStore,
    private val backupManager: SettingsBackupManager
) {
    private val repoScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val audioFxEngine = AudioFxDspEngine(context)
    private val homeTheatreManager = HomeTheatreManager(context, repoScope)
    private val findEngine = FindEarbudsEngine(context)
    private val notificationHelper = SoniqNotificationHelper(context)

    private var lastLowBatteryAlertAddress: String? = null

    val bluetoothController: AndroidBluetoothController = AndroidBluetoothController(
        context = context,
        scope = repoScope,
        onConnectionLifecycleEvent = { address, name, event, detail ->
            repoScope.launch {
                dao.insertConnectionEvent(
                    ConnectionEventEntity(
                        deviceAddress = address,
                        deviceName = name,
                        eventType = event,
                        detail = detail,
                        dataSourceMode = "LIVE_HARDWARE"
                    )
                )
                val prefs = prefsDataStore.preferencesFlow.first()
                if (event == "DISCONNECTED" && prefs.notificationSettings.lastKnownLocationOnDisconnect) {
                    findEngine.captureCoarseLastKnownLocation(name, address)?.let { rec ->
                        prefsDataStore.saveLastKnownLocation(rec)
                    }
                }
                if (event == "CONNECTION_UPDATE" && prefs.notificationSettings.connectionAlertEnabled) {
                    notificationHelper.postAlert(
                        notificationId = 1001,
                        title = "Audio Device: $name",
                        body = detail
                    )
                }
                refreshSpatializer()
                homeTheatreManager.refreshRoutesAndVolume()
            }
        },
        onBatteryObserved = { address, name, battery ->
            repoScope.launch {
                dao.insertBatteryHistory(
                    BatteryHistoryEntity(
                        deviceAddress = address,
                        deviceName = name,
                        leftPercent = battery.leftPercent,
                        rightPercent = battery.rightPercent,
                        casePercent = battery.casePercent,
                        singlePercent = battery.singlePercent,
                        dataSourceMode = "LIVE_HARDWARE"
                    )
                )
                val prefs = prefsDataStore.preferencesFlow.first()
                val lowest = listOfNotNull(
                    battery.leftPercent,
                    battery.rightPercent,
                    battery.singlePercent
                ).minOrNull()
                if (lowest != null &&
                    prefs.notificationSettings.batteryLowAlertEnabled &&
                    lowest <= prefs.notificationSettings.batteryLowThreshold &&
                    lastLowBatteryAlertAddress != address
                ) {
                    lastLowBatteryAlertAddress = address
                    notificationHelper.postAlert(
                        notificationId = 1002,
                        title = "Low Device Battery ($lowest%)",
                        body = "$name dropped below ${prefs.notificationSettings.batteryLowThreshold}%."
                    )
                } else if (lowest != null && lowest > prefs.notificationSettings.batteryLowThreshold + 5) {
                    lastLowBatteryAlertAddress = null
                }
            }
        },
        onErrorLogged = { category, message ->
            repoScope.launch {
                val prefs = prefsDataStore.preferencesFlow.first()
                if (prefs.diagnosticsConsentGranted) {
                    dao.insertDiagnosticLog(
                        DiagnosticLogEntity(
                            category = category,
                            message = message,
                            severity = "ERROR"
                        )
                    )
                }
            }
        }
    )

    private val _equalizerState = MutableStateFlow(EqualizerState())
    val equalizerState: StateFlow<EqualizerState> = _equalizerState.asStateFlow()

    private val _spatialState = MutableStateFlow(SpatialAudioCapabilityState())
    val spatialState: StateFlow<SpatialAudioCapabilityState> = _spatialState.asStateFlow()

    val theatreState: StateFlow<TheatreEcosystemState> = homeTheatreManager.state
    val bluetoothSnapshot: StateFlow<BluetoothHardwareSnapshot> = bluetoothController.snapshot
    val preferencesFlow: Flow<StoredAppPreferences> = prefsDataStore.preferencesFlow
    val batteryHistoryFlow: Flow<List<BatteryHistoryEntity>> = dao.observeBatteryHistory()
    val connectionEventsFlow: Flow<List<ConnectionEventEntity>> = dao.observeConnectionEvents()
    val deviceProfilesFlow: Flow<List<DeviceProfileEntity>> = dao.observeDeviceProfiles()
    val diagnosticLogsFlow: Flow<List<DiagnosticLogEntity>> = dao.observeDiagnosticLogs()

    fun startHardwareServices() {
        val initialEq = audioFxEngine.initialize()
        _equalizerState.value = initialEq
        val spatial = audioFxEngine.inspectSpatializer()
        _spatialState.value = spatial
        bluetoothController.updateAudioCapabilities(
            eqAvailable = initialEq.isEngineAvailable,
            bassAvailable = initialEq.bassBoostAvailable,
            vocalAvailable = initialEq.vocalEnhancerAvailable,
            spatial = spatial
        )
        bluetoothController.start()
        homeTheatreManager.refreshRoutesAndVolume()
        repoScope.launch {
            flushPendingCrashReport()
            val stored = prefsDataStore.preferencesFlow.first()
            applyStoredAudioDsp(stored)
        }
    }

    fun stopHardwareServices() {
        findEngine.stopLocatorChime()
        homeTheatreManager.stopNetworkDiscovery()
        bluetoothController.stop()
        audioFxEngine.release()
    }

    private suspend fun flushPendingCrashReport() {
        val crashPrefs = context.getSharedPreferences("soniq_crash_prefs", Context.MODE_PRIVATE)
        val summary = crashPrefs.getString("last_crash_summary", null) ?: return
        val consent = prefsDataStore.preferencesFlow.first().diagnosticsConsentGranted
        if (consent) {
            dao.insertDiagnosticLog(
                DiagnosticLogEntity(
                    category = "crash_recovery",
                    message = summary,
                    severity = "ERROR"
                )
            )
        }
        crashPrefs.edit().remove("last_crash_summary").remove("last_crash_time").apply()
    }

    private fun applyStoredAudioDsp(stored: StoredAppPreferences) {
        val currentBands = _equalizerState.value.bands
        val targetLevels = when {
            stored.eqBandsMb.isNotEmpty() -> stored.eqBandsMb
            else -> audioFxEngine.mapPresetToHardwareBands(stored.activeEqPreset, currentBands)
        }
        audioFxEngine.setEqualizerEnabled(stored.eqEnabled)
        audioFxEngine.applyBands(targetLevels)
        audioFxEngine.setBassBoostStrength(stored.bassBoostStrength)
        audioFxEngine.setVocalGainMb(stored.vocalGainMb)
        audioFxEngine.setVirtualizerStrength(stored.virtualizerStrength)
        _equalizerState.update { current ->
            val updatedBands = current.bands.mapIndexed { idx, band ->
                val target = targetLevels.getOrElse(idx) { band.levelMb }
                    .coerceIn(band.minMb, band.maxMb)
                band.copy(levelMb = target)
            }
            current.copy(
                isEnabled = stored.eqEnabled && current.isEngineAvailable,
                activePreset = stored.activeEqPreset,
                bands = updatedBands,
                bassBoostStrength = stored.bassBoostStrength,
                vocalGainMb = stored.vocalGainMb,
                virtualizerStrength = stored.virtualizerStrength
            )
        }
    }

    fun refreshSpatializer() {
        val spatial = audioFxEngine.inspectSpatializer()
        _spatialState.value = spatial
        val eq = _equalizerState.value
        bluetoothController.updateAudioCapabilities(
            eqAvailable = eq.isEngineAvailable,
            bassAvailable = eq.bassBoostAvailable,
            vocalAvailable = eq.vocalEnhancerAvailable,
            spatial = spatial
        )
    }

    suspend fun setEqualizerEnabled(enabled: Boolean) {
        audioFxEngine.setEqualizerEnabled(enabled)
        _equalizerState.update { it.copy(isEnabled = enabled) }
        persistCurrentDsp()
    }

    suspend fun selectEqPreset(preset: EqPresetId) {
        val currentBands = _equalizerState.value.bands
        val mapped = audioFxEngine.mapPresetToHardwareBands(preset, currentBands)
        audioFxEngine.applyBands(mapped)
        _equalizerState.update { state ->
            val bands = state.bands.mapIndexed { i, band ->
                band.copy(levelMb = mapped.getOrElse(i) { 0 }.coerceIn(band.minMb, band.maxMb))
            }
            state.copy(activePreset = preset, bands = bands)
        }
        persistCurrentDsp()
    }

    suspend fun updateEqBand(index: Int, levelMb: Int) {
        audioFxEngine.setBandLevel(index, levelMb)
        _equalizerState.update { state ->
            val bands = state.bands.map { band ->
                if (band.index == index) band.copy(levelMb = levelMb.coerceIn(band.minMb, band.maxMb))
                else band
            }
            state.copy(activePreset = EqPresetId.CUSTOM, bands = bands)
        }
        persistCurrentDsp()
    }

    suspend fun setBassBoostStrength(strength: Int) {
        val clamped = strength.coerceIn(0, 1000)
        audioFxEngine.setBassBoostStrength(clamped)
        _equalizerState.update { it.copy(bassBoostStrength = clamped) }
        persistCurrentDsp()
    }

    suspend fun setVocalGainMb(gainMb: Int) {
        val clamped = gainMb.coerceIn(0, 1500)
        audioFxEngine.setVocalGainMb(clamped)
        _equalizerState.update { it.copy(vocalGainMb = clamped) }
        persistCurrentDsp()
    }

    suspend fun setVirtualizerStrength(strength: Int) {
        val clamped = strength.coerceIn(0, 1000)
        audioFxEngine.setVirtualizerStrength(clamped)
        _equalizerState.update { it.copy(virtualizerStrength = clamped) }
        persistCurrentDsp()
    }

    private suspend fun persistCurrentDsp() {
        val eq = _equalizerState.value
        prefsDataStore.saveAudioDspSettings(
            preset = eq.activePreset,
            bandsMb = eq.bands.map { it.levelMb },
            bassBoostStrength = eq.bassBoostStrength,
            vocalGainMb = eq.vocalGainMb,
            virtualizerStrength = eq.virtualizerStrength,
            eqEnabled = eq.isEnabled
        )
    }

    // Home Theatre Ecosystem actions
    fun startTheatreScan() = homeTheatreManager.startNetworkDiscovery()
    fun stopTheatreScan() = homeTheatreManager.stopNetworkDiscovery()
    fun refreshTheatreRoutes() = homeTheatreManager.refreshRoutesAndVolume()
    fun setTheatreVolume(volume: Int) = homeTheatreManager.setStreamVolume(volume)
    fun toggleTheatreMute() = homeTheatreManager.toggleMute()
    fun dispatchMediaKey(keyCode: Int) = homeTheatreManager.dispatchMediaKey(keyCode)

    fun startFindChime(side: EarbudSide?, onStopped: () -> Unit) {
        findEngine.startLocatorChime(repoScope, side, onStopped)
    }

    fun stopFindChime() = findEngine.stopLocatorChime()

    suspend fun captureLastKnownLocationNow(deviceName: String, address: String): LastKnownLocationRecord? {
        val rec = findEngine.captureCoarseLastKnownLocation(deviceName, address) ?: return null
        prefsDataStore.saveLastKnownLocation(rec)
        return rec
    }

    fun triggerHaptic() = findEngine.vibrateTap()

    suspend fun setThemePreference(theme: ThemePreference) = prefsDataStore.setThemePreference(theme)

    suspend fun updateDashboardLayout(ordered: List<DashboardWidgetId>, hidden: Set<DashboardWidgetId>) =
        prefsDataStore.updateDashboardLayout(ordered, hidden)

    suspend fun updateNotificationSettings(settings: NotificationSettings) =
        prefsDataStore.updateNotificationSettings(settings)

    suspend fun setDiagnosticsConsent(granted: Boolean) =
        prefsDataStore.setDiagnosticsConsent(granted)

    suspend fun saveProfile(
        profileName: String,
        deviceName: String,
        deviceAddress: String
    ) {
        val eq = _equalizerState.value
        dao.insertDeviceProfile(
            DeviceProfileEntity(
                profileName = profileName.ifBlank { "Studio Profile" },
                deviceName = deviceName,
                deviceAddress = deviceAddress,
                eqPresetName = eq.activePreset.name,
                eqBandsCsv = eq.bands.joinToString(",") { it.levelMb.toString() },
                bassBoostStrength = eq.bassBoostStrength,
                vocalGainMb = eq.vocalGainMb,
                virtualizerStrength = eq.virtualizerStrength,
                gamingModePreferred = eq.activePreset == EqPresetId.GAMING_AUDIO,
                gesturesJson = "[]"
            )
        )
    }

    suspend fun applyProfile(profile: DeviceProfileEntity) {
        val preset = EqPresetId.entries.firstOrNull { it.name == profile.eqPresetName } ?: EqPresetId.CUSTOM
        val currentBands = _equalizerState.value.bands
        val savedBands = profile.eqBandsCsv.split(",").mapNotNull { it.trim().toIntOrNull() }
        val targetLevels = if (savedBands.size == currentBands.size) {
            savedBands
        } else {
            audioFxEngine.mapPresetToHardwareBands(preset, currentBands)
        }
        audioFxEngine.applyBands(targetLevels)
        audioFxEngine.setBassBoostStrength(profile.bassBoostStrength)
        audioFxEngine.setVocalGainMb(profile.vocalGainMb)
        audioFxEngine.setVirtualizerStrength(profile.virtualizerStrength)
        _equalizerState.update { state ->
            state.copy(
                activePreset = preset,
                bands = state.bands.mapIndexed { idx, b ->
                    b.copy(levelMb = targetLevels.getOrElse(idx) { 0 }.coerceIn(b.minMb, b.maxMb))
                },
                bassBoostStrength = profile.bassBoostStrength,
                vocalGainMb = profile.vocalGainMb,
                virtualizerStrength = profile.virtualizerStrength
            )
        }
        persistCurrentDsp()
    }

    suspend fun deleteProfile(id: Long) = dao.deleteDeviceProfile(id)

    suspend fun clearHistory() {
        dao.clearBatteryHistory()
        dao.clearConnectionEvents()
    }

    suspend fun exportBackupJson(): String {
        val prefs = prefsDataStore.preferencesFlow.first()
        val profiles = dao.getAllDeviceProfilesOnce()
        return backupManager.exportToJson(prefs, profiles)
    }

    suspend fun importBackupJson(rawJson: String): Result<Int> {
        val parsed = backupManager.importFromJson(rawJson).getOrElse { return Result.failure(it) }
        parsed.themePreference?.let { prefsDataStore.setThemePreference(it) }
        parsed.notificationSettings?.let { prefsDataStore.updateNotificationSettings(it) }
        if (parsed.hiddenWidgets != null) {
            val current = prefsDataStore.preferencesFlow.first()
            prefsDataStore.updateDashboardLayout(current.dashboardWidgets, parsed.hiddenWidgets)
        }
        if (parsed.eqBandsMb != null || parsed.activeEqPreset != null) {
            val current = prefsDataStore.preferencesFlow.first()
            prefsDataStore.saveAudioDspSettings(
                preset = parsed.activeEqPreset ?: current.activeEqPreset,
                bandsMb = parsed.eqBandsMb ?: current.eqBandsMb,
                bassBoostStrength = parsed.bassBoostStrength ?: current.bassBoostStrength,
                vocalGainMb = parsed.vocalGainMb ?: current.vocalGainMb,
                virtualizerStrength = parsed.virtualizerStrength ?: current.virtualizerStrength,
                eqEnabled = parsed.eqEnabled ?: current.eqEnabled
            )
            applyStoredAudioDsp(prefsDataStore.preferencesFlow.first())
        }
        parsed.profiles.forEach { dao.insertDeviceProfile(it) }
        return Result.success(parsed.profiles.size)
    }

    suspend fun runDiagnosticsSuite(): List<DiagnosticCheckResult> {
        val snap = bluetoothController.snapshot.value
        val eq = _equalizerState.value
        val spatial = _spatialState.value
        val theatre = homeTheatreManager.state.value
        val results = listOf(
            DiagnosticCheckResult(
                id = "bt_hw",
                title = "Bluetooth Hardware Adapter",
                passed = snap.isBluetoothSupported,
                detail = if (snap.isBluetoothSupported) "Bluetooth radio present" else "No Bluetooth adapter found"
            ),
            DiagnosticCheckResult(
                id = "bt_power",
                title = "Bluetooth Radio & Permissions",
                passed = snap.isBluetoothEnabled && snap.permissionState.hasAllRequiredBluetoothPermissions,
                detail = "Enabled=${snap.isBluetoothEnabled}, Scan=${snap.permissionState.hasScanPermission}, Connect=${snap.permissionState.hasConnectPermission}"
            ),
            DiagnosticCheckResult(
                id = "a2dp_proxy",
                title = "A2DP & HFP Audio Profiles",
                passed = snap.a2dpProxyReady || snap.headsetProxyReady,
                detail = "A2DP=${snap.a2dpProxyReady}, HFP=${snap.headsetProxyReady}"
            ),
            DiagnosticCheckResult(
                id = "audio_fx",
                title = "Android AudioFX Studio DSP",
                passed = eq.isEngineAvailable,
                detail = "${eq.statusMessage} (${eq.hardwareBandCount} bands, Bass=${eq.bassBoostAvailable}, Vocal=${eq.vocalEnhancerAvailable})"
            ),
            DiagnosticCheckResult(
                id = "spatializer",
                title = "Android Spatializer 3D HAL",
                passed = spatial.isAvailable,
                detail = spatial.explanation
            ),
            DiagnosticCheckResult(
                id = "home_theatre",
                title = "Home Theatre Wi-Fi / Output Routes",
                passed = theatre.isWifiConnected || theatre.activeOutputRoutes.isNotEmpty(),
                detail = "Wi-Fi=${theatre.isWifiConnected}, Active Outputs=${theatre.activeOutputRoutes.size}"
            )
        )
        val prefs = prefsDataStore.preferencesFlow.first()
        if (prefs.diagnosticsConsentGranted) {
            results.forEach { check ->
                dao.insertDiagnosticLog(
                    DiagnosticLogEntity(
                        category = check.id,
                        message = "${check.title}: ${check.detail}",
                        severity = if (check.passed) "INFO" else "WARN"
                    )
                )
            }
        }
        return results
    }

    suspend fun buildCompatibilityReport(): String {
        val snap = bluetoothController.snapshot.value
        val eq = _equalizerState.value
        val spatial = _spatialState.value
        val theatre = homeTheatreManager.state.value
        return buildString {
            appendLine("=== SONIQ Compatibility Report ===")
            appendLine("App Version: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
            appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL} (Android ${Build.VERSION.RELEASE} / API ${Build.VERSION.SDK_INT})")
            appendLine("Bluetooth Supported: ${snap.isBluetoothSupported}, Enabled: ${snap.isBluetoothEnabled}")
            appendLine("A2DP Ready: ${snap.a2dpProxyReady}, HFP Ready: ${snap.headsetProxyReady}")
            appendLine("Active Connected Device: ${snap.activeLiveDevice?.name ?: "None"} (${snap.activeLiveDevice?.brand?.displayName ?: "-"})")
            appendLine("AudioFX EQ Bands: ${eq.hardwareBandCount} (EngineAvailable=${eq.isEngineAvailable})")
            appendLine("Spatializer: Available=${spatial.isAvailable}, Enabled=${spatial.isEnabled}, HeadTracker=${spatial.isHeadTrackerAvailable}")
            appendLine("Home Theatre Routes: ${theatre.activeOutputRoutes.joinToString { "${it.productName} [${it.typeLabel}]" }}")
            appendLine("\nOEM Integrations:")
            OemIntegrationRegistry.allIntegrations.forEach { oem ->
                appendLine("- ${oem.info.displayName}: ${oem.info.sdkStatus}")
            }
        }
    }

    suspend fun clearDiagnosticLogs() = dao.clearDiagnosticLogs()
}
