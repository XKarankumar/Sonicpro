package com.example.data.repository

import com.example.BuildConfig
import com.example.domain.model.AppReleaseNote

object ReleaseNotesCatalog {
    val releases: List<AppReleaseNote> = listOf(
        AppReleaseNote(
            versionName = BuildConfig.VERSION_NAME,
            releaseDate = "2026-11-24",
            isCurrent = true,
            highlights = listOf(
                "Modular Bluetooth protocol adapters for standard A2DP/HFP, BLE GATT (0x180F / 0x180A), Google Fast Pair, and OnePlus/OPPO identification.",
                "Live Android AudioFX 5-band Equalizer, Bass Boost, and Spatial Virtualizer DSP engine.",
                "Versioned JSON backup & restore for device profiles with schema backward compatibility.",
                "Consent-gated diagnostics suite and clear Live Hardware vs Demo Preview separation."
            )
        ),
        AppReleaseNote(
            versionName = "1.1.0",
            releaseDate = "2026-10-12",
            highlights = listOf(
                "Added battery and connection event timeline persisted locally via Room.",
                "Added stereo / Left / Right acoustic locator chime for Find My Earbuds.",
                "Introduced customizable Studio dashboard widgets and OLED Dark / Studio Light themes."
            )
        ),
        AppReleaseNote(
            versionName = "1.0.0",
            releaseDate = "2026-09-01",
            highlights = listOf(
                "Initial release of SONIQ PRO companion studio.",
                "Bluetooth LE & Classic discovery with Android 12+ neverForLocation scan compliance."
            )
        )
    )
}
