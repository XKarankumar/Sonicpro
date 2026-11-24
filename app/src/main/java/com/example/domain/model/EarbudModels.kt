package com.example.domain.model

enum class DataSourceMode {
    LIVE_HARDWARE,
    DEMO_PREVIEW
}

enum class BluetoothConnectionState {
    UNAVAILABLE,
    BLUETOOTH_OFF,
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    DISCONNECTING
}

enum class EarbudBrand(val displayName: String) {
    ONEPLUS("OnePlus"),
    OPPO("OPPO"),
    REALME("Realme"),
    NOTHING("Nothing"),
    SONY("Sony"),
    SAMSUNG("Samsung"),
    APPLE("Apple / Beats"),
    GOOGLE("Google Fast Pair"),
    GENERIC_BT("Standard Bluetooth")
}

enum class AncMode(val label: String, val shortLabel: String) {
    OFF("Noise Control Off", "Off"),
    NOISE_CANCELLATION("Active Noise Cancellation", "ANC"),
    TRANSPARENCY("Transparency Mode", "Transparent"),
    ADAPTIVE("Adaptive Ambient", "Adaptive")
}

enum class SpatialAudioMode(val label: String) {
    OFF("Stereo Direct"),
    FIXED_STAGE("Fixed Spatial Stage"),
    HEAD_TRACKING("Dynamic Head Tracking")
}

enum class ThemePreference(val label: String) {
    DARK_OLED("Obsidian Black (Default)"),
    LIGHT_CRIMSON("Studio Light"),
    SYSTEM("Follow System")
}

enum class DashboardWidgetId(val title: String, val subtitle: String) {
    BATTERY_TRIAD("Battery Telemetry", "Left, Right & Charging Case rings"),
    ANC_CONTROL("Noise Control", "ANC, Transparency & Ambient levels"),
    AUDIO_VISUALIZER("Acoustic Visualizer", "Real-time harmonic frequency pulse"),
    QUICK_CONTROLS("Audio & Sensor Toggles", "Gaming, Spatial, In-Ear & Bass"),
    CODEC_TELEMETRY("Codec & Link Quality", "Active Bluetooth audio codec & RSSI"),
    MULTIPOINT_STATUS("Dual-Device Connection", "Primary and secondary paired hosts")
}

data class DeviceCapabilities(
    val supportsIndividualBattery: Boolean = false,
    val supportsSingleBattery: Boolean = false,
    val supportsAncControl: Boolean = false,
    val supportsTransparencyMode: Boolean = false,
    val supportsAmbientIntensity: Boolean = false,
    val supportsCustomTouchGestures: Boolean = false,
    val supportsLowLatencyGamingMode: Boolean = false,
    val supportsSpatialAudioHardware: Boolean = false,
    val supportsInEarDetection: Boolean = false,
    val supportsDualDeviceMultipoint: Boolean = false,
    val supportsCallMicControls: Boolean = false,
    val supportsFirmwareRead: Boolean = false,
    val supportsFirmwareOta: Boolean = false,
    val supportsCodecInfo: Boolean = false,
    val supportsSystemEqualizer: Boolean = false,
    val supportsSystemBassBoost: Boolean = false,
    val supportsSystemVirtualizer: Boolean = false,
    val protocolNotes: String = ""
)

data class AudioCodecInfo(
    val codecName: String,
    val sampleRateHz: Int? = null,
    val bitsPerSample: Int? = null,
    val channelMode: String? = null
)

data class BatteryTelemetry(
    val leftPercent: Int? = null,
    val rightPercent: Int? = null,
    val casePercent: Int? = null,
    val singlePercent: Int? = null,
    val isLeftCharging: Boolean = false,
    val isRightCharging: Boolean = false,
    val isCaseCharging: Boolean = false,
    val sourceLabel: String = "Not reported",
    val lastUpdatedMs: Long = System.currentTimeMillis()
) {
    val hasAnyReading: Boolean
        get() = leftPercent != null || rightPercent != null || casePercent != null || singlePercent != null
}

data class MultipointHost(
    val hostName: String,
    val hostType: String,
    val isActiveAudioSource: Boolean,
    val isConnected: Boolean
)

data class EarbudsDevice(
    val address: String,
    val name: String,
    val brand: EarbudBrand,
    val rssiDbm: Int? = null,
    val isBonded: Boolean = false,
    val isAudioDevice: Boolean = true,
    val connectionState: BluetoothConnectionState = BluetoothConnectionState.DISCONNECTED,
    val isCaseOpen: Boolean = false,
    val battery: BatteryTelemetry = BatteryTelemetry(),
    val ancMode: AncMode = AncMode.OFF,
    val ancIntensity: Float = 0.75f,
    val gamingModeEnabled: Boolean = false,
    val spatialAudioMode: SpatialAudioMode = SpatialAudioMode.OFF,
    val inEarDetectionEnabled: Boolean = false,
    val autoPlayPauseEnabled: Boolean = false,
    val dualDeviceEnabled: Boolean = false,
    val callNoiseReductionEnabled: Boolean = false,
    val micSidetoneEnabled: Boolean = false,
    val codecInfo: AudioCodecInfo? = null,
    val firmwareVersion: String? = null,
    val hardwareRevision: String? = null,
    val manufacturerName: String? = null,
    val capabilities: DeviceCapabilities = DeviceCapabilities(),
    val dataSourceMode: DataSourceMode = DataSourceMode.LIVE_HARDWARE,
    val multipointHosts: List<MultipointHost> = emptyList()
)

data class EqBandState(
    val index: Int,
    val centerFreqHz: Int,
    val label: String,
    val levelMb: Int,
    val minMb: Int,
    val maxMb: Int
)

enum class EqPresetId(val displayName: String, val description: String, val curveMb: List<Int>) {
    STUDIO_REFERENCE("Studio Reference", "Flat neutral mastering curve", listOf(0, 0, 0, 0, 0)),
    ONEPLUS_BOLD_BASS("Crimson Bass", "Deep sub-bass punch with crisp highs", listOf(550, 320, -100, 180, 300)),
    VOCAL_CLARITY("Vocal Presence", "Forward mids for podcasts and vocals", listOf(-150, 100, 420, 350, 100)),
    ACOUSTIC_STAGE("Acoustic Air", "Warm lows and airy harmonic sparkle", listOf(180, 120, 80, 260, 420)),
    ELECTRONIC_PULSE("Electronic Pulse", "V-shaped club energy", listOf(480, 200, -180, 240, 460)),
    CUSTOM("Custom Curve", "User tuned frequency response", listOf(0, 0, 0, 0, 0))
}

data class EqualizerState(
    val isEngineAvailable: Boolean = false,
    val isEnabled: Boolean = true,
    val activePreset: EqPresetId = EqPresetId.STUDIO_REFERENCE,
    val bands: List<EqBandState> = defaultBands(),
    val bassBoostAvailable: Boolean = false,
    val bassBoostStrength: Int = 0, // 0..1000
    val virtualizerAvailable: Boolean = false,
    val virtualizerStrength: Int = 0, // 0..1000
    val statusMessage: String = "System AudioFX DSP ready"
) {
    companion object {
        fun defaultBands(): List<EqBandState> = listOf(
            EqBandState(0, 60, "60Hz", 0, -1500, 1500),
            EqBandState(1, 230, "230Hz", 0, -1500, 1500),
            EqBandState(2, 910, "910Hz", 0, -1500, 1500),
            EqBandState(3, 3600, "3.6kHz", 0, -1500, 1500),
            EqBandState(4, 14000, "14kHz", 0, -1500, 1500)
        )
    }
}

enum class EarbudSide(val label: String) {
    LEFT("Left Earbud"),
    RIGHT("Right Earbud")
}

enum class TouchTrigger(val label: String) {
    SINGLE_TAP("Single Tap"),
    DOUBLE_TAP("Double Tap"),
    TRIPLE_TAP("Triple Tap"),
    TOUCH_AND_HOLD("Touch & Hold")
}

enum class TouchAction(val label: String) {
    PLAY_PAUSE("Play / Pause"),
    NEXT_TRACK("Next Track"),
    PREVIOUS_TRACK("Previous Track"),
    VOICE_ASSISTANT("Voice Assistant"),
    CYCLE_ANC("Cycle ANC / Transparency"),
    VOLUME_UP("Volume Up"),
    VOLUME_DOWN("Volume Down"),
    GAMING_MODE("Toggle Low Latency"),
    NONE("Disabled")
}

data class GestureMapping(
    val side: EarbudSide,
    val trigger: TouchTrigger,
    val action: TouchAction
)

data class LastKnownLocationRecord(
    val deviceName: String,
    val deviceAddress: String,
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float,
    val recordedAtMs: Long
)

data class AppReleaseNote(
    val versionName: String,
    val releaseDate: String,
    val highlights: List<String>,
    val isCurrent: Boolean = false
)

data class DiagnosticCheckResult(
    val id: String,
    val title: String,
    val passed: Boolean,
    val detail: String
)

data class NotificationSettings(
    val batteryLowAlertEnabled: Boolean = true,
    val batteryLowThreshold: Int = 20,
    val connectionAlertEnabled: Boolean = true,
    val firmwareAlertEnabled: Boolean = true,
    val lastKnownLocationOnDisconnect: Boolean = false
)
