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
                "SONIQ Universal Audio Intelligence Platform supporting 15 brands across 8 modular OEM integrations.",
                "Up-to-10-band Android AudioFX Studio Equalizer, Bass Boost, LoudnessEnhancer Vocal Clarity, and 7 acoustic presets.",
                "Home Theatre Ecosystem module with live LAN mDNS discovery (Cast, AirPlay, Sonos, Spotify Connect, DLNA), HDMI ARC/eARC routing, and stream controls.",
                "Real Android 13+ Spatializer 3D Audio & IMU head-tracking inspector with strict unavailable-feature transparency."
            )
        ),
        AppReleaseNote(
            versionName = "1.1.0",
            releaseDate = "2026-10-12",
            highlights = listOf(
                "Added battery and connection event timeline persisted locally via Room.",
                "Added stereo / Left / Right acoustic locator chime for Find Device.",
                "Introduced OLED Dark & Studio Light themes with JSON backup and restore."
            )
        ),
        AppReleaseNote(
            versionName = "1.0.0",
            releaseDate = "2026-09-01",
            highlights = listOf(
                "Initial release of SONIQ Bluetooth audio companion.",
                "Bluetooth LE & Classic discovery with Android 12+ neverForLocation scan compliance."
            )
        )
    )
}
