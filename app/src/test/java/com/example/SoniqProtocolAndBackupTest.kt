package com.example

import com.example.data.bluetooth.AudioFxDspEngine
import com.example.data.local.DeviceProfileEntity
import com.example.data.local.SettingsBackupManager
import com.example.data.local.StoredAppPreferences
import com.example.data.repository.ReleaseNotesCatalog
import com.example.domain.integration.BoseIntegration
import com.example.domain.integration.GenericBluetoothIntegration
import com.example.domain.integration.HomeTheatreIntegration
import com.example.domain.integration.IntegrationScanContext
import com.example.domain.integration.IntegrationTelemetryContext
import com.example.domain.integration.JBLIntegration
import com.example.domain.integration.NothingIntegration
import com.example.domain.integration.OemIntegrationRegistry
import com.example.domain.integration.OnePlusIntegration
import com.example.domain.integration.SamsungIntegration
import com.example.domain.integration.SonyIntegration
import com.example.domain.model.AudioDeviceCategory
import com.example.domain.model.DashboardWidgetId
import com.example.domain.model.EarbudBrand
import com.example.domain.model.EqBandState
import com.example.domain.model.EqPresetId
import com.example.domain.model.SpatialAudioCapabilityState
import com.example.domain.model.SpatialImmersiveLevel
import com.example.domain.model.ThemePreference
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SoniqProtocolAndBackupTest {

    private fun sampleTelemetry(
        spatialAvailable: Boolean = true,
        headTrackerAvailable: Boolean = false
    ) = IntegrationTelemetryContext(
        hasGattBatteryService = true,
        hasDeviceInfoService = true,
        hasSingleBatteryLevel = true,
        hasCodecInfo = true,
        systemEqAvailable = true,
        systemBassAvailable = true,
        systemVocalAvailable = true,
        spatialState = SpatialAudioCapabilityState(
            apiSupported = true,
            isAvailable = spatialAvailable,
            isEnabled = spatialAvailable,
            immersiveLevel = if (spatialAvailable) SpatialImmersiveLevel.MULTICHANNEL else SpatialImmersiveLevel.NONE,
            isHeadTrackerAvailable = headTrackerAvailable
        )
    )

    @Test
    fun `registry contains all 8 modular integrations covering all 15 audio brands`() {
        assertEquals(8, OemIntegrationRegistry.allIntegrations.size)
        val covered = OemIntegrationRegistry.allIntegrations
            .flatMap { it.info.coveredBrands }
            .toSet()
        EarbudBrand.entries.forEach { brand ->
            assertTrue("Missing integration coverage for ${brand.displayName}", brand in covered)
        }
    }

    @Test
    fun `oem registry resolves manufacturers and categories accurately`() {
        val onePlus = OemIntegrationRegistry.resolveByStrings("OnePlus Buds Pro 3", setOf(0x058E))
        assertTrue(onePlus is OnePlusIntegration)
        assertEquals(EarbudBrand.ONEPLUS, onePlus.inferBrand(IntegrationScanContext("OnePlus Buds Pro 3")))

        val samsung = OemIntegrationRegistry.resolveByStrings("Galaxy Buds3 Pro", setOf(0x0075))
        assertTrue(samsung is SamsungIntegration)
        assertEquals(EarbudBrand.SAMSUNG, samsung.inferBrand(IntegrationScanContext("Galaxy Buds3 Pro")))

        val sony = OemIntegrationRegistry.resolveByStrings("Sony WH-1000XM5", setOf(0x012D))
        assertTrue(sony is SonyIntegration)
        assertEquals(AudioDeviceCategory.HEADPHONES, sony.inferCategory(IntegrationScanContext("Sony WH-1000XM5")))

        val jbl = OemIntegrationRegistry.resolveByStrings("JBL Charge 5")
        assertTrue(jbl is JBLIntegration)
        assertEquals(AudioDeviceCategory.SPEAKER, jbl.inferCategory(IntegrationScanContext("JBL Charge 5")))

        val bose = OemIntegrationRegistry.resolveByStrings("Bose Smart Soundbar 900")
        assertTrue(bose is BoseIntegration)
        assertEquals(AudioDeviceCategory.SOUNDBAR, bose.inferCategory(IntegrationScanContext("Bose Smart Soundbar 900")))

        val nothing = OemIntegrationRegistry.resolveByStrings("Nothing Ear (2)")
        assertTrue(nothing is NothingIntegration)

        val avr = OemIntegrationRegistry.resolveByStrings("Denon AVR-X3800H")
        assertTrue(avr is HomeTheatreIntegration)
        assertEquals(AudioDeviceCategory.HOME_THEATRE, avr.inferCategory(IntegrationScanContext("Denon AVR-X3800H")))

        val airpods = OemIntegrationRegistry.resolveByStrings("AirPods Pro", setOf(0x004C))
        assertTrue(airpods is GenericBluetoothIntegration)
        assertEquals(EarbudBrand.APPLE, airpods.inferBrand(IntegrationScanContext("AirPods Pro", setOf(0x004C))))
    }

    @Test
    fun `integrations never fabricate unsupported proprietary controls`() {
        val telemetry = sampleTelemetry()
        val integrations = listOf(
            OnePlusIntegration(),
            SamsungIntegration(),
            SonyIntegration(),
            JBLIntegration(),
            BoseIntegration(),
            NothingIntegration(),
            GenericBluetoothIntegration()
        )
        integrations.forEach { integration ->
            val caps = integration.detectCapabilities(telemetry)
            assertTrue(caps.supportsSingleBattery)
            assertTrue(caps.supportsSystemEqualizer)
            assertTrue(caps.supportsSystemBassBoost)
            assertTrue(caps.supportsSystemVocalEnhancement)
            assertTrue(caps.supportsFirmwareRead)
            assertFalse("${integration.info.integrationId} must not fake individual L/R/Case battery", caps.supportsIndividualBattery)
            assertFalse("${integration.info.integrationId} must not fake proprietary ANC", caps.supportsAncControl)
            assertFalse("${integration.info.integrationId} must not fake touch gestures", caps.supportsCustomTouchGestures)
            assertFalse("${integration.info.integrationId} must not fake wear detection", caps.supportsWearDetection)
            assertTrue(caps.featureMatrix.any { !it.isSupported && it.requirementNote.contains("UNAVAILABLE") })
        }
    }

    @Test
    fun `home theatre integration exposes honest LAN and route capabilities`() {
        val ht = HomeTheatreIntegration()
        val caps = ht.detectCapabilities(sampleTelemetry(spatialAvailable = false))
        assertTrue(caps.featureMatrix.any { it.featureName.contains("mDNS") && it.isSupported })
        assertTrue(caps.featureMatrix.any { it.featureName.contains("HDMI Input Switching") && !it.isSupported })
    }

    @Test
    fun `audiofx preset mapper scales 10-band curves cleanly onto 5-band and 10-band hardware`() {
        val fiveBands = (0 until 5).map { idx ->
            EqBandState(idx, 60 * (idx + 1), "${idx}Hz", 0, -1500, 1500)
        }
        val tenBands = (0 until 10).map { idx ->
            EqBandState(idx, 31 * (idx + 1), "${idx}Hz", 0, -1500, 1500)
        }

        val mapped5 = AudioFxDspEngine.mapPresetToHardwareBands(EqPresetId.BASS_ENHANCEMENT, fiveBands)
        assertEquals(5, mapped5.size)
        assertTrue("Low band should be boosted for Bass Boost", mapped5.first() >= 500)

        val mapped10 = AudioFxDspEngine.mapPresetToHardwareBands(EqPresetId.VOCAL_ENHANCEMENT, tenBands)
        assertEquals(10, mapped10.size)
        assertEquals(450, mapped10[5])
    }

    @Test
    fun `settings backup manager exports and restores 10-band EQ and vocalGainMb`() {
        val manager = SettingsBackupManager()
        val prefs = StoredAppPreferences(
            themePreference = ThemePreference.LIGHT_CRIMSON,
            hiddenDashboardWidgets = setOf(DashboardWidgetId.RECENT_DEVICES),
            activeEqPreset = EqPresetId.CINEMA,
            eqBandsMb = listOf(450, 400, 200, -50, 100, 250, 300, 200, 150, 100),
            bassBoostStrength = 520,
            vocalGainMb = 650
        )
        val profile = DeviceProfileEntity(
            profileName = "Theatre Night",
            deviceName = "Sony WH-1000XM5",
            deviceAddress = "AA:BB:CC:DD:EE:FF",
            eqPresetName = EqPresetId.CINEMA.name,
            eqBandsCsv = "450,400,200,-50,100,250,300,200,150,100",
            bassBoostStrength = 520,
            vocalGainMb = 650,
            virtualizerStrength = 0,
            gamingModePreferred = false,
            gesturesJson = "[]"
        )

        val json = manager.exportToJson(prefs, listOf(profile))
        val restored = manager.importFromJson(json).getOrThrow()

        assertEquals(ThemePreference.LIGHT_CRIMSON, restored.themePreference)
        assertEquals(EqPresetId.CINEMA, restored.activeEqPreset)
        assertEquals(10, restored.eqBandsMb?.size)
        assertEquals(520, restored.bassBoostStrength)
        assertEquals(650, restored.vocalGainMb)
        assertEquals(setOf(DashboardWidgetId.RECENT_DEVICES), restored.hiddenWidgets)
        assertEquals(1, restored.profiles.size)
        assertEquals(650, restored.profiles.first().vocalGainMb)
    }

    @Test
    fun `backup manager remains backward compatible with schema v1 lacking vocalGainMb`() {
        val manager = SettingsBackupManager()
        val legacyV1Json = """
            {
              "schemaVersion": 1,
              "themePreference": "DARK_OLED",
              "activeEqPreset": "MUSIC",
              "eqBandsCsv": "200,150,0,150,200",
              "bassBoostStrength": 300
            }
        """.trimIndent()
        val restored = manager.importFromJson(legacyV1Json).getOrThrow()
        assertEquals(ThemePreference.DARK_OLED, restored.themePreference)
        assertEquals(EqPresetId.MUSIC, restored.activeEqPreset)
        assertEquals(5, restored.eqBandsMb?.size)
        assertEquals(null, restored.vocalGainMb)
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
