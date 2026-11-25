package com.example.ui.viewmodel

import android.view.KeyEvent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.bluetooth.BluetoothHardwareSnapshot
import com.example.data.bluetooth.BluetoothPermissionState
import com.example.data.local.BatteryHistoryEntity
import com.example.data.local.ConnectionEventEntity
import com.example.data.local.DeviceProfileEntity
import com.example.data.local.DiagnosticLogEntity
import com.example.data.local.StoredAppPreferences
import com.example.data.repository.ReleaseNotesCatalog
import com.example.data.repository.SoniqRepository
import com.example.domain.integration.OemDeviceIntegration
import com.example.domain.integration.OemIntegrationRegistry
import com.example.domain.model.AppReleaseNote
import com.example.domain.model.DashboardWidgetId
import com.example.domain.model.DiagnosticCheckResult
import com.example.domain.model.EarbudSide
import com.example.domain.model.EarbudsDevice
import com.example.domain.model.EqPresetId
import com.example.domain.model.EqualizerState
import com.example.domain.model.NotificationSettings
import com.example.domain.model.SpatialAudioCapabilityState
import com.example.domain.model.TheatreEcosystemState
import com.example.domain.model.ThemePreference
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SoniqUiState(
    val activeDevice: EarbudsDevice? = null,
    val inspectedDevice: EarbudsDevice? = null,
    val bluetoothSnapshot: BluetoothHardwareSnapshot = BluetoothHardwareSnapshot(
        permissionState = BluetoothPermissionState(
            hasScanPermission = false,
            hasConnectPermission = false,
            hasLocationPermission = false,
            hasNotificationPermission = false
        )
    ),
    val equalizerState: EqualizerState = EqualizerState(),
    val spatialState: SpatialAudioCapabilityState = SpatialAudioCapabilityState(),
    val theatreState: TheatreEcosystemState = TheatreEcosystemState(),
    val preferences: StoredAppPreferences = StoredAppPreferences(),
    val oemIntegrations: List<OemDeviceIntegration> = OemIntegrationRegistry.allIntegrations,
    val batteryHistory: List<BatteryHistoryEntity> = emptyList(),
    val connectionEvents: List<ConnectionEventEntity> = emptyList(),
    val savedProfiles: List<DeviceProfileEntity> = emptyList(),
    val diagnosticLogs: List<DiagnosticLogEntity> = emptyList(),
    val diagnosticResults: List<DiagnosticCheckResult> = emptyList(),
    val releaseNotes: List<AppReleaseNote> = ReleaseNotesCatalog.releases,
    val isFindChimePlaying: Boolean = false,
    val findChimeTarget: EarbudSide? = null,
    val isRunningDiagnostics: Boolean = false
)

sealed interface SoniqUiEvent {
    data class ShowMessage(val message: String) : SoniqUiEvent
    data object OpenSystemBluetoothSettings : SoniqUiEvent
}

class SoniqViewModel(private val repository: SoniqRepository) : ViewModel() {

    private val selectedInspectAddress = MutableStateFlow<String?>(null)
    private val diagnosticResultsState = MutableStateFlow<List<DiagnosticCheckResult>>(emptyList())
    private val findState = MutableStateFlow<Pair<Boolean, EarbudSide?>>(false to null)
    private val runningDiagnostics = MutableStateFlow(false)

    private val _events = MutableSharedFlow<SoniqUiEvent>()
    val events: SharedFlow<SoniqUiEvent> = _events.asSharedFlow()

    private val auxState = combine(
        selectedInspectAddress,
        diagnosticResultsState,
        findState,
        runningDiagnostics
    ) { inspectAddr, diagResults, find, isRunningDiag ->
        AuxHolder(inspectAddr, diagResults, find.first, find.second, isRunningDiag)
    }

    private val historyHolder = combine(
        repository.batteryHistoryFlow,
        repository.connectionEventsFlow,
        repository.deviceProfilesFlow,
        repository.diagnosticLogsFlow
    ) { bat, conn, prof, logs ->
        HistoryHolder(bat, conn, prof, logs)
    }

    private val audioEnvHolder = combine(
        repository.equalizerState,
        repository.spatialState,
        repository.theatreState
    ) { eq, spatial, theatre ->
        AudioEnvHolder(eq, spatial, theatre)
    }

    val uiState: StateFlow<SoniqUiState> = combine(
        repository.bluetoothSnapshot,
        audioEnvHolder,
        repository.preferencesFlow,
        auxState,
        historyHolder
    ) { bt, audio, prefs, aux, hist ->
        val allKnown = bt.bondedAudioDevices + bt.discoveredDevices
        val inspected = aux.inspectAddress?.let { addr ->
            allKnown.firstOrNull { it.address == addr }
        } ?: bt.activeLiveDevice ?: bt.bondedAudioDevices.firstOrNull()

        SoniqUiState(
            activeDevice = bt.activeLiveDevice,
            inspectedDevice = inspected,
            bluetoothSnapshot = bt,
            equalizerState = audio.eq,
            spatialState = audio.spatial,
            theatreState = audio.theatre,
            preferences = prefs,
            oemIntegrations = OemIntegrationRegistry.allIntegrations,
            batteryHistory = hist.batteryHistory,
            connectionEvents = hist.connectionEvents,
            savedProfiles = hist.profiles,
            diagnosticLogs = hist.logs,
            diagnosticResults = aux.diagResults,
            releaseNotes = ReleaseNotesCatalog.releases,
            isFindChimePlaying = aux.isFindPlaying,
            findChimeTarget = aux.findSide,
            isRunningDiagnostics = aux.isRunningDiag
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SoniqUiState()
    )

    private data class AudioEnvHolder(
        val eq: EqualizerState,
        val spatial: SpatialAudioCapabilityState,
        val theatre: TheatreEcosystemState
    )

    private data class AuxHolder(
        val inspectAddress: String?,
        val diagResults: List<DiagnosticCheckResult>,
        val isFindPlaying: Boolean,
        val findSide: EarbudSide?,
        val isRunningDiag: Boolean
    )

    private data class HistoryHolder(
        val batteryHistory: List<BatteryHistoryEntity>,
        val connectionEvents: List<ConnectionEventEntity>,
        val profiles: List<DeviceProfileEntity>,
        val logs: List<DiagnosticLogEntity>
    )

    fun onPermissionsChanged() {
        repository.bluetoothController.refreshPermissionState()
    }

    fun selectInspectedDevice(address: String) {
        repository.triggerHaptic()
        selectedInspectAddress.value = address
    }

    fun startScan() {
        repository.triggerHaptic()
        repository.bluetoothController.startScan()
    }

    fun stopScan() {
        repository.bluetoothController.stopScan()
    }

    fun refreshLiveHardware() {
        repository.triggerHaptic()
        repository.bluetoothController.refreshLiveTelemetryNow()
        repository.refreshSpatializer()
        repository.refreshTheatreRoutes()
    }

    fun pairDevice(address: String) {
        repository.triggerHaptic()
        val ok = repository.bluetoothController.pairDevice(address)
        viewModelScope.launch {
            if (ok) {
                _events.emit(SoniqUiEvent.ShowMessage("Bluetooth pairing requested"))
            } else {
                _events.emit(SoniqUiEvent.OpenSystemBluetoothSettings)
            }
        }
    }

    fun connectDevice(address: String) {
        repository.triggerHaptic()
        val ok = repository.bluetoothController.connectDevice(address)
        viewModelScope.launch {
            if (ok) {
                _events.emit(SoniqUiEvent.ShowMessage("Connecting audio profile..."))
            } else {
                _events.emit(SoniqUiEvent.ShowMessage("Opening Android Bluetooth settings to complete connection"))
                _events.emit(SoniqUiEvent.OpenSystemBluetoothSettings)
            }
        }
    }

    fun disconnectDevice(address: String) {
        repository.triggerHaptic()
        val ok = repository.bluetoothController.disconnectDevice(address)
        viewModelScope.launch {
            if (!ok) {
                _events.emit(SoniqUiEvent.OpenSystemBluetoothSettings)
            }
        }
    }

    // Studio AudioFX DSP & Spatializer
    fun toggleEqualizerEnabled(enabled: Boolean) {
        repository.triggerHaptic()
        viewModelScope.launch { repository.setEqualizerEnabled(enabled) }
    }

    fun selectEqPreset(preset: EqPresetId) {
        repository.triggerHaptic()
        viewModelScope.launch { repository.selectEqPreset(preset) }
    }

    fun updateEqBand(index: Int, levelMb: Int) {
        viewModelScope.launch { repository.updateEqBand(index, levelMb) }
    }

    fun updateBassBoost(strength: Int) {
        viewModelScope.launch { repository.setBassBoostStrength(strength) }
    }

    fun updateVocalGain(gainMb: Int) {
        viewModelScope.launch { repository.setVocalGainMb(gainMb) }
    }

    fun updateVirtualizer(strength: Int) {
        viewModelScope.launch { repository.setVirtualizerStrength(strength) }
    }

    fun refreshSpatializer() {
        repository.triggerHaptic()
        repository.refreshSpatializer()
    }

    // Home Theatre Ecosystem
    fun startTheatreScan() {
        repository.triggerHaptic()
        repository.startTheatreScan()
    }

    fun stopTheatreScan() {
        repository.stopTheatreScan()
    }

    fun refreshTheatreRoutes() {
        repository.refreshTheatreRoutes()
    }

    fun setTheatreVolume(volume: Int) {
        repository.setTheatreVolume(volume)
    }

    fun toggleTheatreMute() {
        repository.triggerHaptic()
        repository.toggleTheatreMute()
    }

    fun dispatchMediaPlayPause() {
        repository.triggerHaptic()
        repository.dispatchMediaKey(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
    }

    fun dispatchMediaNext() {
        repository.triggerHaptic()
        repository.dispatchMediaKey(KeyEvent.KEYCODE_MEDIA_NEXT)
    }

    fun dispatchMediaPrevious() {
        repository.triggerHaptic()
        repository.dispatchMediaKey(KeyEvent.KEYCODE_MEDIA_PREVIOUS)
    }

    // Find Device & Location
    fun toggleFindChime(side: EarbudSide?) {
        repository.triggerHaptic()
        val (currentlyPlaying, currentSide) = findState.value
        if (currentlyPlaying && currentSide == side) {
            repository.stopFindChime()
            findState.value = false to null
        } else {
            findState.value = true to side
            repository.startFindChime(side) {
                findState.value = false to null
            }
        }
    }

    fun stopFindChime() {
        repository.stopFindChime()
        findState.value = false to null
    }

    fun captureLastKnownLocation() {
        val dev = uiState.value.activeDevice
        val name = dev?.name ?: "Last Connected Audio Device"
        val addr = dev?.address ?: "UNKNOWN"
        viewModelScope.launch {
            val saved = repository.captureLastKnownLocationNow(name, addr)
            if (saved != null) {
                _events.emit(SoniqUiEvent.ShowMessage("Coarse last-known location recorded"))
            } else {
                _events.emit(SoniqUiEvent.ShowMessage("Location permission or provider unavailable"))
            }
        }
    }

    // Settings, Profiles, Backup & Diagnostics
    fun setThemePreference(theme: ThemePreference) {
        repository.triggerHaptic()
        viewModelScope.launch { repository.setThemePreference(theme) }
    }

    fun toggleDashboardWidgetVisibility(widgetId: DashboardWidgetId) {
        repository.triggerHaptic()
        viewModelScope.launch {
            val current = uiState.value.preferences
            val nextHidden = current.hiddenDashboardWidgets.toMutableSet()
            if (widgetId in nextHidden) nextHidden.remove(widgetId) else nextHidden.add(widgetId)
            repository.updateDashboardLayout(current.dashboardWidgets, nextHidden)
        }
    }

    fun moveDashboardWidgetUp(widgetId: DashboardWidgetId) {
        repository.triggerHaptic()
        viewModelScope.launch {
            val current = uiState.value.preferences
            val list = current.dashboardWidgets.toMutableList()
            val idx = list.indexOf(widgetId)
            if (idx > 0) {
                val prev = list[idx - 1]
                list[idx - 1] = widgetId
                list[idx] = prev
                repository.updateDashboardLayout(list, current.hiddenDashboardWidgets)
            }
        }
    }

    fun updateNotificationSettings(settings: NotificationSettings) {
        repository.triggerHaptic()
        viewModelScope.launch { repository.updateNotificationSettings(settings) }
    }

    fun setDiagnosticsConsent(granted: Boolean) {
        repository.triggerHaptic()
        viewModelScope.launch { repository.setDiagnosticsConsent(granted) }
    }

    fun runDiagnostics() {
        repository.triggerHaptic()
        viewModelScope.launch {
            runningDiagnostics.value = true
            diagnosticResultsState.value = repository.runDiagnosticsSuite()
            runningDiagnostics.value = false
        }
    }

    fun clearDiagnosticLogs() {
        viewModelScope.launch { repository.clearDiagnosticLogs() }
    }

    fun saveCurrentAsProfile(profileName: String) {
        repository.triggerHaptic()
        val active = uiState.value.activeDevice
        viewModelScope.launch {
            repository.saveProfile(
                profileName = profileName,
                deviceName = active?.name ?: "Universal Audio Profile",
                deviceAddress = active?.address ?: ""
            )
            _events.emit(SoniqUiEvent.ShowMessage("Saved profile \"$profileName\""))
        }
    }

    fun applyProfile(profile: DeviceProfileEntity) {
        repository.triggerHaptic()
        viewModelScope.launch {
            repository.applyProfile(profile)
            _events.emit(SoniqUiEvent.ShowMessage("Applied profile \"${profile.profileName}\""))
        }
    }

    fun deleteProfile(id: Long) {
        viewModelScope.launch { repository.deleteProfile(id) }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearHistory()
            _events.emit(SoniqUiEvent.ShowMessage("Cleared battery & connection history"))
        }
    }

    suspend fun generateBackupJson(): String = repository.exportBackupJson()

    suspend fun generateCompatibilityReport(): String = repository.buildCompatibilityReport()

    fun restoreBackupJson(rawJson: String) {
        viewModelScope.launch {
            repository.importBackupJson(rawJson)
                .onSuccess { count ->
                    _events.emit(SoniqUiEvent.ShowMessage("Backup restored ($count device profile(s))"))
                }
                .onFailure { err ->
                    _events.emit(SoniqUiEvent.ShowMessage("Invalid backup file: ${err.message}"))
                }
        }
    }

    fun showMessage(message: String) {
        viewModelScope.launch { _events.emit(SoniqUiEvent.ShowMessage(message)) }
    }

    class Factory(private val repository: SoniqRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SoniqViewModel(repository) as T
        }
    }
}
