package com.example.domain.model

enum class BluetoothConnectionState {
    UNAVAILABLE,
    BLUETOOTH_OFF,
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    DISCONNECTING
}

enum class AudioDeviceCategory(val label: String) {
    EARBUDS("True Wireless Earbuds"),
    HEADPHONES("Over-Ear / On-Ear Headphones"),
    SPEAKER("Bluetooth Speaker"),
    SOUNDBAR("Soundbar"),
    HOME_THEATRE("Home Theatre / AVR")
}

enum class EarbudBrand(val displayName: String) {
    ONEPLUS("OnePlus"),
    SAMSUNG("Samsung"),
    SONY("Sony"),
    JBL("JBL"),
    BOSE("Bose"),
    APPLE("Apple AirPods"),
    NOTHING("Nothing"),
    REALME("Realme"),
    OPPO("Oppo"),
    VIVO("Vivo"),
    XIAOMI("Xiaomi"),
    BOAT("boAt"),
    NOISE("Noise"),
    SENNHEISER("Sennheiser"),
    GENERIC_BT("Generic Bluetooth")
}

enum class AncMode(val label: String, val shortLabel: String) {
    OFF("Noise Control Off", "Off"),
    NOISE_CANCELLATION("Active Noise Cancellation", "ANC"),
    TRANSPARENCY("Transparency Mode", "Transparent"),
    ADAPTIVE("Adaptive ANC", "Adaptive")
}

enum class SpatialImmersiveLevel(val label: String) {
    NONE("Unavailable on Output"),
    MULTICHANNEL("Multichannel Spatializer"),
    BED_PLUS_OBJECTS("Spatial Audio + Objects"),
    LEGACY_VIRTUALIZER("Legacy AudioFX Virtualizer")
}

enum class ThemePreference(val label: String) {
    DARK_OLED("AMOLED Deep Black (Default)"),
    LIGHT_CRIMSON("Studio Light"),
    SYSTEM("Follow System")
}

enum class DashboardWidgetId(val title: String, val subtitle: String) {
    BATTERY_TRIAD("Live Battery Telemetry", "Left, Right, Case or combined battery"),
    ANC_CONTROL("ANC & Smart Controls", "ANC, Transparency, Adaptive & Wear Detection"),
    AUDIO_VISUALIZER("Acoustic Studio Profile", "Active EQ preset & DSP response"),
    CODEC_TELEMETRY("Codec & Link Quality", "Active codec, sample rate, bit depth & RSSI"),
    RECENT_DEVICES("Recently Connected", "Quick access to recent audio hardware")
}

data class FeatureAvailabilityItem(
    val featureName: String,
    val isSupported: Boolean,
    val requirementNote: String
)

data class DeviceCapabilities(
    val integrationId: String = "GenericBluetoothIntegration",
    val supportsIndividualBattery: Boolean = false,
    val supportsSingleBattery: Boolean = false,
    val supportsAncControl: Boolean = false,
    val supportsTransparencyMode: Boolean = false,
    val supportsAdaptiveAnc: Boolean = false,
    val supportsAmbientIntensity: Boolean = false,
    val supportsCustomTouchGestures: Boolean = false,
    val supportsWearDetection: Boolean = false,
    val supportsFindDevice: Boolean = true,
    val supportsFirmwareRead: Boolean = false,
    val supportsCodecInfo: Boolean = false,
    val supportsSystemEqualizer: Boolean = false,
    val supportsSystemBassBoost: Boolean = false,
    val supportsSystemVocalEnhancement: Boolean = false,
    val supportsSpatializer: Boolean = false,
    val supportsHeadTracking: Boolean = false,
    val protocolNotes: String = "",
    val featureMatrix: List<FeatureAvailabilityItem> = emptyList()
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
    val sourceLabel: String = "Unavailable from device",
    val lastUpdatedMs: Long = System.currentTimeMillis()
) {
    val hasAnyReading: Boolean
        get() = leftPercent != null || rightPercent != null || casePercent != null || singlePercent != null
}

data class EarbudsDevice(
    val address: String,
    val name: String,
    val brand: EarbudBrand,
    val category: AudioDeviceCategory = AudioDeviceCategory.EARBUDS,
    val rssiDbm: Int? = null,
    val isBonded: Boolean = false,
    val isAudioDevice: Boolean = true,
    val connectionState: BluetoothConnectionState = BluetoothConnectionState.DISCONNECTED,
    val battery: BatteryTelemetry = BatteryTelemetry(),
    val codecInfo: AudioCodecInfo? = null,
    val firmwareVersion: String? = null,
    val hardwareRevision: String? = null,
    val manufacturerName: String? = null,
    val modelNumber: String? = null,
    val capabilities: DeviceCapabilities = DeviceCapabilities()
)

data class EqBandState(
    val index: Int,
    val centerFreqHz: Int,
    val label: String,
    val levelMb: Int,
    val minMb: Int,
    val maxMb: Int
)

enum class EqPresetId(
    val displayName: String,
    val description: String,
    val targetCurveDb: List<Float>
) {
    STUDIO_REFERENCE("Studio Reference", "Flat neutral mastering curve", listOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f)),
    MUSIC("Music", "Balanced dynamic sparkle with warm sub-bass", listOf(2.5f, 3.0f, 1.5f, 0f, -0.5f, 0.5f, 1.5f, 2.5f, 3.0f, 2.0f)),
    CINEMA("Cinema", "Wide soundstage, deep LFE rumble & dialogue clarity", listOf(4.5f, 4.0f, 2.0f, -0.5f, 1.0f, 2.5f, 3.0f, 2.0f, 1.5f, 1.0f)),
    GAMING_AUDIO("Gaming Audio", "Footstep & directional cue emphasis", listOf(-1.0f, 0.5f, 1.5f, 1.0f, 0.5f, 2.0f, 3.5f, 4.0f, 3.0f, 1.5f)),
    BASS_ENHANCEMENT("Bass Boost", "Sub-bass and punchy low-end extension", listOf(6.0f, 5.5f, 4.0f, 2.0f, 0f, 0f, 0.5f, 1.0f, 1.5f, 1.0f)),
    VOCAL_ENHANCEMENT("Vocal Clarity", "Presence & dialogue intelligibility lift", listOf(-2.0f, -1.5f, -0.5f, 1.5f, 3.5f, 4.5f, 4.0f, 2.0f, 0.5f, 0f)),
    CUSTOM("Custom", "User-tuned parametric curve", listOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f))
}

data class EqualizerState(
    val isEngineAvailable: Boolean = false,
    val isEnabled: Boolean = true,
    val hardwareBandCount: Int = 5,
    val activePreset: EqPresetId = EqPresetId.STUDIO_REFERENCE,
    val bands: List<EqBandState> = defaultBands(),
    val bassBoostAvailable: Boolean = false,
    val bassBoostStrength: Int = 0, // 0..1000
    val vocalEnhancerAvailable: Boolean = false,
    val vocalGainMb: Int = 0, // 0..1500
    val virtualizerAvailable: Boolean = false,
    val virtualizerStrength: Int = 0, // 0..1000
    val statusMessage: String = "Initializing AudioFX DSP..."
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

data class SpatialAudioCapabilityState(
    val apiSupported: Boolean = false,
    val isAvailable: Boolean = false,
    val isEnabled: Boolean = false,
    val immersiveLevel: SpatialImmersiveLevel = SpatialImmersiveLevel.NONE,
    val isHeadTrackerAvailable: Boolean = false,
    val isHeadTrackerEnabled: Boolean = false,
    val supportsStereo: Boolean = true,
    val supports5Point1: Boolean = false,
    val supports7Point1: Boolean = false,
    val supportsDolbyMultichannel: Boolean = false,
    val explanation: String = "Requires Android 13 (API 33+) Spatializer HAL support."
)

enum class EarbudSide(val label: String) {
    LEFT("Left Earbud"),
    RIGHT("Right Earbud")
}

data class TheatreNetworkDevice(
    val serviceName: String,
    val serviceType: String,
    val protocolLabel: String,
    val hostAddress: String?,
    val port: Int?,
    val modelHint: String?,
    val isResolved: Boolean
)

data class AudioOutputRoute(
    val id: Int,
    val productName: String,
    val typeLabel: String,
    val isHomeTheatreCapable: Boolean,
    val supportedEncodings: List<String>
)

data class TheatreEcosystemState(
    val isWifiConnected: Boolean = false,
    val isScanningNetwork: Boolean = false,
    val discoveredNetworkDevices: List<TheatreNetworkDevice> = emptyList(),
    val activeOutputRoutes: List<AudioOutputRoute> = emptyList(),
    val currentStreamVolume: Int = 0,
    val maxStreamVolume: Int = 15,
    val isMuted: Boolean = false,
    val statusMessage: String = "Ready for Home Theatre & Speaker discovery"
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
