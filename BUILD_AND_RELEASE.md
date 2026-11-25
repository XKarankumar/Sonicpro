# SONIQ — Build & Release Guide

## Architecture Overview
- **Domain (`com.example.domain`)**:
  - `EarbudModels.kt`: Hardware telemetry, dynamic EQ bands, Android `Spatializer` capability state, and Home Theatre route/mDNS endpoint models.
  - `OemIntegration.kt`: 8 modular `OemDeviceIntegration` adapters (`OnePlusIntegration`, `SamsungIntegration`, `SonyIntegration`, `JBLIntegration`, `BoseIntegration`, `NothingIntegration`, `HomeTheatreIntegration`, `GenericBluetoothIntegration`) covering 15 manufacturers with strict `UNAVAILABLE` labeling for proprietary SDK features.
- **Data (`com.example.data`)**:
  - `AndroidBluetoothController`: BLE `BluetoothLeScanner`, Classic discovery, A2DP/HFP profile proxies, BLE GATT Battery (`0x180F`) and Device Information (`0x180A`), and A2DP codec telemetry.
  - `AudioFxDspEngine`: Real Android `Equalizer` (up to 10 hardware bands), `BassBoost`, `LoudnessEnhancer`, `Virtualizer`, and Android 13+ (`API 33+`) `Spatializer` inspector.
  - `HomeTheatreManager`: Android `NsdManager` mDNS LAN discovery (`_googlecast._tcp.`, `_airplay._tcp.`, `_sonos._tcp.`, `_spotify-connect._tcp.`, `_http._tcp.`), `AudioDeviceInfo` route inspection, stream volume, and `KeyEvent` media transport.
  - `FindEarbudsEngine`: Stereo / Left / Right `AudioTrack` locator chime + optional coarse last-known location.
  - `SoniqDatabase` (Room), `UserPreferencesDataStore`, and `SettingsBackupManager` (versioned JSON export/import).
- **UI (`com.example.ui`)**: Jetpack Compose Material 3 AMOLED Black & Crimson theme, adaptive 5-tab `NavigationBar` / `NavigationRail` (`Home`, `Devices`, `Studio`, `Theatre`, `Settings`).

## Local Build & Test Commands
Run unit & Robolectric tests:
```bash
gradle :app:testDebugUnitTest
```

Build debug APK:
```bash
gradle :app:assembleDebug
```

Build signed release APK / AAB locally:
```bash
export KEYSTORE_PATH=/path/to/my-upload-key.jks
export STORE_PASSWORD=your_store_password
export KEY_PASSWORD=your_key_password
gradle :app:assembleRelease :app:bundleRelease
```

## GitHub Actions CI & Signed Release Setup
1. Every push runs `:app:testDebugUnitTest` and builds a downloadable Debug APK (`soniq-debug-apk` under **Actions → Artifacts**) using JDK 21, Gradle 9.3.1, Android SDK 36, and `debug.keystore.base64`.
2. To generate signed release APKs and AABs (`soniq-signed-release`), set GitHub Repository Secrets `KEYSTORE_BASE64`, `STORE_PASSWORD`, and `KEY_PASSWORD` matching keystore alias `upload`.
