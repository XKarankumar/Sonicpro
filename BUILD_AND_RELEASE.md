# SONIQ PRO — Build & Release Guide

## Architecture Overview
- **Domain (`com.example.domain`)**: Hardware-agnostic earbud telemetry models and modular `EarbudsProtocolAdapter` registry (`StandardBluetoothAdapter`, `OnePlusOppoProtocolAdapter`, `FastPairProtocolAdapter`, and explicit opt-in `DemoPreviewFactory`).
- **Data (`com.example.data`)**:
  - `AndroidBluetoothController`: Real BLE `BluetoothLeScanner`, Classic discovery, A2DP/HFP profile proxies, BLE GATT Battery (`0x180F`) and Device Info (`0x180A`), and A2DP codec status.
  - `AudioFxDspEngine`: Real `android.media.audiofx.Equalizer`, `BassBoost`, and `Virtualizer` DSP controls.
  - `FindEarbudsEngine`: Stereo / Left / Right `AudioTrack` locator chime + optional coarse last-known location.
  - `SoniqDatabase` (Room) & `UserPreferencesDataStore` & `SettingsBackupManager` (versioned JSON export/import).
- **UI (`com.example.ui`)**: Jetpack Compose Material 3 OnePlus-inspired Obsidian Black & Crimson theme, adaptive `NavigationBar` / `NavigationRail`, and four main screens (`Studio`, `Sound`, `Devices`, `System`).

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
1. Every push automatically runs unit/Robolectric tests and builds a downloadable Debug APK (`soniq-pro-debug-apk` under **Actions → Artifacts**) using JDK 21, Gradle 9.3.1, Android SDK 36, and `debug.keystore.base64`.
2. To also generate signed release APKs and AABs (`soniq-pro-signed-release`):
   - Generate an upload keystore (alias `upload` to match `app/build.gradle.kts`):
     ```bash
     keytool -genkey -v -keystore my-upload-key.jks -keyalg RSA -keysize 2048 -validity 10000 -alias upload
     ```
   - Base64-encode the keystore and add the following GitHub Repository Secrets:
     - `KEYSTORE_BASE64`: `base64 -w 0 my-upload-key.jks`
     - `STORE_PASSWORD`: Keystore password
     - `KEY_PASSWORD`: Key password
3. Push to GitHub or trigger **SONIQ PRO Android CI & APK Build** manually via **workflow_dispatch**.
