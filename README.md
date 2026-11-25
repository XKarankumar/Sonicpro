# SONIQ — Universal Audio Intelligence Platform

**SONIQ** is a native Kotlin & Jetpack Compose Android companion platform for wireless earbuds, headphones, Bluetooth speakers, soundbars, and Home Theatre network audio ecosystems.

## Five Core Modules
1. **Home Dashboard**: Live connected hardware overview, battery telemetry (`0x180F` / HFP), A2DP codec status, quick DSP summary, recently connected hardware, and customizable widget ordering.
2. **Devices & Telemetry**: BLE & Classic Bluetooth discovery, pairing/connection management, complete Hardware Feature Matrix with honest `UNAVAILABLE` OEM indicators, Find Device L/R/Both `AudioTrack` chime, and optional coarse Last-Known Location.
3. **Audio Studio & 3D**: Dynamic Android `AudioFX` parametric Equalizer (up to 10 bands depending on device HAL), 7 sound modes (`Studio Reference`, `Music`, `Cinema`, `Gaming Audio`, `Bass Boost`, `Vocal Clarity`, `Custom`), `BassBoost`, `LoudnessEnhancer` vocal enhancement, and real Android 13+ `Spatializer` 3D audio & head-tracking inspection.
4. **Home Theatre Ecosystem**: Local Wi-Fi mDNS (`NsdManager`) discovery for Google Cast, AirPlay, Sonos, Spotify Connect, and DLNA/Network AVRs, HDMI ARC/eARC/USB/A2DP output route inspection, system volume/mute, and media playback transport controls.
5. **Settings & Ecosystem**: 8 modular OEM integrations covering 15 manufacturers, Room-persisted audio profiles, battery/connection history, diagnostics & local crash recovery logs, compatibility report export, and versioned JSON backup/restore.

## Build & Documentation
- See `BUILD_AND_RELEASE.md` for local Gradle commands and GitHub Actions APK/AAB CI instructions.
- See `DEVICE_COMPATIBILITY.md` for manufacturer-by-manufacturer protocol and SDK capability boundaries.
