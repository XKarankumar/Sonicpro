# SONIQ — Device & OEM Compatibility Matrix

SONIQ follows strict hardware honesty: standard Android Bluetooth, BLE GATT, AudioFX, Spatializer, and NsdManager capabilities operate live, while proprietary OEM features requiring closed partner SDKs are explicitly marked **UNAVAILABLE**.

| Brand / Category | Integration Module | Live Android Capabilities | Proprietary / Unavailable Without OEM SDK |
| --- | --- | --- | --- |
| **OnePlus / Oppo / Realme** | `OnePlusIntegration` | A2DP/AAC/LHDC codec telemetry, HFP/GATT battery (`0x180F`), Device Info (`0x180A`), AudioFX Studio EQ/Bass/Vocal, Spatializer check, Find Chime | Per-bud L/R/Case battery, HeyMelody ANC toggle, touch remapping, OTA |
| **Samsung Galaxy Buds** | `SamsungIntegration` | A2DP/AAC/SBC/SSC codec telemetry, combined battery, AudioFX DSP, Spatializer check, Find Chime | Galaxy Wearable 360 Audio head tracking, ANC levels, per-bud battery, touch mapping |
| **Sony WF / WH / LinkBuds** | `SonyIntegration` | LDAC/AAC/SBC codec telemetry, combined battery, AudioFX DSP, Spatializer check, Find Chime | Sony Sound Connect Tandem ANC/Adaptive Sound, Speak-to-Chat, DSEE Extreme |
| **JBL / Harman** | `JBLIntegration` | A2DP/HFP, GATT battery & info, AudioFX DSP, Theatre speaker routing | Ambient Aware / TalkThru hardware toggles, PartyBoost chaining |
| **Bose** | `BoseIntegration` | A2DP/HFP, GATT battery & info, AudioFX DSP, Spatializer check | Bose BMAP ANC levels, Bose Immersive Audio, SimpleSync |
| **Nothing / CMF** | `NothingIntegration` | Fast Pair, A2DP/HFP, AudioFX DSP, combined battery | Nothing X Ultra Bass, Personalized ANC, Dirac Opteo |
| **Apple AirPods / Beats, Vivo, Xiaomi, boAt, Noise, Sennheiser, Generic** | `GenericBluetoothIntegration` | Standard Bluetooth A2DP/HFP, GATT `0x180F`/`0x180A`, AudioFX DSP, Spatializer check, Find Chime | Vendor-specific ANC, per-bud battery, touch customization |
| **Home Theatre / Soundbars / AVRs** | `HomeTheatreIntegration` | LAN mDNS (`_googlecast._tcp`, `_airplay._tcp`, `_sonos._tcp`, `_spotify-connect._tcp`, `_http._tcp`), `AudioDeviceInfo` HDMI ARC/eARC/USB/A2DP routes, volume & media transport | Vendor-authenticated multi-room grouping & HDMI input switching |
