package com.example.domain.protocol

import android.os.ParcelUuid
import com.example.domain.model.AncMode
import com.example.domain.model.AudioCodecInfo
import com.example.domain.model.BatteryTelemetry
import com.example.domain.model.BluetoothConnectionState
import com.example.domain.model.DataSourceMode
import com.example.domain.model.DeviceCapabilities
import com.example.domain.model.EarbudBrand
import com.example.domain.model.EarbudsDevice
import com.example.domain.model.MultipointHost
import com.example.domain.model.SpatialAudioMode

/**
 * Modular protocol adapter contract for Bluetooth earbuds.
 * Each brand adapter only claims capabilities that are genuinely supported by the
 * underlying Android Bluetooth stack, BLE GATT services, or documented vendor frames.
 */
interface EarbudsProtocolAdapter {
    val adapterId: String
    val brand: EarbudBrand

    fun matches(
        deviceName: String?,
        manufacturerIds: Set<Int> = emptySet(),
        serviceUuids: List<ParcelUuid> = emptyList()
    ): Boolean

    fun resolveCapabilities(
        hasGattBatteryService: Boolean,
        hasDeviceInfoService: Boolean,
        hasSingleBatteryLevel: Boolean,
        hasCodecInfo: Boolean,
        systemEqAvailable: Boolean,
        systemBassAvailable: Boolean,
        systemVirtualizerAvailable: Boolean
    ): DeviceCapabilities

    fun parseManufacturerBattery(manufacturerData: Map<Int, ByteArray>): BatteryTelemetry? = null
}

/**
 * Standard Bluetooth A2DP / HFP / BLE GATT (0x180F + 0x180A) adapter.
 * Works with any Bluetooth earbuds without fabricating proprietary hardware controls.
 */
class StandardBluetoothAdapter : EarbudsProtocolAdapter {
    override val adapterId: String = "standard_bt_gatt"
    override val brand: EarbudBrand = EarbudBrand.GENERIC_BT

    override fun matches(
        deviceName: String?,
        manufacturerIds: Set<Int>,
        serviceUuids: List<ParcelUuid>
    ): Boolean = true

    override fun resolveCapabilities(
        hasGattBatteryService: Boolean,
        hasDeviceInfoService: Boolean,
        hasSingleBatteryLevel: Boolean,
        hasCodecInfo: Boolean,
        systemEqAvailable: Boolean,
        systemBassAvailable: Boolean,
        systemVirtualizerAvailable: Boolean
    ): DeviceCapabilities = DeviceCapabilities(
        supportsIndividualBattery = false,
        supportsSingleBattery = hasGattBatteryService || hasSingleBatteryLevel,
        supportsAncControl = false,
        supportsTransparencyMode = false,
        supportsAmbientIntensity = false,
        supportsCustomTouchGestures = false,
        supportsLowLatencyGamingMode = false,
        supportsSpatialAudioHardware = false,
        supportsInEarDetection = false,
        supportsDualDeviceMultipoint = false,
        supportsCallMicControls = false,
        supportsFirmwareRead = hasDeviceInfoService,
        supportsFirmwareOta = false,
        supportsCodecInfo = hasCodecInfo,
        supportsSystemEqualizer = systemEqAvailable,
        supportsSystemBassBoost = systemBassAvailable,
        supportsSystemVirtualizer = systemVirtualizerAvailable,
        protocolNotes = "Standard Bluetooth A2DP/HFP & BLE GATT profile. Proprietary ANC/gesture/OTA commands are not exposed by generic Bluetooth."
    )
}

/**
 * Identifies OnePlus / OPPO / Realme earbuds via device name or Bluetooth SIG Company IDs
 * (0x058E / 0x079A / 0x06AE) and inspects documented BLE advertisement metadata.
 * Never fabricates proprietary HeyMelody RFCOMM commands that require signed OEM keys.
 */
class OnePlusOppoProtocolAdapter : EarbudsProtocolAdapter {
    override val adapterId: String = "oneplus_oppo_ble"
    override val brand: EarbudBrand = EarbudBrand.ONEPLUS

    private val companyIds = setOf(0x058E, 0x079A, 0x06AE)
    private val nameHints = listOf("oneplus", "buds", "nord buds", "oppo", "enco", "realme")

    override fun matches(
        deviceName: String?,
        manufacturerIds: Set<Int>,
        serviceUuids: List<ParcelUuid>
    ): Boolean {
        if (manufacturerIds.any { it in companyIds }) return true
        val lower = deviceName?.lowercase().orEmpty()
        return nameHints.any { lower.contains(it) } &&
            (lower.contains("oneplus") || lower.contains("oppo") || lower.contains("enco") || lower.contains("nord buds") || lower.contains("realme"))
    }

    override fun resolveCapabilities(
        hasGattBatteryService: Boolean,
        hasDeviceInfoService: Boolean,
        hasSingleBatteryLevel: Boolean,
        hasCodecInfo: Boolean,
        systemEqAvailable: Boolean,
        systemBassAvailable: Boolean,
        systemVirtualizerAvailable: Boolean
    ): DeviceCapabilities = DeviceCapabilities(
        supportsIndividualBattery = false,
        supportsSingleBattery = hasGattBatteryService || hasSingleBatteryLevel,
        supportsAncControl = false,
        supportsTransparencyMode = false,
        supportsAmbientIntensity = false,
        supportsCustomTouchGestures = false,
        supportsLowLatencyGamingMode = false,
        supportsSpatialAudioHardware = false,
        supportsInEarDetection = false,
        supportsDualDeviceMultipoint = false,
        supportsCallMicControls = false,
        supportsFirmwareRead = hasDeviceInfoService,
        supportsFirmwareOta = false,
        supportsCodecInfo = hasCodecInfo,
        supportsSystemEqualizer = systemEqAvailable,
        supportsSystemBassBoost = systemBassAvailable,
        supportsSystemVirtualizer = systemVirtualizerAvailable,
        protocolNotes = "OnePlus/OPPO BLE hardware identified. Standard BLE Battery/Device Info and Android system DSP are active; proprietary HeyMelody RFCOMM packets require OEM system permission."
    )
}

/**
 * Detects Google Fast Pair Service UUID (0xFE2C) and standard brand signatures
 * (Nothing, Sony, Samsung, Apple/Beats) for accurate protocol identification.
 */
class FastPairProtocolAdapter : EarbudsProtocolAdapter {
    override val adapterId: String = "google_fast_pair"
    override val brand: EarbudBrand = EarbudBrand.GOOGLE

    override fun matches(
        deviceName: String?,
        manufacturerIds: Set<Int>,
        serviceUuids: List<ParcelUuid>
    ): Boolean {
        val hasFastPairUuid = serviceUuids.any {
            it.uuid.toString().lowercase().startsWith("0000fe2c")
        }
        val lower = deviceName?.lowercase().orEmpty()
        return hasFastPairUuid || lower.contains("pixel buds")
    }

    override fun resolveCapabilities(
        hasGattBatteryService: Boolean,
        hasDeviceInfoService: Boolean,
        hasSingleBatteryLevel: Boolean,
        hasCodecInfo: Boolean,
        systemEqAvailable: Boolean,
        systemBassAvailable: Boolean,
        systemVirtualizerAvailable: Boolean
    ): DeviceCapabilities = DeviceCapabilities(
        supportsIndividualBattery = false,
        supportsSingleBattery = hasGattBatteryService || hasSingleBatteryLevel,
        supportsAncControl = false,
        supportsTransparencyMode = false,
        supportsAmbientIntensity = false,
        supportsCustomTouchGestures = false,
        supportsLowLatencyGamingMode = false,
        supportsSpatialAudioHardware = false,
        supportsInEarDetection = false,
        supportsDualDeviceMultipoint = false,
        supportsCallMicControls = false,
        supportsFirmwareRead = hasDeviceInfoService,
        supportsFirmwareOta = false,
        supportsCodecInfo = hasCodecInfo,
        supportsSystemEqualizer = systemEqAvailable,
        supportsSystemBassBoost = systemBassAvailable,
        supportsSystemVirtualizer = systemVirtualizerAvailable,
        protocolNotes = "Google Fast Pair (0xFE2C) detected. Standard BLE telemetry & system AudioFX DSP enabled; encrypted Fast Pair RFCOMM requires device-specific keys."
    )
}

object DeviceProtocolRegistry {
    private val adapters: List<EarbudsProtocolAdapter> = listOf(
        OnePlusOppoProtocolAdapter(),
        FastPairProtocolAdapter(),
        StandardBluetoothAdapter()
    )

    fun inferBrand(
        deviceName: String?,
        manufacturerIds: Set<Int> = emptySet(),
        serviceUuids: List<ParcelUuid> = emptyList()
    ): EarbudBrand = inferBrandByUuidStrings(
        deviceName = deviceName,
        manufacturerIds = manufacturerIds,
        serviceUuidStrings = serviceUuids.map { it.uuid.toString() }
    )

    fun inferBrandByUuidStrings(
        deviceName: String?,
        manufacturerIds: Set<Int> = emptySet(),
        serviceUuidStrings: List<String> = emptyList()
    ): EarbudBrand {
        val lower = deviceName?.lowercase().orEmpty()
        return when {
            lower.contains("oneplus") || lower.contains("nord buds") -> EarbudBrand.ONEPLUS
            lower.contains("oppo") || lower.contains("enco") -> EarbudBrand.OPPO
            lower.contains("realme") -> EarbudBrand.REALME
            lower.contains("nothing") || lower.contains("cmf") -> EarbudBrand.NOTHING
            lower.contains("sony") || lower.contains("wf-") || lower.contains("wh-") -> EarbudBrand.SONY
            lower.contains("galaxy buds") || lower.contains("samsung") -> EarbudBrand.SAMSUNG
            lower.contains("airpods") || lower.contains("beats") -> EarbudBrand.APPLE
            lower.contains("pixel buds") || serviceUuidStrings.any {
                it.lowercase().startsWith("0000fe2c")
            } -> EarbudBrand.GOOGLE
            manufacturerIds.contains(0x058E) -> EarbudBrand.ONEPLUS
            manufacturerIds.contains(0x079A) -> EarbudBrand.OPPO
            else -> EarbudBrand.GENERIC_BT
        }
    }

    fun resolveAdapter(
        deviceName: String?,
        manufacturerIds: Set<Int> = emptySet(),
        serviceUuids: List<ParcelUuid> = emptyList()
    ): EarbudsProtocolAdapter {
        return adapters.firstOrNull { it.matches(deviceName, manufacturerIds, serviceUuids) }
            ?: StandardBluetoothAdapter()
    }
}

/**
 * Explicit opt-in Demo Preview device for testing UI animations and layouts when
 * no physical earbuds are paired (e.g. emulator). Clearly badged as DEMO_PREVIEW everywhere.
 */
object DemoPreviewFactory {
    const val DEMO_ADDRESS = "DEMO:00:11:22:33:44"

    fun createDemoDevice(
        systemEqAvailable: Boolean,
        systemBassAvailable: Boolean,
        systemVirtualizerAvailable: Boolean
    ): EarbudsDevice = EarbudsDevice(
        address = DEMO_ADDRESS,
        name = "SONIQ Buds Pro 3 (Demo)",
        brand = EarbudBrand.ONEPLUS,
        rssiDbm = -44,
        isBonded = true,
        isAudioDevice = true,
        connectionState = BluetoothConnectionState.CONNECTED,
        isCaseOpen = true,
        battery = BatteryTelemetry(
            leftPercent = 88,
            rightPercent = 84,
            casePercent = 72,
            singlePercent = 86,
            isLeftCharging = false,
            isRightCharging = false,
            isCaseCharging = true,
            sourceLabel = "Demo Preview Sandbox (Simulated)",
            lastUpdatedMs = System.currentTimeMillis()
        ),
        ancMode = AncMode.NOISE_CANCELLATION,
        ancIntensity = 0.85f,
        gamingModeEnabled = false,
        spatialAudioMode = SpatialAudioMode.FIXED_STAGE,
        inEarDetectionEnabled = true,
        autoPlayPauseEnabled = true,
        dualDeviceEnabled = true,
        callNoiseReductionEnabled = true,
        micSidetoneEnabled = false,
        codecInfo = AudioCodecInfo(
            codecName = "LHDC 5.0 (Demo)",
            sampleRateHz = 96000,
            bitsPerSample = 24,
            channelMode = "Stereo"
        ),
        firmwareVersion = "v3.4.18-demo",
        hardwareRevision = "Rev C Acoustic",
        manufacturerName = "SONIQ Reference Lab",
        capabilities = DeviceCapabilities(
            supportsIndividualBattery = true,
            supportsSingleBattery = true,
            supportsAncControl = true,
            supportsTransparencyMode = true,
            supportsAmbientIntensity = true,
            supportsCustomTouchGestures = true,
            supportsLowLatencyGamingMode = true,
            supportsSpatialAudioHardware = true,
            supportsInEarDetection = true,
            supportsDualDeviceMultipoint = true,
            supportsCallMicControls = true,
            supportsFirmwareRead = true,
            supportsFirmwareOta = false,
            supportsCodecInfo = true,
            supportsSystemEqualizer = systemEqAvailable,
            supportsSystemBassBoost = systemBassAvailable,
            supportsSystemVirtualizer = systemVirtualizerAvailable,
            protocolNotes = "Interactive Demo Preview active. No physical Bluetooth earbuds are connected; values below illustrate full-feature hardware telemetry."
        ),
        dataSourceMode = DataSourceMode.DEMO_PREVIEW,
        multipointHosts = listOf(
            MultipointHost("This Android Phone", "Smartphone • Primary", isActiveAudioSource = true, isConnected = true),
            MultipointHost("Studio Workstation", "Laptop • Standby", isActiveAudioSource = false, isConnected = true)
        )
    )
}
