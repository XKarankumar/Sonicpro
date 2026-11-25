package com.example.domain.integration

import android.os.ParcelUuid
import com.example.domain.model.AudioDeviceCategory
import com.example.domain.model.DeviceCapabilities
import com.example.domain.model.EarbudBrand
import com.example.domain.model.FeatureAvailabilityItem
import com.example.domain.model.SpatialAudioCapabilityState

data class IntegrationScanContext(
    val deviceName: String?,
    val manufacturerIds: Set<Int> = emptySet(),
    val serviceUuidStrings: List<String> = emptyList(),
    val bluetoothMajorClass: Int? = null,
    val bluetoothDeviceClass: Int? = null
)

data class IntegrationTelemetryContext(
    val hasGattBatteryService: Boolean,
    val hasDeviceInfoService: Boolean,
    val hasSingleBatteryLevel: Boolean,
    val hasCodecInfo: Boolean,
    val systemEqAvailable: Boolean,
    val systemBassAvailable: Boolean,
    val systemVocalAvailable: Boolean,
    val spatialState: SpatialAudioCapabilityState
)

data class OemIntegrationInfo(
    val integrationId: String,
    val displayName: String,
    val coveredBrands: List<EarbudBrand>,
    val transportStandards: String,
    val sdkStatus: String,
    val unavailableNote: String
)

interface OemDeviceIntegration {
    val info: OemIntegrationInfo

    fun matches(ctx: IntegrationScanContext): Boolean

    fun inferBrand(ctx: IntegrationScanContext): EarbudBrand

    fun inferCategory(ctx: IntegrationScanContext): AudioDeviceCategory = AudioDeviceCategory.EARBUDS

    fun detectCapabilities(telemetry: IntegrationTelemetryContext): DeviceCapabilities

    fun connectionGuidance(): String

    fun handleProtocolError(error: Throwable): String =
        "${info.displayName} protocol error: ${error.localizedMessage ?: error.javaClass.simpleName}"
}

private fun buildCommonFeatureMatrix(
    telemetry: IntegrationTelemetryContext,
    oemAncRequirement: String,
    oemBatteryRequirement: String,
    oemTouchWearRequirement: String
): List<FeatureAvailabilityItem> = listOf(
    FeatureAvailabilityItem(
        featureName = "Audio Stream Equalizer & Bass DSP",
        isSupported = telemetry.systemEqAvailable || telemetry.systemBassAvailable,
        requirementNote = if (telemetry.systemEqAvailable) {
            "Active via Android AudioFX Equalizer/BassBoost"
        } else {
            "UNAVAILABLE — AudioFX global session rejected by device HAL"
        }
    ),
    FeatureAvailabilityItem(
        featureName = "Vocal Clarity DSP",
        isSupported = telemetry.systemVocalAvailable,
        requirementNote = if (telemetry.systemVocalAvailable) {
            "Active via Android LoudnessEnhancer"
        } else {
            "UNAVAILABLE — LoudnessEnhancer unsupported on output mix"
        }
    ),
    FeatureAvailabilityItem(
        featureName = "Combined Device Battery",
        isSupported = telemetry.hasGattBatteryService || telemetry.hasSingleBatteryLevel,
        requirementNote = if (telemetry.hasGattBatteryService || telemetry.hasSingleBatteryLevel) {
            "Live reading via Android HFP / BLE GATT 0x180F"
        } else {
            "UNAVAILABLE — Connected device did not report GATT 0x180F or HFP battery"
        }
    ),
    FeatureAvailabilityItem(
        featureName = "Individual L / R / Case Battery",
        isSupported = false,
        requirementNote = oemBatteryRequirement
    ),
    FeatureAvailabilityItem(
        featureName = "ANC / Transparency / Adaptive Mode",
        isSupported = false,
        requirementNote = oemAncRequirement
    ),
    FeatureAvailabilityItem(
        featureName = "Touch Controls & Wear Detection",
        isSupported = false,
        requirementNote = oemTouchWearRequirement
    ),
    FeatureAvailabilityItem(
        featureName = "3D Spatial Audio & Head Tracking",
        isSupported = telemetry.spatialState.isAvailable,
        requirementNote = telemetry.spatialState.explanation
    ),
    FeatureAvailabilityItem(
        featureName = "Find Device Locator Chime",
        isSupported = true,
        requirementNote = "Supported via stereo / isolated Left-Right AudioTrack chime"
    )
)

class OnePlusIntegration : OemDeviceIntegration {
    private val companyIds = setOf(0x058E, 0x079A, 0x06AE)
    override val info = OemIntegrationInfo(
        integrationId = "OnePlusIntegration",
        displayName = "OnePlus / HeyMelody Audio",
        coveredBrands = listOf(EarbudBrand.ONEPLUS, EarbudBrand.OPPO, EarbudBrand.REALME),
        transportStandards = "Bluetooth A2DP/HFP, BLE GATT (0x180F / 0x180A), Google Fast Pair (0xFE2C)",
        sdkStatus = "Standard Android BT + BLE GATT active; official HeyMelody RFCOMM requires OEM-signed authorization",
        unavailableNote = "Per-bud battery, hardware ANC switching, touch remapping, and OTA require official OnePlus/HeyMelody SDK credentials."
    )

    override fun matches(ctx: IntegrationScanContext): Boolean {
        if (ctx.manufacturerIds.any { it in companyIds }) return true
        val lower = ctx.deviceName?.lowercase().orEmpty()
        return lower.contains("oneplus") || lower.contains("nord buds") ||
            lower.contains("oppo") || lower.contains("enco") || lower.contains("realme")
    }

    override fun inferBrand(ctx: IntegrationScanContext): EarbudBrand {
        val lower = ctx.deviceName?.lowercase().orEmpty()
        return when {
            lower.contains("oppo") || lower.contains("enco") || ctx.manufacturerIds.contains(0x079A) -> EarbudBrand.OPPO
            lower.contains("realme") -> EarbudBrand.REALME
            else -> EarbudBrand.ONEPLUS
        }
    }

    override fun detectCapabilities(telemetry: IntegrationTelemetryContext): DeviceCapabilities = DeviceCapabilities(
        integrationId = info.integrationId,
        supportsSingleBattery = telemetry.hasGattBatteryService || telemetry.hasSingleBatteryLevel,
        supportsFindDevice = true,
        supportsFirmwareRead = telemetry.hasDeviceInfoService,
        supportsCodecInfo = telemetry.hasCodecInfo,
        supportsSystemEqualizer = telemetry.systemEqAvailable,
        supportsSystemBassBoost = telemetry.systemBassAvailable,
        supportsSystemVocalEnhancement = telemetry.systemVocalAvailable,
        supportsSpatializer = telemetry.spatialState.isAvailable,
        supportsHeadTracking = telemetry.spatialState.isHeadTrackerAvailable,
        protocolNotes = info.unavailableNote,
        featureMatrix = buildCommonFeatureMatrix(
            telemetry,
            oemAncRequirement = "UNAVAILABLE — Requires official OnePlus/HeyMelody OEM SDK authorization",
            oemBatteryRequirement = "UNAVAILABLE — Individual L/R/Case channels require OnePlus HeyMelody RFCOMM service",
            oemTouchWearRequirement = "UNAVAILABLE — Requires OnePlus/HeyMelody companion service permission"
        )
    )

    override fun connectionGuidance(): String =
        "Pair via Android Bluetooth or Fast Pair. SONIQ monitors live A2DP/AAC/LHDC codec info, battery level, and applies Studio AudioFX DSP."
}

class SamsungIntegration : OemDeviceIntegration {
    private val companyId = 0x0075
    override val info = OemIntegrationInfo(
        integrationId = "SamsungIntegration",
        displayName = "Samsung Galaxy Audio",
        coveredBrands = listOf(EarbudBrand.SAMSUNG),
        transportStandards = "Bluetooth A2DP/HFP (AAC/SBC/SSC), BLE GATT (0x180F / 0x180A)",
        sdkStatus = "Standard Android BT + BLE GATT active; Galaxy Wearable plugin APIs are proprietary",
        unavailableNote = "360 Audio head-tracking and Galaxy Buds ANC/touch controls require Samsung Galaxy Wearable accessory framework."
    )

    override fun matches(ctx: IntegrationScanContext): Boolean {
        val lower = ctx.deviceName?.lowercase().orEmpty()
        return ctx.manufacturerIds.contains(companyId) && lower.contains("buds") ||
            lower.contains("galaxy buds") || lower.contains("samsung")
    }

    override fun inferBrand(ctx: IntegrationScanContext): EarbudBrand = EarbudBrand.SAMSUNG

    override fun detectCapabilities(telemetry: IntegrationTelemetryContext): DeviceCapabilities = DeviceCapabilities(
        integrationId = info.integrationId,
        supportsSingleBattery = telemetry.hasGattBatteryService || telemetry.hasSingleBatteryLevel,
        supportsFindDevice = true,
        supportsFirmwareRead = telemetry.hasDeviceInfoService,
        supportsCodecInfo = telemetry.hasCodecInfo,
        supportsSystemEqualizer = telemetry.systemEqAvailable,
        supportsSystemBassBoost = telemetry.systemBassAvailable,
        supportsSystemVocalEnhancement = telemetry.systemVocalAvailable,
        supportsSpatializer = telemetry.spatialState.isAvailable,
        supportsHeadTracking = telemetry.spatialState.isHeadTrackerAvailable,
        protocolNotes = info.unavailableNote,
        featureMatrix = buildCommonFeatureMatrix(
            telemetry,
            oemAncRequirement = "UNAVAILABLE — Requires Samsung Galaxy Wearable Accessory SDK",
            oemBatteryRequirement = "UNAVAILABLE — Individual L/R/Case telemetry requires Galaxy Wearable SPP protocol",
            oemTouchWearRequirement = "UNAVAILABLE — Requires Galaxy Wearable plugin permission"
        )
    )

    override fun connectionGuidance(): String =
        "Connect Galaxy Buds via Android Bluetooth. Live battery, codec telemetry, and Studio AudioFX EQ work automatically."
}

class SonyIntegration : OemDeviceIntegration {
    private val companyId = 0x012D
    override val info = OemIntegrationInfo(
        integrationId = "SonyIntegration",
        displayName = "Sony Audio / Sound Connect",
        coveredBrands = listOf(EarbudBrand.SONY),
        transportStandards = "Bluetooth A2DP (LDAC / AAC / SBC), BLE GATT, Google Fast Pair",
        sdkStatus = "Standard Android BT + LDAC codec telemetry active; Sony Sound Connect Tandem protocol is proprietary",
        unavailableNote = "Sony Adaptive Sound Control, Speak-to-Chat, and DSEE Extreme require licensed Sony Partner SDK."
    )

    override fun matches(ctx: IntegrationScanContext): Boolean {
        val lower = ctx.deviceName?.lowercase().orEmpty()
        return ctx.manufacturerIds.contains(companyId) ||
            lower.contains("sony") || lower.contains("wf-1000") || lower.contains("wh-1000") || lower.contains("linkbuds")
    }

    override fun inferBrand(ctx: IntegrationScanContext): EarbudBrand = EarbudBrand.SONY

    override fun inferCategory(ctx: IntegrationScanContext): AudioDeviceCategory {
        val lower = ctx.deviceName?.lowercase().orEmpty()
        return if (lower.contains("wh-")) AudioDeviceCategory.HEADPHONES else AudioDeviceCategory.EARBUDS
    }

    override fun detectCapabilities(telemetry: IntegrationTelemetryContext): DeviceCapabilities = DeviceCapabilities(
        integrationId = info.integrationId,
        supportsSingleBattery = telemetry.hasGattBatteryService || telemetry.hasSingleBatteryLevel,
        supportsFindDevice = true,
        supportsFirmwareRead = telemetry.hasDeviceInfoService,
        supportsCodecInfo = telemetry.hasCodecInfo,
        supportsSystemEqualizer = telemetry.systemEqAvailable,
        supportsSystemBassBoost = telemetry.systemBassAvailable,
        supportsSystemVocalEnhancement = telemetry.systemVocalAvailable,
        supportsSpatializer = telemetry.spatialState.isAvailable,
        supportsHeadTracking = telemetry.spatialState.isHeadTrackerAvailable,
        protocolNotes = info.unavailableNote,
        featureMatrix = buildCommonFeatureMatrix(
            telemetry,
            oemAncRequirement = "UNAVAILABLE — Requires Sony Audio Partner SDK (Sound Connect Tandem)",
            oemBatteryRequirement = "UNAVAILABLE — Multi-battery Tandem frames require Sony Partner authorization",
            oemTouchWearRequirement = "UNAVAILABLE — Requires Sony Sound Connect SDK"
        )
    )

    override fun connectionGuidance(): String =
        "Connect Sony WF/WH audio via Bluetooth. LDAC bitrate/sample-rate telemetry and Android AudioFX EQ are supported natively."
}

class JBLIntegration : OemDeviceIntegration {
    private val companyId = 0x0057
    override val info = OemIntegrationInfo(
        integrationId = "JBLIntegration",
        displayName = "JBL / Harman Audio",
        coveredBrands = listOf(EarbudBrand.JBL),
        transportStandards = "Bluetooth A2DP/HFP, BLE GATT (0x180F / 0x180A), Google Fast Pair",
        sdkStatus = "Standard Android BT + BLE GATT active; Harman/JBL PartyBoost & ANC GATT require OEM SDK",
        unavailableNote = "JBL Ambient Aware / TalkThru hardware toggles and PartyBoost chaining require Harman OEM SDK access."
    )

    override fun matches(ctx: IntegrationScanContext): Boolean {
        val lower = ctx.deviceName?.lowercase().orEmpty()
        return ctx.manufacturerIds.contains(companyId) ||
            lower.contains("jbl") || lower.contains("harman")
    }

    override fun inferBrand(ctx: IntegrationScanContext): EarbudBrand = EarbudBrand.JBL

    override fun inferCategory(ctx: IntegrationScanContext): AudioDeviceCategory {
        val lower = ctx.deviceName?.lowercase().orEmpty()
        return when {
            lower.contains("bar") -> AudioDeviceCategory.SOUNDBAR
            lower.contains("flip") || lower.contains("charge") || lower.contains("pulse") || lower.contains("boombox") || lower.contains("partybox") -> AudioDeviceCategory.SPEAKER
            else -> AudioDeviceCategory.EARBUDS
        }
    }

    override fun detectCapabilities(telemetry: IntegrationTelemetryContext): DeviceCapabilities = DeviceCapabilities(
        integrationId = info.integrationId,
        supportsSingleBattery = telemetry.hasGattBatteryService || telemetry.hasSingleBatteryLevel,
        supportsFindDevice = true,
        supportsFirmwareRead = telemetry.hasDeviceInfoService,
        supportsCodecInfo = telemetry.hasCodecInfo,
        supportsSystemEqualizer = telemetry.systemEqAvailable,
        supportsSystemBassBoost = telemetry.systemBassAvailable,
        supportsSystemVocalEnhancement = telemetry.systemVocalAvailable,
        supportsSpatializer = telemetry.spatialState.isAvailable,
        supportsHeadTracking = telemetry.spatialState.isHeadTrackerAvailable,
        protocolNotes = info.unavailableNote,
        featureMatrix = buildCommonFeatureMatrix(
            telemetry,
            oemAncRequirement = "UNAVAILABLE — Requires Harman/JBL Headphones OEM SDK",
            oemBatteryRequirement = "UNAVAILABLE — Per-bud telemetry requires Harman proprietary BLE characteristic",
            oemTouchWearRequirement = "UNAVAILABLE — Requires JBL Headphones companion SDK"
        )
    )

    override fun connectionGuidance(): String =
        "Pair JBL earbuds or speakers via Bluetooth. Battery, codec status, AudioFX EQ, and Theatre speaker routing work natively."
}

class BoseIntegration : OemDeviceIntegration {
    private val companyId = 0x009E
    override val info = OemIntegrationInfo(
        integrationId = "BoseIntegration",
        displayName = "Bose Audio Ecosystem",
        coveredBrands = listOf(EarbudBrand.BOSE),
        transportStandards = "Bluetooth A2DP/HFP, BLE GATT (0x180F / 0x180A)",
        sdkStatus = "Standard Android BT + BLE GATT active; Bose BMAP / SimpleSync require Bose Developer credentials",
        unavailableNote = "Bose QuietComfort ANC levels, Immersive Audio mode, and SimpleSync require official Bose Partner SDK credentials."
    )

    override fun matches(ctx: IntegrationScanContext): Boolean {
        val lower = ctx.deviceName?.lowercase().orEmpty()
        return ctx.manufacturerIds.contains(companyId) ||
            lower.contains("bose") || lower.contains("quietcomfort") || lower.contains("soundlink")
    }

    override fun inferBrand(ctx: IntegrationScanContext): EarbudBrand = EarbudBrand.BOSE

    override fun inferCategory(ctx: IntegrationScanContext): AudioDeviceCategory {
        val lower = ctx.deviceName?.lowercase().orEmpty()
        return when {
            lower.contains("soundbar") || lower.contains("smart ultra") -> AudioDeviceCategory.SOUNDBAR
            lower.contains("soundlink") -> AudioDeviceCategory.SPEAKER
            lower.contains("quietcomfort") && !lower.contains("earbuds") -> AudioDeviceCategory.HEADPHONES
            else -> AudioDeviceCategory.EARBUDS
        }
    }

    override fun detectCapabilities(telemetry: IntegrationTelemetryContext): DeviceCapabilities = DeviceCapabilities(
        integrationId = info.integrationId,
        supportsSingleBattery = telemetry.hasGattBatteryService || telemetry.hasSingleBatteryLevel,
        supportsFindDevice = true,
        supportsFirmwareRead = telemetry.hasDeviceInfoService,
        supportsCodecInfo = telemetry.hasCodecInfo,
        supportsSystemEqualizer = telemetry.systemEqAvailable,
        supportsSystemBassBoost = telemetry.systemBassAvailable,
        supportsSystemVocalEnhancement = telemetry.systemVocalAvailable,
        supportsSpatializer = telemetry.spatialState.isAvailable,
        supportsHeadTracking = telemetry.spatialState.isHeadTrackerAvailable,
        protocolNotes = info.unavailableNote,
        featureMatrix = buildCommonFeatureMatrix(
            telemetry,
            oemAncRequirement = "UNAVAILABLE — Requires Bose BMAP Developer SDK authorization",
            oemBatteryRequirement = "UNAVAILABLE — Per-earbud battery requires Bose BMAP GATT service",
            oemTouchWearRequirement = "UNAVAILABLE — Requires Bose Developer SDK"
        )
    )

    override fun connectionGuidance(): String =
        "Pair Bose audio hardware over Android Bluetooth. Combined battery, codec telemetry, and Studio DSP operate natively."
}

class NothingIntegration : OemDeviceIntegration {
    override val info = OemIntegrationInfo(
        integrationId = "NothingIntegration",
        displayName = "Nothing / CMF Audio",
        coveredBrands = listOf(EarbudBrand.NOTHING),
        transportStandards = "Bluetooth A2DP/HFP, BLE GATT (0x180F / 0x180A), Google Fast Pair",
        sdkStatus = "Standard Android BT + Fast Pair active; Nothing X RFCOMM protocol is closed-source",
        unavailableNote = "Nothing X Ultra Bass, Personalized ANC, and Dirac Opteo require Nothing X OEM authorization."
    )

    override fun matches(ctx: IntegrationScanContext): Boolean {
        val lower = ctx.deviceName?.lowercase().orEmpty()
        return lower.contains("nothing") || lower.contains("ear (") || lower.contains("cmf buds")
    }

    override fun inferBrand(ctx: IntegrationScanContext): EarbudBrand = EarbudBrand.NOTHING

    override fun detectCapabilities(telemetry: IntegrationTelemetryContext): DeviceCapabilities = DeviceCapabilities(
        integrationId = info.integrationId,
        supportsSingleBattery = telemetry.hasGattBatteryService || telemetry.hasSingleBatteryLevel,
        supportsFindDevice = true,
        supportsFirmwareRead = telemetry.hasDeviceInfoService,
        supportsCodecInfo = telemetry.hasCodecInfo,
        supportsSystemEqualizer = telemetry.systemEqAvailable,
        supportsSystemBassBoost = telemetry.systemBassAvailable,
        supportsSystemVocalEnhancement = telemetry.systemVocalAvailable,
        supportsSpatializer = telemetry.spatialState.isAvailable,
        supportsHeadTracking = telemetry.spatialState.isHeadTrackerAvailable,
        protocolNotes = info.unavailableNote,
        featureMatrix = buildCommonFeatureMatrix(
            telemetry,
            oemAncRequirement = "UNAVAILABLE — Requires Nothing X OEM SDK credentials",
            oemBatteryRequirement = "UNAVAILABLE — Individual L/R/Case readings require Nothing X RFCOMM",
            oemTouchWearRequirement = "UNAVAILABLE — Requires Nothing X companion SDK"
        )
    )

    override fun connectionGuidance(): String =
        "Pair Nothing / CMF audio via Fast Pair or Android Bluetooth. Live telemetry & Studio EQ apply immediately."
}

class GenericBluetoothIntegration : OemDeviceIntegration {
    override val info = OemIntegrationInfo(
        integrationId = "GenericBluetoothIntegration",
        displayName = "Universal Standard Bluetooth",
        coveredBrands = listOf(
            EarbudBrand.APPLE,
            EarbudBrand.VIVO,
            EarbudBrand.XIAOMI,
            EarbudBrand.BOAT,
            EarbudBrand.NOISE,
            EarbudBrand.SENNHEISER,
            EarbudBrand.GENERIC_BT
        ),
        transportStandards = "Bluetooth Classic A2DP / HFP / AVRCP, BLE GATT (0x180F Battery, 0x180A Device Info), Fast Pair (0xFE2C)",
        sdkStatus = "Universal Android Bluetooth & BLE GATT standard profile",
        unavailableNote = "Vendor-specific ANC, per-bud battery, and gesture remapping require manufacturer-signed SDKs (Apple MFi/AAP, Xiaomi/Vivo/Sennheiser/boAt/Noise OEM APIs)."
    )

    override fun matches(ctx: IntegrationScanContext): Boolean = true

    override fun inferBrand(ctx: IntegrationScanContext): EarbudBrand {
        val lower = ctx.deviceName?.lowercase().orEmpty()
        return when {
            ctx.manufacturerIds.contains(0x004C) || lower.contains("airpods") || lower.contains("beats") -> EarbudBrand.APPLE
            lower.contains("vivo") || lower.contains("iqoo") || lower.contains("tws") -> EarbudBrand.VIVO
            lower.contains("xiaomi") || lower.contains("redmi") || lower.contains("poco") -> EarbudBrand.XIAOMI
            lower.contains("boat") || lower.contains("airdopes") || lower.contains("nirvana") -> EarbudBrand.BOAT
            lower.contains("noise") || lower.contains("buds vs") -> EarbudBrand.NOISE
            lower.contains("sennheiser") || lower.contains("momentum") -> EarbudBrand.SENNHEISER
            else -> EarbudBrand.GENERIC_BT
        }
    }

    override fun inferCategory(ctx: IntegrationScanContext): AudioDeviceCategory {
        val lower = ctx.deviceName?.lowercase().orEmpty()
        return when {
            lower.contains("soundbar") -> AudioDeviceCategory.SOUNDBAR
            lower.contains("speaker") || lower.contains("marshall") || lower.contains("boom") -> AudioDeviceCategory.SPEAKER
            lower.contains("momentum 4") || lower.contains("headphone") || lower.contains("studio") -> AudioDeviceCategory.HEADPHONES
            else -> AudioDeviceCategory.EARBUDS
        }
    }

    override fun detectCapabilities(telemetry: IntegrationTelemetryContext): DeviceCapabilities = DeviceCapabilities(
        integrationId = info.integrationId,
        supportsSingleBattery = telemetry.hasGattBatteryService || telemetry.hasSingleBatteryLevel,
        supportsFindDevice = true,
        supportsFirmwareRead = telemetry.hasDeviceInfoService,
        supportsCodecInfo = telemetry.hasCodecInfo,
        supportsSystemEqualizer = telemetry.systemEqAvailable,
        supportsSystemBassBoost = telemetry.systemBassAvailable,
        supportsSystemVocalEnhancement = telemetry.systemVocalAvailable,
        supportsSpatializer = telemetry.spatialState.isAvailable,
        supportsHeadTracking = telemetry.spatialState.isHeadTrackerAvailable,
        protocolNotes = info.unavailableNote,
        featureMatrix = buildCommonFeatureMatrix(
            telemetry,
            oemAncRequirement = "UNAVAILABLE — Proprietary ANC control requires manufacturer OEM SDK",
            oemBatteryRequirement = "UNAVAILABLE — Individual L/R/Case channels are not part of standard Bluetooth HFP/GATT 0x180F",
            oemTouchWearRequirement = "UNAVAILABLE — Proprietary touch/wear configuration requires OEM SDK"
        )
    )

    override fun connectionGuidance(): String =
        "Pair any Bluetooth earbuds, headphones, or speakers via Android Bluetooth. Standard battery, codec telemetry, AudioFX EQ, and Spatializer checks work universally."
}

class HomeTheatreIntegration : OemDeviceIntegration {
    override val info = OemIntegrationInfo(
        integrationId = "HomeTheatreIntegration",
        displayName = "Home Theatre & Network Audio",
        coveredBrands = listOf(EarbudBrand.SONY, EarbudBrand.JBL, EarbudBrand.BOSE, EarbudBrand.SAMSUNG, EarbudBrand.GENERIC_BT),
        transportStandards = "Android NsdManager (mDNS Cast _googlecast._tcp, AirPlay _airplay._tcp, Sonos _sonos._tcp, Spotify _spotify-connect._tcp, DLNA), AudioDeviceInfo (HDMI ARC/eARC, USB, A2DP)",
        sdkStatus = "Open LAN mDNS discovery + Android AudioManager stream volume & media transport active",
        unavailableNote = "Proprietary AVR HDMI input switching and vendor multi-room grouping require authorized Google Cast SDK / AirPlay / Sonos Control API credentials."
    )

    override fun matches(ctx: IntegrationScanContext): Boolean {
        val lower = ctx.deviceName?.lowercase().orEmpty()
        return lower.contains("soundbar") || lower.contains("avr") || lower.contains("receiver") ||
            lower.contains("theatre") || lower.contains("theater") || lower.contains("sonos") ||
            lower.contains("denon") || lower.contains("yamaha")
    }

    override fun inferBrand(ctx: IntegrationScanContext): EarbudBrand = EarbudBrand.GENERIC_BT

    override fun inferCategory(ctx: IntegrationScanContext): AudioDeviceCategory = AudioDeviceCategory.HOME_THEATRE

    override fun detectCapabilities(telemetry: IntegrationTelemetryContext): DeviceCapabilities = DeviceCapabilities(
        integrationId = info.integrationId,
        supportsSingleBattery = false,
        supportsFindDevice = false,
        supportsCodecInfo = telemetry.hasCodecInfo,
        supportsSystemEqualizer = telemetry.systemEqAvailable,
        supportsSystemBassBoost = telemetry.systemBassAvailable,
        supportsSystemVocalEnhancement = telemetry.systemVocalAvailable,
        supportsSpatializer = telemetry.spatialState.isAvailable,
        protocolNotes = info.unavailableNote,
        featureMatrix = listOf(
            FeatureAvailabilityItem(
                featureName = "LAN mDNS / Zeroconf Audio Discovery",
                isSupported = true,
                requirementNote = "Live discovery via Android NsdManager (Cast, AirPlay, Sonos, DLNA, Spotify Connect)"
            ),
            FeatureAvailabilityItem(
                featureName = "System Volume & Media Playback Control",
                isSupported = true,
                requirementNote = "Live control via Android AudioManager & MediaKeyEvent"
            ),
            FeatureAvailabilityItem(
                featureName = "HDMI ARC / eARC & A2DP Route Inspection",
                isSupported = true,
                requirementNote = "Live hardware routing via Android AudioManager.getDevices()"
            ),
            FeatureAvailabilityItem(
                featureName = "Proprietary Multi-Room & HDMI Input Switching",
                isSupported = false,
                requirementNote = "UNAVAILABLE — Requires vendor-authorized Cast/Sonos/AVR API credentials"
            )
        )
    )

    override fun connectionGuidance(): String =
        "Connect your phone to the same Wi-Fi network as your soundbar, AVR, or smart speaker, or connect via Bluetooth / HDMI."
}

object OemIntegrationRegistry {
    val allIntegrations: List<OemDeviceIntegration> = listOf(
        OnePlusIntegration(),
        SamsungIntegration(),
        SonyIntegration(),
        JBLIntegration(),
        BoseIntegration(),
        NothingIntegration(),
        HomeTheatreIntegration(),
        GenericBluetoothIntegration()
    )

    fun resolve(
        deviceName: String?,
        manufacturerIds: Set<Int> = emptySet(),
        serviceUuids: List<ParcelUuid> = emptyList()
    ): OemDeviceIntegration = resolveByStrings(
        deviceName = deviceName,
        manufacturerIds = manufacturerIds,
        serviceUuidStrings = serviceUuids.map { it.uuid.toString() }
    )

    fun resolveByStrings(
        deviceName: String?,
        manufacturerIds: Set<Int> = emptySet(),
        serviceUuidStrings: List<String> = emptyList()
    ): OemDeviceIntegration {
        val ctx = IntegrationScanContext(
            deviceName = deviceName,
            manufacturerIds = manufacturerIds,
            serviceUuidStrings = serviceUuidStrings
        )
        return allIntegrations.firstOrNull { it.matches(ctx) } ?: GenericBluetoothIntegration()
    }
}
