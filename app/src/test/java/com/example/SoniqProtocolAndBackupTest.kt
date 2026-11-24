package com.example

import com.example.data.local.DeviceProfileEntity
import com.example.data.local.SettingsBackupManager
import com.example.data.local.StoredAppPreferences
import com.example.data.repository.ReleaseNotesCatalog
import com.example.domain.model.DashboardWidgetId
import com.example.domain.model.DataSourceMode
import com.example.domain.model.EarbudBrand
import com.example.domain.model.EqPresetId
import com.example.domain.model.ThemePreference
import com.example.domain.protocol.DemoPreviewFactory
import com.example.domain.protocol.DeviceProtocolRegistry
import com.example.domain.protocol.OnePlusOppoProtocolAdapter
import com.example.domain.protocol.StandardBluetoothAdapter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SoniqProtocolAndBackupTest {

    @Test
    fun `standard bluetooth adapter never fabricates unsupported proprietary controls`() {
        val adapter = StandardBluetoothAdapter()
        val caps = adapter.resolveCapabilities(
            hasGattBatteryService = true,
            hasDeviceInfoService = true,
            hasSingleBatteryLevel = true,
            hasCodecInfo = true,
            systemEqAvailable = true,
            systemBassAvailable = true,
            systemVirtualizerAvailable = false
        )
        assertTrue(caps.supportsSingleBattery)
        assertTrue(caps.supportsFirmwareRead)
        assertTrue(caps.supportsSystemEqualizer)
        assertFalse(caps.supportsIndividualBattery)
        assertFalse(caps.supportsAncControl)
        assertFalse(caps.supportsFirmwareOta)
        assertFalse(caps.supportsCustomTouchGestures)
    }

    @Test
    fun `protocol registry identifies OnePlus and Google Fast Pair hardware accurately`() {
        val onePlusAdapter = DeviceProtocolRegistry.resolveAdapter(
            deviceName = "OnePlus Buds Pro 2",
            manufacturerIds = setOf(0x058E)
        )
        assertTrue(onePlusAdapter is OnePlusOppoProtocolAdapter)
        assertEquals(EarbudBrand.ONEPLUS, DeviceProtocolRegistry.inferBrandByUuidStrings("OnePlus Buds Pro 2"))

        val fastPairBrand = DeviceProtocolRegistry.inferBrandByUuidStrings(
            deviceName = "Generic Wireless",
            serviceUuidStrings = listOf("0000fe2c-0000-1000-8000-00805f9b34fb")
        )
        assertEquals(EarbudBrand.GOOGLE, fastPairBrand)
    }

    @Test
    fun `demo preview factory clearly marks device as DEMO_PREVIEW`() {
        val demo = DemoPreviewFactory.createDemoDevice(
            systemEqAvailable = true,
            systemBassAvailable = true,
            systemVirtualizerAvailable = false
        )
        assertEquals(DataSourceMode.DEMO_PREVIEW, demo.dataSourceMode)
        assertTrue(demo.battery.sourceLabel.contains("Demo", ignoreCase = true))
    }

    @Test
    fun `settings backup manager exports and restores profiles with schema validation`() {
        val manager = SettingsBackupManager()
        val prefs = StoredAppPreferences(
            themePreference = ThemePreference.LIGHT_CRIMSON,
            hiddenDashboardWidgets = setOf(DashboardWidgetId.MULTIPOINT_STATUS),
            activeEqPreset = EqPresetId.ONEPLUS_BOLD_BASS,
            eqBandsMb = listOf(550, 320, -100, 180, 300),
            bassBoostStrength = 420
        )
        val profile = DeviceProfileEntity(
            profileName = "Gym Bass",
            deviceName = "OnePlus Buds 3",
            deviceAddress = "AA:BB:CC:DD:EE:FF",
            eqPresetName = EqPresetId.ONEPLUS_BOLD_BASS.name,
            eqBandsCsv = "550,320,-100,180,300",
            bassBoostStrength = 420,
            virtualizerStrength = 200,
            gamingModePreferred = true,
            gesturesJson = "[]"
        )

        val json = manager.exportToJson(prefs, listOf(profile))
        val restored = manager.importFromJson(json).getOrThrow()

        assertEquals(ThemePreference.LIGHT_CRIMSON, restored.themePreference)
        assertEquals(EqPresetId.ONEPLUS_BOLD_BASS, restored.activeEqPreset)
        assertEquals(420, restored.bassBoostStrength)
        assertEquals(setOf(DashboardWidgetId.MULTIPOINT_STATUS), restored.hiddenWidgets)
        assertEquals(1, restored.profiles.size)
        assertEquals("Gym Bass", restored.profiles.first().profileName)
    }

    @Test
    fun `backup manager rejects unsupported future schema versions`() {
        val manager = SettingsBackupManager()
        val badJson = """{"schemaVersion": 999}"""
        val result = manager.importFromJson(badJson)
        assertTrue(result.isFailure)
    }

    @Test
    fun `release notes catalog has current release first`() {
        assertTrue(ReleaseNotesCatalog.releases.isNotEmpty())
        assertTrue(ReleaseNotesCatalog.releases.first().isCurrent)
    }
}
