package com.example.ui.viewmodel

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
import com.example.domain.model.AncMode
import com.example.domain.model.AppReleaseNote
import com.example.domain.model.BluetoothConnectionState
import com.example.domain.model.DashboardWidgetId
import com.example.domain.model.DataSourceMode
import com.example.domain.model.DiagnosticCheckResult
import com.example.domain.model.EarbudSide
import com.example.domain.model.EarbudsDevice
import com.example.domain.model.EqPresetId
import com.example.domain.model.EqualizerState
import com.example.domain.model.GestureMapping
import com.example.domain.model.NotificationSettings
import com.example.domain.model.SpatialAudioMode
import com.example.domain.model.ThemePreference
import com.example.domain.model.TouchAction
import com.example.domain.model.TouchTrigger
import com.example.domain.protocol.DemoPreviewFactory
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

data class SoniqUiState(
    val activeDevice: EarbudsDevice? = null,
    val isDemoPreviewActive: Boolean = false,
    val bluetoothSnapshot: BluetoothHardwareSnapshot = BluetoothHardwareSnapshot(
        permissionState = BluetoothPermissionState(
            hasScanPermission = false,
            hasConnectPermission = false,
            hasLocationPermission = false,
            hasNotificationPermission = false
        )
    ),
    val equalizerState: EqualizerState = EqualizerState(),
    val preferences: StoredAppPreferences = StoredAppPreferences(),
    val gestureMappings: List<GestureMapping> = defaultGestures(),
    val batteryHistory: List<BatteryHistoryEntity> = emptyList(),
    val connectionEvents: List<ConnectionEventEntity> = emptyList(),
    val savedProfiles: List<DeviceProfileEntity> = emptyList(),
    val diagnosticLogs: List<DiagnosticLogEntity> = emptyList(),
    val diagnosticResults: List<DiagnosticCheckResult> = emptyList(),
    val releaseNotes: List<AppReleaseNote> = ReleaseNotesCatalog.releases,
    val isFindChimePlaying: Boolean = false,
    val findChimeTarget: EarbudSide? = null,
    val isRunningDiagnostics: Boolean = false
) {
    companion object {
        fun defaultGestures(): List<GestureMapping> = listOf(
            GestureMapping(EarbudSide.LEFT, TouchTrigger.SINGLE_TAP, TouchAction.PLAY_PAUSE),
            GestureMapping(EarbudSide.LEFT, TouchTrigger.DOUBLE_TAP, TouchAction.PREVIOUS_TRACK),
            GestureMapping(EarbudSide.LEFT, TouchTrigger.TRIPLE_TAP, TouchAction.VOICE_ASSISTANT),
            GestureMapping(EarbudSide.LEFT, TouchTrigger.TOUCH_AND_HOLD, TouchAction.CYCLE_ANC),
            GestureMapping(EarbudSide.RIGHT, TouchTrigger.SINGLE_TAP, TouchAction.PLAY_PAUSE),
            GestureMapping(EarbudSide.RIGHT, TouchTrigger.DOUBLE_TAP, TouchAction.NEXT_TRACK),
            GestureMapping(EarbudSide.RIGHT, TouchTrigger.TRIPLE_TAP, TouchAction.GAMING_MODE),
            GestureMapping(EarbudSide.RIGHT, TouchTrigger.TOUCH_AND_HOLD, TouchAction.CYCLE_ANC)
        )
    }
}

sealed interface SoniqUiEvent {
    data class ShowMessage(val message: String) : SoniqUiEvent
    data object OpenSystemBluetoothSettings : SoniqUiEvent
}

class SoniqViewModel(private val repository: SoniqRepository) : ViewModel() {

    private val demoDeviceState = MutableStateFlow<EarbudsDevice?>(null)
    private val gestureMappingsState = MutableStateFlow(SoniqUiState.defaultGestures())
    private val diagnosticResultsState = MutableStateFlow<List<DiagnosticCheckResult>>(emptyList())
    private val findState = MutableStateFlow<Pair<Boolean, EarbudSide?>>(false to null)
    private val runningDiagnostics = MutableStateFlow(false)

    private val _events = MutableSharedFlow<SoniqUiEvent>()
    val events: SharedFlow<SoniqUiEvent> = _events.asSharedFlow()

    private val auxState = combine(
        demoDeviceState,
        gestureMappingsState,
        diagnosticResultsState,
        findState,
        runningDiagnostics
    ) { demo, gestures, diagResults, find, isRunningDiag ->
        AuxHolder(demo, gestures, diagResults, find.first, find.second, isRunningDiag)
    }

    private val historyHolder = combine(
        repository.batteryHistoryFlow,
        repository.connectionEventsFlow,
        repository.deviceProfilesFlow,
        repository.diagnosticLogsFlow
    ) { bat, conn, prof, logs ->
        HistoryHolder(bat, conn, prof, logs)
    }

    val uiState: StateFlow<SoniqUiState> = combine(
        repository.bluetoothSnapshot,
        repository.equalizerState,
        repository.preferencesFlow,
        auxState,
        historyHolder
    ) { bt, eq, prefs, aux, hist ->
        val resolvedActive = aux.demoDevice ?: bt.activeLiveDevice
        SoniqUiState(
            activeDevice = resolvedActive,
            isDemoPreviewActive = aux.demoDevice != null,
            bluetoothSnapshot = bt,
            equalizerState = eq,
            preferences = prefs,
            gestureMappings = aux.gestures,
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

    private data class AuxHolder(
        val demoDevice: EarbudsDevice?,
        val gestures: List<GestureMapping>,
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
    }

    fun pairDevice(address: String) {
        repository.triggerHaptic()
        val ok = repository.bluetoothController.pairDevice(address)
        viewModelScope.launch {
            if (ok) {
                _events.emit(SoniqUiEvent.ShowMessage("Pairing request initiated"))
            } else {
                _events.emit(SoniqUiEvent.OpenSystemBluetoothSettings)
            }
        }
    }

    fun connectDevice(address: String) {
        repository.triggerHaptic()
        if (address == DemoPreviewFactory.DEMO_ADDRESS) {
            enableDemoPreview()
            return
        }
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
        if (address == DemoPreviewFactory.DEMO_ADDRESS) {
            disableDemoPreview()
            return
        }
        val ok = repository.bluetoothController.disconnectDevice(address)
        viewModelScope.launch {
            if (!ok) {
                _events.emit(SoniqUiEvent.OpenSystemBluetoothSettings)
            }
        }
    }

    fun enableDemoPreview() {
        repository.triggerHaptic()
        val eq = repository.equalizerState.value
        demoDeviceState.value = DemoPreviewFactory.createDemoDevice(
            systemEqAvailable = eq.isEngineAvailable,
            systemBassAvailable = eq.bassBoostAvailable,
            systemVirtualizerAvailable = eq.virtualizerAvailable
        )
        viewModelScope.launch {
            _events.emit(SoniqUiEvent.ShowMessage("Demo Preview activated (Simulated telemetry — not live hardware)"))
        }
    }

    fun disableDemoPreview() {
        repository.triggerHaptic()
        demoDeviceState.value = null
        repository.bluetoothController.refreshBondedAndConnected()
    }

    fun toggleDemoCaseLid() {
        val current = demoDeviceState.value ?: return
        repository.triggerHaptic()
        demoDeviceState.value = current.copy(isCaseOpen = !current.isCaseOpen)
    }

    fun setAncMode(mode: AncMode) {
        val active = uiState.value.activeDevice ?: return
        if (!active.capabilities.supportsAncControl) {
            emitUnsupported("Active Noise Cancellation")
            return
        }
        repository.triggerHaptic()
        if (active.dataSourceMode == DataSourceMode.DEMO_PREVIEW) {
            demoDeviceState.update { it?.copy(ancMode = mode) }
        }
    }

    fun setAncIntensity(intensity: Float) {
        val active = uiState.value.activeDevice ?: return
        if (!active.capabilities.supportsAmbientIntensity) return
        if (active.dataSourceMode == DataSourceMode.DEMO_PREVIEW) {
            demoDeviceState.update { it?.copy(ancIntensity = intensity.coerceIn(0.1f, 1f)) }
        }
    }

    fun toggleGamingMode() {
        val active = uiState.value.activeDevice ?: return
        if (!active.capabilities.supportsLowLatencyGamingMode) {
            emitUnsupported("Hardware Low-Latency Gaming Mode")
            return
        }
        repository.triggerHaptic()
        if (active.dataSourceMode == DataSourceMode.DEMO_PREVIEW) {
            demoDeviceState.update { it?.copy(gamingModeEnabled = !it.gamingModeEnabled) }
        }
    }

    fun setSpatialAudioMode(mode: SpatialAudioMode) {
        val active = uiState.value.activeDevice
        val eq = uiState.value.equalizerState
        if (active?.dataSourceMode == DataSourceMode.DEMO_PREVIEW) {
            repository.triggerHaptic()
            demoDeviceState.update { it?.copy(spatialAudioMode = mode) }
            val virtStrength = when (mode) {
                SpatialAudioMode.OFF -> 0
                SpatialAudioMode.FIXED_STAGE -> 600
                SpatialAudioMode.HEAD_TRACKING -> 900
            }
            viewModelScope.launch { repository.setVirtualizerStrength(virtStrength) }
            return
        }
        if (eq.virtualizerAvailable) {
            repository.triggerHaptic()
            val strength = if (mode == SpatialAudioMode.OFF) 0 else 650
            viewModelScope.launch { repository.setVirtualizerStrength(strength) }
        } else {
            emitUnsupported("Spatial Audio / Virtualizer")
        }
    }

    fun toggleInEarDetection() {
        val active = uiState.value.activeDevice ?: return
        if (!active.capabilities.supportsInEarDetection) {
            emitUnsupported("In-Ear Detection & Auto Play/Pause")
            return
        }
        repository.triggerHaptic()
        if (active.dataSourceMode == DataSourceMode.DEMO_PREVIEW) {
            demoDeviceState.update {
                it?.copy(
                    inEarDetectionEnabled = !it.inEarDetectionEnabled,
                    autoPlayPauseEnabled = !it.inEarDetectionEnabled
                )
            }
        }
    }

    fun toggleDualDeviceMultipoint() {
        val active = uiState.value.activeDevice ?: return
        if (!active.capabilities.supportsDualDeviceMultipoint) {
            emitUnsupported("Dual-Device Multipoint Control")
            return
        }
        repository.triggerHaptic()
        if (active.dataSourceMode == DataSourceMode.DEMO_PREVIEW) {
            demoDeviceState.update { it?.copy(dualDeviceEnabled = !it.dualDeviceEnabled) }
        }
    }

    fun switchMultipointPrimary(hostName: String) {
        val active = uiState.value.activeDevice ?: return
        if (active.dataSourceMode != DataSourceMode.DEMO_PREVIEW) return
        repository.triggerHaptic()
        demoDeviceState.update { device ->
            device?.copy(
                multipointHosts = device.multipointHosts.map { host ->
                    host.copy(isActiveAudioSource = host.hostName == hostName)
                }
            )
        }
    }

    fun toggleCallNoiseReduction() {
        val active = uiState.value.activeDevice ?: return
        if (!active.capabilities.supportsCallMicControls) {
            emitUnsupported("Call Microphone ENC")
            return
        }
        repository.triggerHaptic()
        if (active.dataSourceMode == DataSourceMode.DEMO_PREVIEW) {
            demoDeviceState.update { it?.copy(callNoiseReductionEnabled = !it.callNoiseReductionEnabled) }
        }
    }

    fun toggleMicSidetone() {
        val active = uiState.value.activeDevice ?: return
        if (!active.capabilities.supportsCallMicControls) {
            emitUnsupported("Microphone Sidetone")
            return
        }
        repository.triggerHaptic()
        if (active.dataSourceMode == DataSourceMode.DEMO_PREVIEW) {
            demoDeviceState.update { it?.copy(micSidetoneEnabled = !it.micSidetoneEnabled) }
        }
    }

    fun updateTouchGesture(side: EarbudSide, trigger: TouchTrigger, action: TouchAction) {
        val active = uiState.value.activeDevice
        if (active != null && !active.capabilities.supportsCustomTouchGestures) {
            emitUnsupported("Custom Touch Gesture Remapping")
            return
        }
        repository.triggerHaptic()
        gestureMappingsState.update { list ->
            list.map { item ->
                if (item.side == side && item.trigger == trigger) item.copy(action = action) else item
            }
        }
    }

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

    fun updateVirtualizer(strength: Int) {
        viewModelScope.launch { repository.setVirtualizerStrength(strength) }
    }

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
        val name = dev?.name ?: "Last Connected Earbuds"
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
        val state = uiState.value
        val active = state.activeDevice
        val gesturesJson = JSONArray().apply {
            state.gestureMappings.forEach { g ->
                put(JSONObject().apply {
                    put("side", g.side.name)
                    put("trigger", g.trigger.name)
                    put("action", g.action.name)
                })
            }
        }.toString()
        viewModelScope.launch {
            repository.saveProfile(
                profileName = profileName,
                deviceName = active?.name ?: "General Earbuds",
                deviceAddress = active?.address ?: "",
                gamingModePreferred = active?.gamingModeEnabled == true,
                gesturesJson = gesturesJson
            )
            _events.emit(SoniqUiEvent.ShowMessage("Saved profile \"$profileName\""))
        }
    }

    fun applyProfile(profile: DeviceProfileEntity) {
        repository.triggerHaptic()
        viewModelScope.launch {
            repository.applyProfile(profile)
            runCatching {
                val arr = JSONArray(profile.gesturesJson)
                val parsed = mutableListOf<GestureMapping>()
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val side = EarbudSide.valueOf(obj.getString("side"))
                    val trigger = TouchTrigger.valueOf(obj.getString("trigger"))
                    val action = TouchAction.valueOf(obj.getString("action"))
                    parsed.add(GestureMapping(side, trigger, action))
                }
                if (parsed.size == 8) gestureMappingsState.value = parsed
            }
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

    private fun emitUnsupported(featureName: String) {
        viewModelScope.launch {
            _events.emit(SoniqUiEvent.ShowMessage("$featureName is not exposed by this device's Bluetooth protocol"))
        }
    }

    class Factory(private val repository: SoniqRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SoniqViewModel(repository) as T
        }
    }
}
